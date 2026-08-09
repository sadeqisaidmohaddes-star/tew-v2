import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { StubTokenVerifier } from '../src/auth/verifier.ts';
import { safeKey } from '../src/storage/audio-store.ts';
import { MemoryStore } from '../src/store/memory-store.ts';
import { tempAudioStore } from './helpers.ts';

const NOW = 1_770_000_000;
const auth = { authorization: 'Bearer stub:u1:amina' };

test('keys that escape the storage root are rejected', () => {
  // A media endpoint that joins an untrusted key onto a root path is the
  // classic route to arbitrary file read. These are rejected rather than
  // sanitised: a key needing cleanup came from somewhere unexpected, and
  // quietly rewriting it would hide that.
  for (const bad of [
    '../etc/passwd',
    '../../etc/passwd',
    '/etc/passwd',
    'nested/../../escape.m4a',
    'a\0b.m4a',
    '',
  ]) {
    assert.throws(() => safeKey(bad), new RegExp('invalid audio key'), `accepted ${JSON.stringify(bad)}`);
  }
});

test('ordinary keys are accepted unchanged', () => {
  assert.equal(safeKey('abc.m4a'), 'abc.m4a');
  assert.equal(safeKey('seed-amina-4.m4a'), 'seed-amina-4.m4a');
});

test('audio needs authentication', async () => {
  // These are recordings of identifiable people. An unauthenticated media
  // endpoint would make every memo downloadable by anyone who guessed a key.
  const app = buildApp({
    store: new MemoryStore(),
    audio: tempAudioStore(),
    verifier: new StubTokenVerifier(),
    now: () => NOW,
  });

  const res = await app.inject({ method: 'GET', url: '/v1/audio/anything.m4a' });
  assert.equal(res.statusCode, 401);
});

test('a stored recording round-trips', async () => {
  const audio = tempAudioStore();
  await audio.put('hello.m4a', Buffer.from('fake-audio-bytes'), 'audio/mp4');

  const app = buildApp({
    store: new MemoryStore(),
    audio,
    verifier: new StubTokenVerifier(),
    now: () => NOW,
  });

  const res = await app.inject({ method: 'GET', url: '/v1/audio/hello.m4a', headers: auth });

  assert.equal(res.statusCode, 200);
  assert.equal(res.headers['content-type'], 'audio/mp4');
  assert.equal(res.body, 'fake-audio-bytes');
  // A shared cache holding someone's voice is the same leak by another route.
  assert.match(String(res.headers['cache-control']), /private/);
});

test('a missing recording is a 404, not an error', async () => {
  const app = buildApp({
    store: new MemoryStore(),
    audio: tempAudioStore(),
    verifier: new StubTokenVerifier(),
    now: () => NOW,
  });

  const res = await app.inject({ method: 'GET', url: '/v1/audio/nope.m4a', headers: auth });
  assert.equal(res.statusCode, 404);
});

test('a traversal key is refused by the route, not just the store', async () => {
  const app = buildApp({
    store: new MemoryStore(),
    audio: tempAudioStore(),
    verifier: new StubTokenVerifier(),
    now: () => NOW,
  });

  // Encoded so it survives to the handler rather than being normalised away
  // by a client or proxy — which is exactly how this reaches a real server.
  const res = await app.inject({
    method: 'GET',
    url: '/v1/audio/..%2F..%2Fetc%2Fpasswd',
    headers: auth,
  });

  assert.equal(res.statusCode, 400);
});

test('deleting a memo deletes its recording from storage', async () => {
  // The retention rule end to end: the row going is not enough. Audio left on
  // disk with nothing pointing at it is the worst version of "deleted" —
  // it survives and nobody knows it is there.
  const audio = tempAudioStore();
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'mine', authorId: 'u1', postedAt: NOW - 10, audioKey: 'mine.m4a' });
  await audio.put('mine.m4a', Buffer.from('bytes'), 'audio/mp4');

  const app = buildApp({ store, audio, verifier: new StubTokenVerifier(), now: () => NOW });

  assert.ok(await audio.get('mine.m4a'), 'setup failed — audio was not stored');

  const res = await app.inject({ method: 'DELETE', url: '/v1/memos/mine', headers: auth });
  assert.equal(res.statusCode, 204);

  assert.equal(await audio.get('mine.m4a'), null, 'the recording survived the delete');
});

test('deleting the account deletes every recording it held', async () => {
  const audio = tempAudioStore();
  const store = new MemoryStore();
  await store.ensureUser('u1', 'amina');
  store.seedMemo({ id: 'a', authorId: 'u1', postedAt: NOW - 10, audioKey: 'a.m4a' });
  store.seedMemo({ id: 'b', authorId: 'u1', postedAt: NOW - 20, audioKey: 'b.m4a' });
  await audio.put('a.m4a', Buffer.from('x'), 'audio/mp4');
  await audio.put('b.m4a', Buffer.from('y'), 'audio/mp4');

  const app = buildApp({ store, audio, verifier: new StubTokenVerifier(), now: () => NOW });
  await app.inject({ method: 'DELETE', url: '/v1/me', headers: auth });

  assert.equal(await audio.get('a.m4a'), null);
  assert.equal(await audio.get('b.m4a'), null);
});

test('posting a memo with no audio is refused', async () => {
  const app = buildApp({
    store: new MemoryStore(),
    audio: tempAudioStore(),
    verifier: new StubTokenVerifier(),
    now: () => NOW,
  });

  const res = await app.inject({ method: 'POST', url: '/v1/memos', headers: auth });
  assert.equal(res.statusCode, 400);
});
