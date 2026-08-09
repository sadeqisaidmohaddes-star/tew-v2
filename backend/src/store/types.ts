/**
 * Wire shapes and the storage contract.
 *
 * The API responses here are the ones `core/API_CONTRACT.md` describes, in the
 * snake_case the Android client's DTOs expect. Keeping them in one file makes
 * the "no counts anywhere" rule checkable at a glance: if a `*_count` field
 * ever appears below, non-negotiable #4 has been broken.
 */

export interface MemoRow {
  id: string;
  author_username: string;
  audio_url: string;
  duration_ms: number;
  posted_at: number;
  transcript: string | null;
  liked_by_me: boolean;
  moderation: {
    state: 'visible' | 'under_review' | 'removed';
    reason: string | null;
    appeal: 'not_applicable' | 'available' | 'submitted' | 'upheld' | 'denied';
  };
}

export interface CommentRow {
  id: string;
  memo_id: string;
  author_username: string;
  audio_url: string;
  duration_ms: number;
  posted_at: number;
  transcript: string | null;
}

export interface FeedPageRow {
  memos: MemoRow[];
  /** Null means the stream has ended. Non-negotiable #3 — never omit this. */
  next_cursor: string | null;
}

export type ReportReason =
  | 'harassment'
  | 'hate_speech'
  | 'sexual_content'
  | 'spam'
  | 'other';

/**
 * Everything the routes need from storage.
 *
 * An interface rather than direct SQL in the handlers, so the route layer can
 * be tested without Postgres — `HANDLING_PROTOCOLS.md` requires a local
 * instance for real database work, and requiring one for every unit test would
 * mean the tests stop being run.
 */
export interface Store {
  ensureUser(id: string, username: string): Promise<void>;

  feed(userId: string, cursor: string | null, nowSeconds: number): Promise<FeedPageRow>;

  setLiked(userId: string, memoId: string, liked: boolean): Promise<boolean>;

  /** Marks a memo heard so it does not come back. Not an engagement signal. */
  markHeard(userId: string, memoId: string): Promise<boolean>;

  comments(memoId: string): Promise<CommentRow[] | null>;

  addComment(
    userId: string,
    memoId: string,
    audioKey: string,
    durationMs: number,
  ): Promise<CommentRow | null>;

  addMemo(userId: string, audioKey: string, durationMs: number): Promise<MemoRow>;

  report(userId: string, memoId: string, reason: ReportReason): Promise<boolean>;

  /** The caller's own memos, including removed ones with their reasons. */
  ownMemos(userId: string): Promise<MemoRow[]>;

  appeal(userId: string, memoId: string, text: string): Promise<boolean>;

  /**
   * Delete one of the caller's own memos, audio and all.
   *
   * Returns false when the memo does not exist or belongs to someone else —
   * the route cannot tell those apart on purpose, because distinguishing them
   * would confirm the existence of another person's memo to a stranger.
   */
  deleteMemo(userId: string, memoId: string): Promise<boolean>;

  /**
   * Delete the caller's account and everything they recorded.
   *
   * Non-negotiable #7 treats voice as biometric data, and the retention rule
   * is: audio exists until the person deletes it or deletes their account.
   * There is no time-based expiry and no soft delete — a "deleted" recording
   * that is still on disk is not deleted.
   */
  deleteAccount(userId: string): Promise<void>;
}
