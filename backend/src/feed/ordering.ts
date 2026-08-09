/**
 * How the feed is ordered, and where it ends.
 *
 * ## This is a product decision wearing technical clothes
 *
 * `BRIEF.md` forbids a ranking algorithm and `IMPLEMENTATION.md` inherits it:
 * no scoring, no personalisation, no engagement signals. But "not ranked" is
 * not the same as "no rule" — something has to decide what a user hears first
 * and when the stream stops.
 *
 * The default implemented here, **pending a decision from Said**:
 *
 * - **Newest first**, over memos the caller has not already heard or skipped.
 * - **A seven-day window.** Older memos fall out rather than accumulating.
 * - **One memo per author per page**, so a prolific poster cannot fill a quiet
 *   day's feed and crowd everyone else out.
 *
 * Each of those is defensible and none is neutral. The third in particular is
 * the closest thing here to a ranking decision — it demotes real memos for
 * reasons the poster did not choose. It is here because the alternative on a
 * small network is one person's ten memos being the entire experience for
 * everyone else, which is worse. **Flagging it rather than burying it.**
 *
 * The seven-day window is what makes non-negotiable #3 true at the data layer
 * rather than only in the client: the stream ends because there is a finite set
 * to end, not because the client stopped asking.
 */

export interface FeedWindow {
  /** How far back the feed reaches. */
  maxAgeDays: number;
  /** Rows per page. Matches the client's default page size. */
  pageSize: number;
  /**
   * Cap on memos from any one author within a single page. Null disables the
   * spreading rule entirely, which is the honest "no rule at all" option if
   * Said would rather have that.
   */
  maxPerAuthorPerPage: number | null;
}

export const defaultFeedWindow: FeedWindow = {
  maxAgeDays: 7,
  pageSize: 10,
  maxPerAuthorPerPage: 1,
};

/** Oldest `posted_at` the feed will serve, in epoch seconds. */
export function windowStart(nowEpochSeconds: number, window: FeedWindow): number {
  return nowEpochSeconds - window.maxAgeDays * 24 * 60 * 60;
}

export interface OrderableMemo {
  id: string;
  authorId: string;
  postedAt: number;
}

/**
 * Apply the per-author spread to an already time-ordered list.
 *
 * Kept separate from SQL and pure so the rule is testable and, more
 * importantly, so it is *visible*. Buried in a query it would be invisible to
 * the next person reading this, and a rule nobody can find is a rule nobody
 * can challenge.
 *
 * Memos held back are not dropped — they are simply not in this page, and the
 * next page picks them up. Nothing is hidden permanently.
 */
export function spreadByAuthor<T extends OrderableMemo>(
  memos: readonly T[],
  window: FeedWindow,
): { page: T[]; heldBack: T[] } {
  const cap = window.maxPerAuthorPerPage;
  if (cap === null) {
    return { page: memos.slice(0, window.pageSize), heldBack: memos.slice(window.pageSize) };
  }

  const seen = new Map<string, number>();
  const page: T[] = [];
  const heldBack: T[] = [];

  for (const memo of memos) {
    const used = seen.get(memo.authorId) ?? 0;
    if (page.length < window.pageSize && used < cap) {
      page.push(memo);
      seen.set(memo.authorId, used + 1);
    } else {
      heldBack.push(memo);
    }
  }

  return { page, heldBack };
}

/**
 * Whether this is the last page.
 *
 * Deliberately not `page.length < pageSize`. The spreading rule can return a
 * short page while memos are still waiting, and treating that as the end would
 * cut a user off early — the stream is supposed to end when it is *empty*, not
 * when a page happens to be small.
 */
export function isLastPage(heldBack: readonly unknown[], moreInDb: boolean): boolean {
  return heldBack.length === 0 && !moreInDb;
}
