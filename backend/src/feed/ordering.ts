/**
 * How the feed is ordered, and where it ends.
 *
 * ## Newest to oldest. That is the whole rule.
 *
 * Decided by Said. Strict reverse-chronological over memos the caller has not
 * already heard or skipped — no scoring, no personalisation, no per-author
 * quota, nothing that reorders one memo relative to another for any reason
 * except when it was posted.
 *
 * An earlier draft of this file capped each author to one memo per page, to
 * stop a prolific poster filling a quiet day. That is gone. It was the closest
 * thing in the codebase to a ranking rule — it demoted real memos for reasons
 * the poster did not choose — and `BRIEF.md` rules out a ranking algorithm.
 * If one person dominating the feed turns out to be a real problem, it is a
 * moderation or product conversation, not something to solve by quietly
 * reordering people.
 *
 * ## Where it ends
 *
 * Two things bound the feed, and neither is a ranking signal:
 *
 * - **Already heard.** A memo served to you does not come back. This is what
 *   makes the set finite for a given listener.
 * - **The age window.** Memos older than [FeedWindow.maxAgeDays] are not
 *   served. Retained so that a new account does not receive the entire history
 *   of the network as one enormous stream; set `maxAgeDays: null` to disable
 *   it and serve everything unheard.
 *
 * Together they are what makes non-negotiable #3 — *the stream ends* — true at
 * the data layer rather than only in the client.
 */

export interface FeedWindow {
  /** How far back the feed reaches. Null serves everything unheard. */
  maxAgeDays: number | null;
  /** Rows per page. Matches the client's default page size. */
  pageSize: number;
}

export const defaultFeedWindow: FeedWindow = {
  maxAgeDays: 30,
  pageSize: 10,
};

/**
 * Oldest `posted_at` the feed will serve, in epoch seconds, or null when the
 * window is disabled.
 */
export function windowStart(nowEpochSeconds: number, window: FeedWindow): number | null {
  if (window.maxAgeDays === null) return null;
  return nowEpochSeconds - window.maxAgeDays * 24 * 60 * 60;
}

export interface OrderableMemo {
  id: string;
  postedAt: number;
}

/**
 * Newest first, ties broken by id so pagination is stable.
 *
 * The tie-break is not cosmetic: two memos posted in the same second need a
 * deterministic order, or a keyset cursor sitting between them would either
 * skip one or serve it twice.
 */
export function newestFirst<T extends OrderableMemo>(memos: readonly T[]): T[] {
  return [...memos].sort((a, b) => {
    if (b.postedAt !== a.postedAt) return b.postedAt - a.postedAt;
    return a.id < b.id ? 1 : a.id > b.id ? -1 : 0;
  });
}

/** Cut an ordered list to one page, and say whether more remain. */
export function takePage<T>(
  ordered: readonly T[],
  window: FeedWindow,
): { page: T[]; hasMore: boolean } {
  const page = ordered.slice(0, window.pageSize);
  return { page, hasMore: ordered.length > window.pageSize };
}
