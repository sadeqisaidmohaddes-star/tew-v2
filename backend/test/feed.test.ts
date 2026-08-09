import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { StubTokenVerifier } from '../src/auth/verifier.ts';
import { decodeCursor, encodeCursor } from '../src/feed/cursor.ts';
import { defaultFeedWindow, newestFirst, takePage } from '../src/feed/ordering.ts';
import { MemoryStore } from '../src/store/memory-store.ts';

const NOW = 1_770_000_000;

function appWith(store: MemoryStore) {
  return buildApp({ store, verifier: new StubTokenVerifier(), now: () => NOW });
}

const auth = { authorization: 'Bearer stub:u1:amina' };

// ---------------------------------------------------------------- cursors

test('a cursor round-trips', () => {
  const cursor = { postedAt: NOW, id: 'memo-7' };
  assert.deepEqual(decodeCursor(encodeCursor(cursor)), cursor);
});

test('a cursor is opaque, not a readable offset', () => {
  const token = encodeCursor({ postedAt: NOW, id: 'memo-7' });
  assert.ok(!token.includes('memo-7'));
});

test('a corrupt cursor means start again, not an error', () => {
  // Cursors get stored on devices and come back after a release. Rejecting one
  // would strand a user on a feed that never loads.
  assert.equal(decodeCursor('not-base64-at-all!!'), null);
  assert.equal(decodeCursor(''), null);
  assert.equal(decodeCursor(undefined), null);
  assert.equal(decodeCursor(Buffer.from('{"a":1}').toString('base64url')), null);
});

// --------------------------------------------------------------- ordering

test('memos come back newest first', () => {
  // Said's decision: strict reverse-chronological, nothing else.
  const memos = [
    { id: 'b', postedAt: NOW - 100 },
    { id: 'a', postedAt: NOW },
    { id: 'c', postedAt: NOW - 200 },
  ];

  assert.deepEqual(newestFirst(memos).map((m) => m.id), ['a', 'b', 'c']);
});

test('ties are broken deterministically', () => {
  // Two memos in the same second need a stable order, or a cursor landing
  // between them would skip one or serve it twice.
  const memos = [
    { id: 'aaa', postedAt: NOW },
    { id: 'ccc', postedAt: NOW },
    { id: 'bbb', postedAt: NOW },
  ];

  const once = newestFirst(memos).map((m) => m.id);
  const twice = newestFirst([...memos].reverse()).map((m) => m.id);

  assert.deepEqual(once, twice, 'ordering is not stable across input order');
});

test('nothing reorders memos by author', () => {
  // Guard on the removed per-author quota. It demoted real memos for reasons
  // the poster did not choose, which BRIEF.md rules out. If it comes back,
  // this fails.
  const memos = Array.from({ length: 8 }, (_, i) => ({
    id: `m${i}`,
    authorId: i < 6 ? 'prolific' : `other${i}`,
    postedAt: NOW - i,
  }));

  const ordered = newestFirst(memos);

  assert.deepEqual(
    ordered.map((m) => m.id),
    memos.map((m) => m.id),
    'something reordered the feed by author',
  );
});

test('a page is cut at the page size, and reports whether more remain', () => {
  const many = Array.from({ length: 25 }, (_, i) => ({ id: `m${i}`, postedAt: NOW - i }));
  const few = Array.from({ length: 3 }, (_, i) => ({ id: `m${i}`, postedAt: NOW - i }));

  const big = takePage(many, defaultFeedWindow);
  assert.equal(big.page.length, defaultFeedWindow.pageSize);
  assert.equal(big.hasMore, true);

  const small = takePage(few, defaultFeedWindow);
  assert.equal(small.page.length, 3);
  assert.equal(small.hasMore, false);
});

// ------------------------------------------------------------------- feed

test('the feed returns memos and a cursor', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  for (let i = 0; i < 25; i++) {
    store.seedMemo({ authorId: `author${i}`, postedAt: NOW - i * 60 });
  }

  const app = appWith(store);
  const res = await app.inject({ method: 'GET', url: '/v1/feed', headers: auth });

  assert.equal(res.statusCode, 200);
  const body = res.json();
  assert.equal(body.memos.length, defaultFeedWindow.pageSize);
  assert.ok(body.next_cursor, 'expected more pages');
});

test('the stream ends — next_cursor becomes null', async () => {
  // Non-negotiable #3, enforced at the data layer rather than only in the app.
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  for (let i = 0; i < 12; i++) {
    store.seedMemo({ authorId: `author${i}`, postedAt: NOW - i * 60 });
  }

  const app = appWith(store);
  let cursor: string | null = null;
  let pages = 0;

  do {
    const url: string = cursor
      ? `/v1/feed?cursor=${encodeURIComponent(cursor)}`
      : '/v1/feed';
    const res = await app.inject({ method: 'GET', url, headers: auth });
    cursor = res.json().next_cursor as string | null;
    pages++;
    assert.ok(pages < 20, 'feed never terminated');
  } while (cursor);

  assert.equal(cursor, null);
});

test('memos outside the window are not served', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'old', authorId: 'a', postedAt: NOW - 90 * 24 * 60 * 60 });
  store.seedMemo({ id: 'fresh', authorId: 'b', postedAt: NOW - 60 });

  const app = appWith(store);
  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();

  assert.deepEqual(body.memos.map((m: { id: string }) => m.id), ['fresh']);
});

test('a skipped memo does not come back', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'skip-me', authorId: 'a', postedAt: NOW - 10 });
  store.seedMemo({ id: 'keep-me', authorId: 'b', postedAt: NOW - 20 });

  const app = appWith(store);
  await app.inject({ method: 'POST', url: '/v1/memos/skip-me/skip', headers: auth });

  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.deepEqual(body.memos.map((m: { id: string }) => m.id), ['keep-me']);
});

test('removed memos never reach the feed', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'removed', authorId: 'a', postedAt: NOW - 10, state: 'removed' });
  store.seedMemo({ id: 'ok', authorId: 'b', postedAt: NOW - 20 });

  const app = appWith(store);
  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();

  assert.deepEqual(body.memos.map((m: { id: string }) => m.id), ['ok']);
});

test('no response carries a count of anything', async () => {
  // Non-negotiable #4. This is the test that would catch a like_count being
  // added to the wire because some future screen "just needs it".
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ authorId: 'a', postedAt: NOW - 10 });

  const app = appWith(store);
  const raw = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).body;

  assert.ok(!/_count"/.test(raw), `a count leaked onto the wire: ${raw}`);
  assert.ok(!/"likes"/.test(raw));
});

test('a prolific author is not throttled in the served feed', async () => {
  // End-to-end version of the ordering guard: six memos from one person come
  // back in one page, newest first, untouched.
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  for (let i = 0; i < 6; i++) {
    store.seedMemo({ id: `p${i}`, authorId: 'prolific', postedAt: NOW - i * 60 });
  }

  const body = (await appWith(store).inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();

  assert.deepEqual(
    body.memos.map((m: { id: string }) => m.id),
    ['p0', 'p1', 'p2', 'p3', 'p4', 'p5'],
  );
});
