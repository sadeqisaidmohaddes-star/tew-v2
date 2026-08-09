import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { StubTokenVerifier } from '../src/auth/verifier.ts';
import { decodeCursor, encodeCursor } from '../src/feed/cursor.ts';
import { defaultFeedWindow, spreadByAuthor, isLastPage } from '../src/feed/ordering.ts';
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

test('one author cannot fill a page', () => {
  const memos = Array.from({ length: 20 }, (_, i) => ({
    id: `m${i}`,
    authorId: i < 15 ? 'loud' : `quiet${i}`,
    postedAt: NOW - i,
  }));

  const { page } = spreadByAuthor(memos, defaultFeedWindow);
  const fromLoud = page.filter((m) => m.authorId === 'loud').length;

  assert.equal(fromLoud, 1, 'one prolific poster took over the page');
});

test('spreading holds memos back rather than dropping them', () => {
  const memos = Array.from({ length: 6 }, (_, i) => ({
    id: `m${i}`,
    authorId: 'same',
    postedAt: NOW - i,
  }));

  const { page, heldBack } = spreadByAuthor(memos, defaultFeedWindow);

  assert.equal(page.length + heldBack.length, memos.length, 'memos disappeared');
});

test('spreading can be turned off entirely', () => {
  // The honest "no rule at all" option, if the per-author cap is judged to be
  // too close to ranking.
  const memos = Array.from({ length: 5 }, (_, i) => ({
    id: `m${i}`,
    authorId: 'same',
    postedAt: NOW - i,
  }));

  const { page } = spreadByAuthor(memos, { ...defaultFeedWindow, maxPerAuthorPerPage: null });

  assert.equal(page.length, 5);
});

test('a short page is not mistaken for the end of the stream', () => {
  // The spreading rule can return a short page while memos are still waiting.
  // Treating that as the end would cut a listener off early.
  assert.equal(isLastPage([{ id: 'held' }], false), false);
  assert.equal(isLastPage([], false), true);
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
  store.seedMemo({ id: 'old', authorId: 'a', postedAt: NOW - 30 * 24 * 60 * 60 });
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
