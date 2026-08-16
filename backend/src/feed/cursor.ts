/**
 * Feed cursors.
 *
 * `core/API_CONTRACT.md` asked whether the cursor is an opaque string. It is —
 * this module is the only place that knows otherwise, and the Android client
 * never parses one.
 *
 * The encoding is keyset pagination on `(posted_at, id)`, not an offset. Offset
 * pagination silently skips or repeats rows when the underlying set changes
 * between pages, and this set changes constantly: memos are posted, and skipped
 * memos drop out of the caller's own feed. A user hearing the same memo twice
 * or missing one entirely would have no way of knowing it happened.
 *
 * The pair is needed rather than `posted_at` alone because two memos can share
 * a timestamp, and a cursor that cannot break that tie loses whichever one
 * sorts second.
 */

export interface FeedCursor {
  /** Seconds since the epoch — matches the API's `posted_at`. */
  postedAt: number;
  /** Tie-break for memos posted in the same second. */
  id: string;
}

/**
 * Encode to an opaque token. Base64url, so it survives a query string without
 * escaping and does not invite anyone to read it.
 */
export function encodeCursor(cursor: FeedCursor): string {
  const raw = JSON.stringify([cursor.postedAt, cursor.id]);
  return Buffer.from(raw, 'utf8').toString('base64url');
}

/**
 * Decode a token, or null if it is unusable.
 *
 * A malformed cursor is treated as "start from the beginning" by the caller
 * rather than as an error. Cursors reach the client, get stored, and come back
 * after a release — rejecting one with a 400 would strand a user on a feed
 * that will not load, with no way to recover but reinstalling.
 */
export function decodeCursor(token: string | undefined | null): FeedCursor | null {
  if (!token) return null;

  try {
    const raw = Buffer.from(token, 'base64url').toString('utf8');
    const parsed: unknown = JSON.parse(raw);

    if (!Array.isArray(parsed) || parsed.length !== 2) return null;

    const [postedAt, id] = parsed;
    if (typeof postedAt !== 'number' || !Number.isFinite(postedAt)) return null;
    if (typeof id !== 'string' || id.length === 0) return null;

    return { postedAt, id };
  } catch {
    return null;
  }
}
