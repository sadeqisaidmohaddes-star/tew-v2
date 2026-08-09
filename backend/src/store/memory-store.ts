import { encodeCursor, decodeCursor } from '../feed/cursor.ts';
import {
  defaultFeedWindow,
  newestFirst,
  takePage,
  windowStart,
  type FeedWindow,
} from '../feed/ordering.ts';
import type {
  CommentRow,
  FeedPageRow,
  MemoRow,
  ReportReason,
  Store,
} from './types.ts';

interface MemoRecord {
  id: string;
  authorId: string;
  authorUsername: string;
  audioKey: string;
  durationMs: number;
  postedAt: number;
  transcript: string | null;
  state: 'visible' | 'under_review' | 'removed';
  reason: string | null;
  appeal: MemoRow['moderation']['appeal'];
}

/**
 * In-memory [Store] for tests and local development without Postgres.
 *
 * It implements the same ordering and cursor rules as the Postgres store —
 * deliberately, because the tests that matter most here are about *feed
 * behaviour* (does the stream end, does one author dominate a page, does a
 * skipped memo come back), and those would be worth nothing if the fake
 * paginated differently from the real thing.
 *
 * Not a cache and not for production.
 */
export class MemoryStore implements Store {
  private users = new Map<string, string>();
  private memos: MemoRecord[] = [];
  private commentRows: CommentRow[] = [];
  private likes = new Set<string>();
  private heard = new Set<string>();
  private reports: Array<{ memoId: string; reason: ReportReason }> = [];
  private counter = 0;

  constructor(private readonly window: FeedWindow = defaultFeedWindow) {}

  async ensureUser(id: string, username: string): Promise<void> {
    this.users.set(id, username);
  }

  /** Test helper — seeds a memo without going through the upload path. */
  seedMemo(memo: Partial<MemoRecord> & { authorId: string; postedAt: number }): string {
    const id = memo.id ?? `memo-${++this.counter}`;
    this.memos.push({
      id,
      authorId: memo.authorId,
      authorUsername: memo.authorUsername ?? this.users.get(memo.authorId) ?? memo.authorId,
      audioKey: memo.audioKey ?? `${id}.m4a`,
      durationMs: memo.durationMs ?? 10_000,
      postedAt: memo.postedAt,
      transcript: memo.transcript ?? null,
      state: memo.state ?? 'visible',
      reason: memo.reason ?? null,
      appeal: memo.appeal ?? 'not_applicable',
    });
    return id;
  }

  async feed(userId: string, cursor: string | null, nowSeconds: number): Promise<FeedPageRow> {
    const from = windowStart(nowSeconds, this.window);
    const decoded = decodeCursor(cursor);

    const candidates = newestFirst(
      this.memos
        .filter((m) => m.state === 'visible')
        .filter((m) => from === null || m.postedAt >= from)
        .filter((m) => !this.heard.has(`${userId}:${m.id}`))
        .filter((m) => {
          if (!decoded) return true;
          // Keyset: strictly older than the cursor, ties broken by id.
          if (m.postedAt !== decoded.postedAt) return m.postedAt < decoded.postedAt;
          return m.id < decoded.id;
        }),
    );

    const { page, hasMore } = takePage(candidates, this.window);
    const last = page.at(-1);

    return {
      memos: page.map((m) => this.toRow(m, userId)),
      next_cursor:
        !hasMore || !last ? null : encodeCursor({ postedAt: last.postedAt, id: last.id }),
    };
  }

  async deleteMemo(userId: string, memoId: string): Promise<string[] | null> {
    const i = this.memos.findIndex((m) => m.id === memoId && m.authorId === userId);
    if (i < 0) return null;

    const keys = [this.memos[i]!.audioKey];
    for (const c of this.commentRows) if (c.memo_id === memoId) keys.push(c.audio_url);

    this.memos.splice(i, 1);
    this.commentRows = this.commentRows.filter((c) => c.memo_id !== memoId);
    for (const key of [...this.likes]) if (key.endsWith(`:${memoId}`)) this.likes.delete(key);
    for (const key of [...this.heard]) if (key.endsWith(`:${memoId}`)) this.heard.delete(key);
    return keys;
  }

  async deleteAccount(userId: string): Promise<string[]> {
    const username = this.users.get(userId);
    const own = this.memos.filter((m) => m.authorId === userId);
    const ownIds = new Set(own.map((m) => m.id));

    const keys = own.map((m) => m.audioKey);
    for (const c of this.commentRows) {
      if (ownIds.has(c.memo_id) || c.author_username === username) keys.push(c.audio_url);
    }

    this.memos = this.memos.filter((m) => m.authorId !== userId);
    this.commentRows = this.commentRows.filter(
      (c) => c.author_username !== username && !ownIds.has(c.memo_id),
    );
    for (const key of [...this.likes]) {
      if (key.startsWith(`${userId}:`) || ownIds.has(key.split(':')[1] ?? '')) this.likes.delete(key);
    }
    for (const key of [...this.heard]) {
      if (key.startsWith(`${userId}:`) || ownIds.has(key.split(':')[1] ?? '')) this.heard.delete(key);
    }
    this.users.delete(userId);
    return keys;
  }

  async setLiked(userId: string, memoId: string, liked: boolean): Promise<boolean> {
    if (!this.memos.some((m) => m.id === memoId)) return false;
    const key = `${userId}:${memoId}`;
    if (liked) this.likes.add(key);
    else this.likes.delete(key);
    return true;
  }

  async markHeard(userId: string, memoId: string): Promise<boolean> {
    if (!this.memos.some((m) => m.id === memoId)) return false;
    this.heard.add(`${userId}:${memoId}`);
    return true;
  }

  async comments(memoId: string): Promise<CommentRow[] | null> {
    if (!this.memos.some((m) => m.id === memoId)) return null;
    return this.commentRows.filter((c) => c.memo_id === memoId);
  }

  async addComment(
    userId: string,
    memoId: string,
    audioKey: string,
    durationMs: number,
  ): Promise<CommentRow | null> {
    if (!this.memos.some((m) => m.id === memoId)) return null;
    const row: CommentRow = {
      id: `comment-${++this.counter}`,
      memo_id: memoId,
      author_username: this.users.get(userId) ?? userId,
      audio_url: audioKey,
      duration_ms: durationMs,
      posted_at: Math.floor(Date.now() / 1000),
      transcript: null,
    };
    this.commentRows.push(row);
    return row;
  }

  async addMemo(userId: string, audioKey: string, durationMs: number): Promise<MemoRow> {
    const id = this.seedMemo({
      authorId: userId,
      authorUsername: this.users.get(userId) ?? userId,
      audioKey,
      durationMs,
      postedAt: Math.floor(Date.now() / 1000),
    });
    const record = this.memos.find((m) => m.id === id)!;
    return this.toRow(record, userId);
  }

  async report(userId: string, memoId: string, reason: ReportReason): Promise<boolean> {
    if (!this.memos.some((m) => m.id === memoId)) return false;
    this.reports.push({ memoId, reason });
    return true;
  }

  async ownMemos(userId: string): Promise<MemoRow[]> {
    return this.memos
      .filter((m) => m.authorId === userId)
      .sort((a, b) => b.postedAt - a.postedAt)
      .map((m) => this.toRow(m, userId));
  }

  async appeal(userId: string, memoId: string, _text: string): Promise<boolean> {
    const memo = this.memos.find((m) => m.id === memoId && m.authorId === userId);
    if (!memo || memo.appeal !== 'available') return false;
    memo.appeal = 'submitted';
    return true;
  }

  private toRow(memo: MemoRecord, viewerId: string): MemoRow {
    return {
      id: memo.id,
      author_username: memo.authorUsername,
      audio_url: memo.audioKey,
      duration_ms: memo.durationMs,
      posted_at: memo.postedAt,
      transcript: memo.transcript,
      liked_by_me: this.likes.has(`${viewerId}:${memo.id}`),
      moderation: {
        state: memo.state,
        reason: memo.reason,
        appeal: memo.appeal,
      },
    };
  }
}
