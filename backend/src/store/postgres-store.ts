import type pg from 'pg';
import { decodeCursor, encodeCursor } from '../feed/cursor.ts';
import { defaultFeedWindow, windowStart, type FeedWindow } from '../feed/ordering.ts';
import type {
  CommentRow,
  FeedPageRow,
  MemoRow,
  ReportReason,
  Store,
} from './types.ts';

/**
 * Postgres-backed [Store].
 *
 * Ordering, the window and the cursor rules are the same as [MemoryStore]'s —
 * they have to be, or the tests written against the in-memory one would stop
 * meaning anything about production. Newest first, ties broken by id,
 * keyset pagination on `(posted_at, id)`.
 *
 * Timestamps cross the boundary as epoch seconds rather than `Date`, matching
 * the wire format the Android client parses. Postgres does the conversion in
 * SQL so no timezone handling happens in JavaScript, which is where that sort
 * of bug usually comes from.
 */
export class PostgresStore implements Store {
  constructor(
    private readonly pool: pg.Pool,
    private readonly window: FeedWindow = defaultFeedWindow,
  ) {}

  async ensureUser(id: string, username: string): Promise<void> {
    await this.pool.query(
      `INSERT INTO users (id, username) VALUES ($1, $2)
       ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username`,
      [id, username],
    );
  }

  async feed(userId: string, cursor: string | null, nowSeconds: number): Promise<FeedPageRow> {
    const decoded = decodeCursor(cursor);
    const from = windowStart(nowSeconds, this.window);

    // One extra row so "is there another page" is answered without a second
    // COUNT query — on a shared VPS that second round trip is not free.
    const limit = this.window.pageSize + 1;

    const { rows } = await this.pool.query(
      `SELECT m.id,
              u.username                         AS author_username,
              m.audio_key,
              m.duration_ms,
              EXTRACT(EPOCH FROM m.posted_at)::bigint AS posted_at,
              m.transcript,
              (l.user_id IS NOT NULL)            AS liked_by_me,
              m.moderation_state,
              m.moderation_reason,
              m.appeal_state
         FROM memos m
         JOIN users u ON u.id = m.author_id
         LEFT JOIN likes l ON l.memo_id = m.id AND l.user_id = $1
         LEFT JOIN heard h ON h.memo_id = m.id AND h.user_id = $1
        WHERE m.moderation_state = 'visible'
          AND h.memo_id IS NULL
          AND ($2::bigint IS NULL OR EXTRACT(EPOCH FROM m.posted_at) >= $2::bigint)
          AND ($3::bigint IS NULL OR (
                EXTRACT(EPOCH FROM m.posted_at) < $3::bigint
                OR (EXTRACT(EPOCH FROM m.posted_at) = $3::bigint AND m.id < $4)
              ))
        ORDER BY m.posted_at DESC, m.id DESC
        LIMIT $5`,
      [userId, from, decoded?.postedAt ?? null, decoded?.id ?? '', limit],
    );

    const hasMore = rows.length > this.window.pageSize;
    const page = rows.slice(0, this.window.pageSize);
    const last = page.at(-1);

    return {
      memos: page.map(toMemoRow),
      next_cursor:
        !hasMore || !last
          ? null
          : encodeCursor({ postedAt: Number(last.posted_at), id: last.id }),
    };
  }

  async setLiked(userId: string, memoId: string, liked: boolean): Promise<boolean> {
    if (!(await this.memoExists(memoId))) return false;

    if (liked) {
      await this.pool.query(
        `INSERT INTO likes (user_id, memo_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
        [userId, memoId],
      );
    } else {
      await this.pool.query(`DELETE FROM likes WHERE user_id = $1 AND memo_id = $2`, [
        userId,
        memoId,
      ]);
    }
    return true;
  }

  async markHeard(userId: string, memoId: string): Promise<boolean> {
    if (!(await this.memoExists(memoId))) return false;
    await this.pool.query(
      `INSERT INTO heard (user_id, memo_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
      [userId, memoId],
    );
    return true;
  }

  async comments(memoId: string): Promise<CommentRow[] | null> {
    if (!(await this.memoExists(memoId))) return null;

    const { rows } = await this.pool.query(
      `SELECT c.id, c.memo_id, u.username AS author_username, c.audio_key,
              c.duration_ms,
              EXTRACT(EPOCH FROM c.posted_at)::bigint AS posted_at,
              c.transcript
         FROM comments c
         JOIN users u ON u.id = c.author_id
        WHERE c.memo_id = $1
        ORDER BY c.posted_at ASC`,
      [memoId],
    );
    return rows.map(toCommentRow);
  }

  async addComment(
    userId: string,
    memoId: string,
    audioKey: string,
    durationMs: number,
  ): Promise<CommentRow | null> {
    if (!(await this.memoExists(memoId))) return null;

    const { rows } = await this.pool.query(
      `INSERT INTO comments (id, memo_id, author_id, audio_key, duration_ms)
       VALUES (gen_random_uuid()::text, $1, $2, $3, $4)
       RETURNING id, memo_id, audio_key, duration_ms,
                 EXTRACT(EPOCH FROM posted_at)::bigint AS posted_at, transcript`,
      [memoId, userId, audioKey, durationMs],
    );
    const row = rows[0];
    const { rows: user } = await this.pool.query(`SELECT username FROM users WHERE id = $1`, [
      userId,
    ]);
    return toCommentRow({ ...row, author_username: user[0]?.username ?? userId });
  }

  async addMemo(userId: string, audioKey: string, durationMs: number): Promise<MemoRow> {
    const { rows } = await this.pool.query(
      `INSERT INTO memos (id, author_id, audio_key, duration_ms)
       VALUES (gen_random_uuid()::text, $1, $2, $3)
       RETURNING id, audio_key, duration_ms,
                 EXTRACT(EPOCH FROM posted_at)::bigint AS posted_at,
                 transcript, moderation_state, moderation_reason, appeal_state`,
      [userId, audioKey, durationMs],
    );
    const { rows: user } = await this.pool.query(`SELECT username FROM users WHERE id = $1`, [
      userId,
    ]);
    return toMemoRow({
      ...rows[0],
      author_username: user[0]?.username ?? userId,
      liked_by_me: false,
    });
  }

  async report(userId: string, memoId: string, reason: ReportReason): Promise<boolean> {
    if (!(await this.memoExists(memoId))) return false;
    await this.pool.query(
      `INSERT INTO reports (id, memo_id, reporter_id, reason)
       VALUES (gen_random_uuid()::text, $1, $2, $3)`,
      [memoId, userId, reason],
    );
    return true;
  }

  async ownMemos(userId: string): Promise<MemoRow[]> {
    const { rows } = await this.pool.query(
      `SELECT m.id, u.username AS author_username, m.audio_key, m.duration_ms,
              EXTRACT(EPOCH FROM m.posted_at)::bigint AS posted_at,
              m.transcript,
              (l.user_id IS NOT NULL) AS liked_by_me,
              m.moderation_state, m.moderation_reason, m.appeal_state
         FROM memos m
         JOIN users u ON u.id = m.author_id
         LEFT JOIN likes l ON l.memo_id = m.id AND l.user_id = $1
        WHERE m.author_id = $1
        ORDER BY m.posted_at DESC`,
      [userId],
    );
    // Includes removed memos, with reasons — non-negotiable #8.
    return rows.map(toMemoRow);
  }

  async appeal(userId: string, memoId: string, text: string): Promise<boolean> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');

      // Guarded by the same conditions the memory store applies: yours, and
      // currently appealable. Done inside the transaction so two taps on a
      // flaky connection cannot file two appeals.
      const { rowCount } = await client.query(
        `UPDATE memos SET appeal_state = 'submitted'
          WHERE id = $1 AND author_id = $2
            AND moderation_state = 'removed' AND appeal_state = 'available'`,
        [memoId, userId],
      );

      if (!rowCount) {
        await client.query('ROLLBACK');
        return false;
      }

      await client.query(
        `INSERT INTO appeals (id, memo_id, author_id, text)
         VALUES (gen_random_uuid()::text, $1, $2, $3)`,
        [memoId, userId, text],
      );
      await client.query('COMMIT');
      return true;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Hard delete. The row goes, and `ON DELETE CASCADE` takes comments, likes
   * and heard-markers with it.
   *
   * The audio object itself is deleted by the caller of this method once
   * object storage exists — see the note in README.md. Until then audio lives
   * beside the row and goes with it.
   */
  async deleteMemo(userId: string, memoId: string): Promise<string[] | null> {
    // Collect the audio keys before the cascade removes the rows that name
    // them. Doing it afterwards would leave the files orphaned on disk with
    // nothing left pointing at them — which under non-negotiable #7 is the
    // worst outcome: the recording survives and nobody knows it is there.
    const { rows: replies } = await this.pool.query(
      `SELECT audio_key FROM comments WHERE memo_id = $1`,
      [memoId],
    );

    const { rows: deleted } = await this.pool.query(
      `DELETE FROM memos WHERE id = $1 AND author_id = $2 RETURNING audio_key`,
      [memoId, userId],
    );
    if (!deleted.length) return null;

    return [deleted[0].audio_key, ...replies.map((r: { audio_key: string }) => r.audio_key)];
  }

  async deleteAccount(userId: string): Promise<string[]> {
    const { rows } = await this.pool.query(
      `SELECT audio_key FROM memos WHERE author_id = $1
       UNION ALL
       SELECT audio_key FROM comments WHERE author_id = $1
       UNION ALL
       SELECT c.audio_key FROM comments c
         JOIN memos m ON m.id = c.memo_id
        WHERE m.author_id = $1`,
      [userId],
    );

    // Cascades through memos, comments, likes, heard, reports and appeals.
    // No soft delete: see migrations/001-init.sql for why there is no
    // deleted_at column.
    await this.pool.query(`DELETE FROM users WHERE id = $1`, [userId]);

    return [...new Set(rows.map((r: { audio_key: string }) => r.audio_key))];
  }

  private async memoExists(memoId: string): Promise<boolean> {
    const { rowCount } = await this.pool.query(`SELECT 1 FROM memos WHERE id = $1`, [memoId]);
    return Boolean(rowCount);
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function toMemoRow(row: any): MemoRow {
  return {
    id: row.id,
    author_username: row.author_username,
    audio_url: row.audio_key,
    duration_ms: Number(row.duration_ms),
    posted_at: Number(row.posted_at),
    transcript: row.transcript ?? null,
    liked_by_me: Boolean(row.liked_by_me),
    moderation: {
      state: row.moderation_state,
      reason: row.moderation_reason ?? null,
      appeal: row.appeal_state,
    },
  };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function toCommentRow(row: any): CommentRow {
  return {
    id: row.id,
    memo_id: row.memo_id,
    author_username: row.author_username,
    audio_url: row.audio_key,
    duration_ms: Number(row.duration_ms),
    posted_at: Number(row.posted_at),
    transcript: row.transcript ?? null,
  };
}
