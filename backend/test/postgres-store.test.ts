import assert from 'node:assert/strict';
import { after, before, describe, test } from 'node:test';
import { buildApp } from '../src/app.ts';
import { tempAudioStore } from './helpers.ts';
import { StubTokenVerifier } from '../src/auth/verifier.ts';
import { createPool, hasDatabase } from '../src/db/pool.ts';
import { PostgresStore } from '../src/store/postgres-store.ts';

/**
 * The same behaviour the in-memory tests assert, run against real Postgres.
 *
 * These exist because the in-memory store is a reimplementation, not the thing
 * that ships. Every ordering, cursor and cascade rule tested elsewhere is
 * re-tested here against actual SQL — otherwise a passing suite would say
 * nothing about what the deployed service does.
 *
 * Skipped when `DATABASE_URL` is unset, so the default `npm test` needs no
 * database. `HANDLING_PROTOCOLS.md`: a local instance with seed data, never
 * the production VPS.
 *
 *   npm run db:up && DATABASE_URL=... npm test
 */
const NOW = 1_770_000_000;
const auth = { authorization: 'Bearer stub:u1:amina' };

describe('PostgresStore', { skip: hasDatabase(process.env) ? false : 'DATABASE_URL not set' }, () => {
  const pool = hasDatabase(process.env) ? createPool(process.env) : null;

  async function freshStore() {
    if (!pool) throw new Error('no pool');
    // Truncate rather than recreate: cascade also proves the FK graph is wired
    // the way the retention rule needs it to be.
    await pool.query('TRUNCATE users, memos, comments, likes, heard, reports, appeals CASCADE');
    return new PostgresStore(pool);
  }

  async function seed(
    id: string,
    authorId: string,
    secondsAgo: number,
    extra: { state?: string; reason?: string; appeal?: string } = {},
  ) {
    await pool!.query(
      `INSERT INTO memos (id, author_id, audio_key, duration_ms, posted_at,
                          moderation_state, moderation_reason, appeal_state)
       VALUES ($1, $2, $3, 10000, to_timestamp($4), $5, $6, $7)`,
      [
        id, authorId, `${id}.m4a`, NOW - secondsAgo,
        extra.state ?? 'visible', extra.reason ?? null, extra.appeal ?? 'not_applicable',
      ],
    );
  }

  after(async () => { await pool?.end(); });

  test('feed comes back newest first', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('older', 'u2', 300);
    await seed('newest', 'u2', 10);
    await seed('middle', 'u2', 100);

    const page = await store.feed('u1', null, NOW);

    assert.deepEqual(page.memos.map((m) => m.id), ['newest', 'middle', 'older']);
  });

  test('a prolific author is not throttled', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('loud', 'loud');
    for (let i = 0; i < 6; i++) await seed(`p${i}`, 'loud', i * 60);

    const page = await store.feed('u1', null, NOW);

    assert.equal(page.memos.length, 6);
  });

  test('pagination reaches the end and stops', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    for (let i = 0; i < 23; i++) await seed(`m${i}`, 'u2', i * 60);

    const seen = new Set<string>();
    let cursor: string | null = null;
    let pages = 0;

    do {
      const page = await store.feed('u1', cursor, NOW);
      for (const m of page.memos) {
        assert.ok(!seen.has(m.id), `memo ${m.id} served twice`);
        seen.add(m.id);
      }
      cursor = page.next_cursor;
      assert.ok(++pages < 20, 'feed never terminated');
    } while (cursor);

    assert.equal(seen.size, 23, 'pagination skipped memos');
  });

  test('skipped memos do not come back', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('skip', 'u2', 10);
    await seed('keep', 'u2', 20);

    await store.markHeard('u1', 'skip');

    const page = await store.feed('u1', null, NOW);
    assert.deepEqual(page.memos.map((m) => m.id), ['keep']);
  });

  test('likes are per user and reversible', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('m', 'u2', 10);

    await store.setLiked('u1', 'm', true);
    assert.equal((await store.feed('u1', null, NOW)).memos[0]!.liked_by_me, true);
    // Somebody else's like must not show as yours.
    assert.equal((await store.feed('u2', null, NOW)).memos[0]!.liked_by_me, false);

    await store.setLiked('u1', 'm', false);
    assert.equal((await store.feed('u1', null, NOW)).memos[0]!.liked_by_me, false);
  });

  test('removed memos stay out of the feed but reach their author', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await seed('gone', 'u1', 10, {
      state: 'removed', reason: 'Broke the rules.', appeal: 'available',
    });

    assert.equal((await store.feed('u1', null, NOW)).memos.length, 0);

    const own = await store.ownMemos('u1');
    assert.equal(own.length, 1);
    assert.equal(own[0]!.moderation.reason, 'Broke the rules.');
  });

  test('an appeal can only be filed once', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await seed('gone', 'u1', 10, { state: 'removed', appeal: 'available' });

    assert.equal(await store.appeal('u1', 'gone', 'please look again'), true);
    assert.equal(await store.appeal('u1', 'gone', 'again'), false);
  });

  test('deleting a memo takes its comments, likes and heard-markers', async () => {
    // The retention rule depends on the cascade actually being wired, not on
    // the API remembering to tidy up.
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('mine', 'u1', 10);
    await store.addComment('u2', 'mine', 'reply.m4a', 3000);
    await store.setLiked('u2', 'mine', true);
    await store.markHeard('u2', 'mine');

    const removed = await store.deleteMemo('u1', 'mine');
    assert.ok(removed, 'delete returned null');
    assert.equal(removed!.length, 2, 'expected the memo audio and its reply audio');

    for (const table of ['comments', 'likes', 'heard']) {
      const { rows } = await pool!.query(`SELECT count(*)::int AS n FROM ${table}`);
      assert.equal(rows[0].n, 0, `${table} still had rows after the memo was deleted`);
    }
  });

  test('you cannot delete a memo that is not yours', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('theirs', 'u2', 10);

    assert.equal(await store.deleteMemo('u1', 'theirs'), null);
    assert.equal((await store.ownMemos('u2')).length, 1);
  });

  test('deleting an account destroys everything that person recorded', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('a', 'u1', 10);
    await seed('b', 'u1', 20);
    await seed('other', 'u2', 30);
    await store.addComment('u1', 'other', 'reply.m4a', 3000);

    await store.deleteAccount('u1');

    const { rows: memos } = await pool!.query('SELECT id FROM memos');
    assert.deepEqual(memos.map((r: { id: string }) => r.id), ['other']);

    const { rows: comments } = await pool!.query('SELECT count(*)::int AS n FROM comments');
    assert.equal(comments[0].n, 0, "the deleted user's comment survived");

    const { rows: users } = await pool!.query('SELECT count(*)::int AS n FROM users');
    assert.equal(users[0].n, 1);
  });

  test('posting a memo returns it, and it appears in the feed', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');

    const posted = await store.addMemo('u2', 'fresh.m4a', 12_000);

    assert.equal(posted.author_username, 'joseph');
    assert.equal(posted.duration_ms, 12_000);
    assert.equal(posted.liked_by_me, false);

    const page = await store.feed('u1', null, NOW + 5);
    assert.deepEqual(page.memos.map((m) => m.id), [posted.id]);
  });

  test('the API serves no counts', async () => {
    const store = await freshStore();
    await store.ensureUser('u1', 'amina');
    await store.ensureUser('u2', 'joseph');
    await seed('m', 'u2', 10);
    await store.setLiked('u1', 'm', true);

    const app = buildApp({ store, audio: tempAudioStore(), verifier: new StubTokenVerifier(), now: () => NOW });
    const raw = (await app.inject({ method: 'GET', url: '/v1/feed', headers: auth })).body;

    assert.ok(!/_count"/.test(raw), `a count leaked onto the wire: ${raw}`);
  });
});
