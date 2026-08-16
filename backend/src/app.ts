import Fastify, {
  type FastifyInstance,
  type FastifyReply,
  type FastifyRequest,
} from 'fastify';
import multipart from '@fastify/multipart';
import { randomUUID } from 'node:crypto';
import { bearerToken, type AuthedUser, type TokenVerifier } from './auth/verifier.ts';
import type { AudioStore } from './storage/audio-store.ts';
import type { ReportReason, Store } from './store/types.ts';

export interface AppOptions {
  store: Store;
  audio: AudioStore;
  verifier: TokenVerifier;
  /** Injected so tests control time rather than racing the clock. */
  now?: () => number;
  logger?: boolean;
}

/** 25 MB. Minutes of speech at the bitrate the client records. */
const MAX_AUDIO_BYTES = 25 * 1024 * 1024;

const REPORT_REASONS: readonly ReportReason[] = [
  'harassment',
  'hate_speech',
  'sexual_content',
  'spam',
  'other',
];

/**
 * The API.
 *
 * Every route is authenticated. There is no public read surface: memos are
 * voice recordings of identifiable people, non-negotiable #7 treats voice as
 * biometric data, and an anonymous endpoint returning them would make the whole
 * feed scrapeable by anyone who found it.
 */
export function buildApp(options: AppOptions): FastifyInstance {
  const { store, audio, verifier } = options;
  const now = options.now ?? (() => Math.floor(Date.now() / 1000));

  const app = Fastify({ logger: options.logger ?? false });

  // Memos are short speech, not music. The cap is generous enough that nobody
  // hits it in normal use and small enough that a bad actor cannot fill the
  // VPS disk with one request.
  app.register(multipart, { limits: { fileSize: MAX_AUDIO_BYTES, files: 1 } });

  /**
   * Resolve the caller, or reply 401.
   *
   * Returns null when it has already sent the reply, so handlers read as
   * `const user = await requireUser(...); if (!user) return;`
   */
  async function requireUser(
    request: FastifyRequest,
    reply: FastifyReply,
  ): Promise<AuthedUser | null> {
    const token = bearerToken(request.headers.authorization);
    const user = await verifier.verify(token);
    if (!user) {
      await reply.code(401).send({ error: 'unauthenticated' });
      return null;
    }
    await store.ensureUser(user.id, user.username);
    return user;
  }

  /**
   * Read the uploaded file, or null if the request did not carry one.
   *
   * @fastify/multipart throws on a non-multipart request, which Fastify maps
   * to 406 Not Acceptable. That is a confusing answer to "you forgot the
   * audio" — a client seeing it would look for a content negotiation problem
   * that does not exist. Caught here so both upload routes answer 400.
   */
  async function readAudio(request: FastifyRequest) {
    try {
      return (await request.file()) ?? null;
    } catch {
      return null;
    }
  }

  app.get('/health', async () => ({ ok: true }));

  /**
   * Serve a memo's audio.
   *
   * Authenticated like everything else. These are recordings of identifiable
   * people; non-negotiable #7 makes them biometric data, and an unauthenticated
   * media endpoint would make every memo in the system downloadable by anyone
   * who guessed a key.
   */
  app.get('/v1/audio/:key', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;

    const { key } = request.params as { key: string };
    let found;
    try {
      found = await audio.get(key);
    } catch {
      // safeKey throws on anything that would escape the storage root.
      return reply.code(400).send({ error: 'invalid_key' });
    }
    if (!found) return reply.code(404).send({ error: 'not_found' });

    return reply
      .header('Content-Type', found.contentType)
      .header('Content-Length', String(found.sizeBytes))
      // Private: a shared cache holding someone's voice is the same leak by
      // another route.
      .header('Cache-Control', 'private, max-age=3600')
      .send(found.stream);
  });

  /** Post a memo: multipart audio in, memo row out. */
  app.post('/v1/memos', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;

    const file = await readAudio(request);
    if (!file) return reply.code(400).send({ error: 'no_audio' });

    const data = await file.toBuffer();
    if (!data.length) return reply.code(400).send({ error: 'empty_audio' });

    const key = `${randomUUID()}.m4a`;
    await audio.put(key, data, file.mimetype || 'audio/mp4');

    const durationMs = Number((file.fields?.duration_ms as { value?: string })?.value ?? 0);
    return store.addMemo(user.id, key, Number.isFinite(durationMs) ? durationMs : 0);
  });

  /** Post a voice reply to a memo. */
  app.post('/v1/memos/:id/comments', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;

    const { id } = request.params as { id: string };
    const file = await readAudio(request);
    if (!file) return reply.code(400).send({ error: 'no_audio' });

    const data = await file.toBuffer();
    if (!data.length) return reply.code(400).send({ error: 'empty_audio' });

    const key = `${randomUUID()}.m4a`;
    await audio.put(key, data, file.mimetype || 'audio/mp4');

    const durationMs = Number((file.fields?.duration_ms as { value?: string })?.value ?? 0);
    const comment = await store.addComment(
      user.id, id, key, Number.isFinite(durationMs) ? durationMs : 0,
    );
    if (!comment) {
      // The memo vanished between upload and insert. Do not leave the file.
      await audio.delete(key);
      return reply.code(404).send({ error: 'not_found' });
    }
    return comment;
  });

  app.get('/v1/me', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    return { id: user.id, username: user.username };
  });

  app.get('/v1/feed', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;

    const query = request.query as { cursor?: string };
    // A malformed cursor is treated as "start again" inside decodeCursor
    // rather than rejected — see the note there. A user with a stale cursor
    // should get a feed, not a wall.
    return store.feed(user.id, query.cursor ?? null, now());
  });

  app.post('/v1/memos/:id/like', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const ok = await store.setLiked(user.id, id, true);
    if (!ok) return reply.code(404).send({ error: 'not_found' });
    return reply.code(204).send();
  });

  app.delete('/v1/memos/:id/like', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const ok = await store.setLiked(user.id, id, false);
    if (!ok) return reply.code(404).send({ error: 'not_found' });
    return reply.code(204).send();
  });

  app.post('/v1/memos/:id/skip', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const ok = await store.markHeard(user.id, id);
    if (!ok) return reply.code(404).send({ error: 'not_found' });
    return reply.code(204).send();
  });

  app.get('/v1/memos/:id/comments', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const comments = await store.comments(id);
    if (comments === null) return reply.code(404).send({ error: 'not_found' });
    return { comments };
  });

  app.post('/v1/memos/:id/report', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const body = request.body as { reason?: string } | undefined;
    const reason = body?.reason as ReportReason | undefined;

    if (!reason || !REPORT_REASONS.includes(reason)) {
      return reply.code(400).send({ error: 'unknown_reason' });
    }

    const ok = await store.report(user.id, id, reason);
    if (!ok) return reply.code(404).send({ error: 'not_found' });
    return reply.code(204).send();
  });

  /**
   * Delete one of your own memos, audio and all.
   *
   * Non-negotiable #7 makes voice biometric data, and the retention rule is:
   * a recording exists until the person deletes it or deletes their account.
   * There is no soft delete here — a "deleted" recording still sitting on
   * disk has not been deleted, and calling it deleted would be a lie told to
   * someone who cannot check.
   *
   * 404 covers both "no such memo" and "not yours", deliberately: telling
   * those apart would confirm another person's memo exists to a stranger.
   */
  app.delete('/v1/memos/:id', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const orphanedKeys = await store.deleteMemo(user.id, id);
    if (orphanedKeys === null) return reply.code(404).send({ error: 'not_found' });
    // The row is gone; now the recording. Skipping this would leave audio on
    // disk that nothing points at, which is the worst version of "deleted".
    await Promise.all(orphanedKeys.map((k) => audio.delete(k)));
    return reply.code(204).send();
  });

  /** Delete the account and everything recorded under it. Irreversible. */
  app.delete('/v1/me', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const orphanedKeys = await store.deleteAccount(user.id);
    await Promise.all(orphanedKeys.map((k) => audio.delete(k)));
    return reply.code(204).send();
  });

  app.get('/v1/me/memos', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    // Includes removed memos with their reasons — non-negotiable #8 means an
    // author can always see what happened to their own memo and why.
    return { memos: await store.ownMemos(user.id) };
  });

  app.post('/v1/memos/:id/appeal', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    const { id } = request.params as { id: string };
    const body = request.body as { text?: string } | undefined;
    const text = body?.text?.trim();

    if (!text) return reply.code(400).send({ error: 'empty_appeal' });

    const ok = await store.appeal(user.id, id, text);
    // 409 rather than 404: the memo exists, but it is not in a state that can
    // be appealed — already appealed, or never removed. Telling those apart
    // matters to a client deciding what to say out loud.
    if (!ok) return reply.code(409).send({ error: 'not_appealable' });
    return reply.code(204).send();
  });

  return app;
}
