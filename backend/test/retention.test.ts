import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { tempAudioStore } from './helpers.ts';
import { StubTokenVerifier } from '../src/auth/verifier.ts';
import { MemoryStore } from '../src/store/memory-store.ts';

const NOW = 1_770_000_000;
const auth = { authorization: 'Bearer stub:u1:amina' };

function appWith(store: MemoryStore) {
  return buildApp({ store, audio: tempAudioStore(), verifier: new StubTokenVerifier(), now: () => NOW });
}

/**
 * The retention rule, as decided: audio exists until the person deletes the
 * memo or deletes their account, and then it is gone. No time-based expiry,
 * no soft delete.
 *
 * These tests exist because non-negotiable #7 makes voice biometric data, and
 * "deleted" has to mean deleted. A user cannot check the disk; the guarantee
 * has to be kept by the code.
 */

test('deleting a memo removes it from the feed', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'mine', authorId: 'u1', postedAt: NOW - 10 });
  store.seedMemo({ id: 'theirs', authorId: 'u2', postedAt: NOW - 20 });

  const app = appWith(store);
  const res = await app.inject({ method: 'DELETE', url: '/v1/memos/mine', headers: auth });
  assert.equal(res.statusCode, 204);

  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.deepEqual(body.memos.map((m: { id: string }) => m.id), ['theirs']);
});

test('a deleted memo is gone from your own memos too, not merely hidden', async () => {
  // The failure this guards against: hiding a memo from the feed while
  // leaving it in place. That is not deletion, and the person who asked for
  // it cannot tell the difference from outside.
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'mine', authorId: 'u1', postedAt: NOW - 10 });

  const app = appWith(store);
  await app.inject({ method: 'DELETE', url: '/v1/memos/mine', headers: auth });

  const own = (await app.inject({ method: 'GET', url: '/v1/me/memos', headers: auth })).json();
  assert.deepEqual(own.memos, []);
});

test('you cannot delete someone else memo, and are not told it exists', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'theirs', authorId: 'someone-else', postedAt: NOW - 10 });

  const app = appWith(store);
  const mine = await app.inject({ method: 'DELETE', url: '/v1/memos/theirs', headers: auth });
  const missing = await app.inject({ method: 'DELETE', url: '/v1/memos/nope', headers: auth });

  // Same status for "not yours" and "does not exist" — distinguishing them
  // would confirm another person's memo exists to a stranger.
  assert.equal(mine.statusCode, 404);
  assert.equal(missing.statusCode, 404);
});

test('deleting a memo takes its comments with it', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'mine', authorId: 'u1', postedAt: NOW - 10 });
  await store.addComment('u2', 'mine', 'reply.m4a', 4000);

  const app = appWith(store);
  await app.inject({ method: 'DELETE', url: '/v1/memos/mine', headers: auth });

  const res = await app.inject({ method: 'GET', url: '/v1/memos/mine/comments', headers: auth });
  assert.equal(res.statusCode, 404);
});

test('deleting the account removes everything that person recorded', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'a', authorId: 'u1', postedAt: NOW - 10 });
  store.seedMemo({ id: 'b', authorId: 'u1', postedAt: NOW - 20 });
  store.seedMemo({ id: 'other', authorId: 'u2', postedAt: NOW - 30 });

  const app = appWith(store);
  const res = await app.inject({ method: 'DELETE', url: '/v1/me', headers: auth });
  assert.equal(res.statusCode, 204);

  // Their memos are gone from everyone's feed, not just their own view.
  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.deepEqual(body.memos.map((m: { id: string }) => m.id), ['other']);
});

test('deleting the account leaves other people untouched', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'theirs', authorId: 'u2', postedAt: NOW - 10 });

  const app = appWith(store);
  await app.inject({ method: 'DELETE', url: '/v1/me', headers: auth });

  const body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.equal(body.memos.length, 1);
});

test('deletion needs authentication', async () => {
  const store = new MemoryStore();
  store.seedMemo({ id: 'm', authorId: 'u1', postedAt: NOW });

  const app = appWith(store);
  for (const url of ['/v1/memos/m', '/v1/me']) {
    const res = await app.inject({ method: 'DELETE', url });
    assert.equal(res.statusCode, 401, url);
  }
});

test('nothing expires on its own', async () => {
  // There is no time-based retention. A memo inside the feed window is still
  // there however long the service has been running — only the person who
  // recorded it can remove it.
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'old-but-mine', authorId: 'u1', postedAt: NOW - 29 * 24 * 60 * 60 });

  const own = (
    await appWith(store).inject({ method: 'GET', url: '/v1/me/memos', headers: auth })
  ).json();

  assert.equal(own.memos.length, 1);
});
