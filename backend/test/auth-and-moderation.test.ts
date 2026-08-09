import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { StubTokenVerifier, bearerToken, createVerifier } from '../src/auth/verifier.ts';
import { MemoryStore } from '../src/store/memory-store.ts';

const NOW = 1_770_000_000;
const auth = { authorization: 'Bearer stub:u1:amina' };

function appWith(store: MemoryStore) {
  return buildApp({ store, verifier: new StubTokenVerifier(), now: () => NOW });
}

// ------------------------------------------------------------------- auth

test('bearer tokens are parsed, junk is not', () => {
  assert.equal(bearerToken('Bearer abc'), 'abc');
  assert.equal(bearerToken('bearer abc'), 'abc');
  assert.equal(bearerToken('Basic abc'), null);
  assert.equal(bearerToken(undefined), null);
});

test('the stub verifier refuses to run in production', () => {
  // A backend that silently accepts any token because a credential was
  // missing is worse than one that will not start. Voice is biometric data.
  assert.throws(
    () => createVerifier({ NODE_ENV: 'production' } as NodeJS.ProcessEnv),
    /Refusing to start in production/,
  );
});

test('setting Firebase credentials fails loudly until the verifier exists', () => {
  assert.throws(
    () =>
      createVerifier({
        FIREBASE_PROJECT_ID: 'x',
        FIREBASE_CLIENT_EMAIL: 'y',
      } as NodeJS.ProcessEnv),
    /not implemented yet/,
  );
});

test('every route needs authentication', async () => {
  // There is no public read surface: memos are recordings of identifiable
  // people, and an anonymous endpoint would make the feed scrapeable.
  const app = appWith(new MemoryStore());

  for (const url of ['/v1/me', '/v1/feed', '/v1/me/memos']) {
    const res = await app.inject({ method: 'GET', url });
    assert.equal(res.statusCode, 401, `${url} was reachable without a token`);
  }
});

test('health needs no token', async () => {
  const res = await appWith(new MemoryStore()).inject({ method: 'GET', url: '/health' });
  assert.equal(res.statusCode, 200);
});

// ------------------------------------------------------------- moderation

test('own memos include removed ones, with their reasons', async () => {
  // Non-negotiable #8: an author can always see what happened to their memo.
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({
    id: 'gone',
    authorId: 'u1',
    postedAt: NOW - 100,
    state: 'removed',
    reason: 'A moderator agreed this broke the community rules.',
    appeal: 'available',
  });

  const body = (
    await appWith(store).inject({ method: 'GET', url: '/v1/me/memos', headers: auth })
  ).json();

  assert.equal(body.memos.length, 1);
  assert.equal(body.memos[0].moderation.state, 'removed');
  assert.match(body.memos[0].moderation.reason, /community rules/);
});

test('an appeal can be submitted once', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({
    id: 'gone',
    authorId: 'u1',
    postedAt: NOW - 100,
    state: 'removed',
    appeal: 'available',
  });

  const app = appWith(store);
  const first = await app.inject({
    method: 'POST',
    url: '/v1/memos/gone/appeal',
    headers: auth,
    payload: { text: 'This was a misunderstanding.' },
  });
  assert.equal(first.statusCode, 204);

  // Second attempt is 409, not 404 — the memo exists but is no longer
  // appealable, and the client says different things for those two.
  const second = await app.inject({
    method: 'POST',
    url: '/v1/memos/gone/appeal',
    headers: auth,
    payload: { text: 'Again' },
  });
  assert.equal(second.statusCode, 409);
});

test('an empty appeal is rejected', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'gone', authorId: 'u1', postedAt: NOW, state: 'removed', appeal: 'available' });

  const res = await appWith(store).inject({
    method: 'POST',
    url: '/v1/memos/gone/appeal',
    headers: auth,
    payload: { text: '   ' },
  });

  assert.equal(res.statusCode, 400);
});

test('you cannot appeal someone else memo', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({
    id: 'theirs',
    authorId: 'someone-else',
    postedAt: NOW,
    state: 'removed',
    appeal: 'available',
  });

  const res = await appWith(store).inject({
    method: 'POST',
    url: '/v1/memos/theirs/appeal',
    headers: auth,
    payload: { text: 'let me in' },
  });

  assert.equal(res.statusCode, 409);
});

test('reports accept only known reasons', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'm', authorId: 'other', postedAt: NOW });

  const app = appWith(store);

  const good = await app.inject({
    method: 'POST',
    url: '/v1/memos/m/report',
    headers: auth,
    payload: { reason: 'harassment' },
  });
  assert.equal(good.statusCode, 204);

  const bad = await app.inject({
    method: 'POST',
    url: '/v1/memos/m/report',
    headers: auth,
    payload: { reason: 'i just do not like it' },
  });
  assert.equal(bad.statusCode, 400);
});

test('acting on a memo that does not exist is a 404', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  const app = appWith(store);

  for (const url of ['/v1/memos/nope/like', '/v1/memos/nope/skip']) {
    const res = await app.inject({ method: 'POST', url, headers: auth });
    assert.equal(res.statusCode, 404, url);
  }
});

test('likes toggle', async () => {
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'm', authorId: 'other', postedAt: NOW - 10 });
  const app = appWith(store);

  await app.inject({ method: 'POST', url: '/v1/memos/m/like', headers: auth });
  let body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.equal(body.memos[0].liked_by_me, true);

  await app.inject({ method: 'DELETE', url: '/v1/memos/m/like', headers: auth });
  body = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).json();
  assert.equal(body.memos[0].liked_by_me, false);
});
