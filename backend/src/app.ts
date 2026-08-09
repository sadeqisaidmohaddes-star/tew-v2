import Fastify, {
  type FastifyInstance,
  type FastifyReply,
  type FastifyRequest,
} from 'fastify';
import { bearerToken, type AuthedUser, type TokenVerifier } from './auth/verifier.ts';
import type { ReportReason, Store } from './store/types.ts';

export interface AppOptions {
  store: Store;
  verifier: TokenVerifier;
  /** Injected so tests control time rather than racing the clock. */
  now?: () => number;
  logger?: boolean;
}

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
  const { store, verifier } = options;
  const now = options.now ?? (() => Math.floor(Date.now() / 1000));

  const app = Fastify({ logger: options.logger ?? false });

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

  app.get('/health', async () => ({ ok: true }));

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
    const ok = await store.deleteMemo(user.id, id);
    if (!ok) return reply.code(404).send({ error: 'not_found' });
    return reply.code(204).send();
  });

  /** Delete the account and everything recorded under it. Irreversible. */
  app.delete('/v1/me', async (request, reply) => {
    const user = await requireUser(request, reply);
    if (!user) return;
    await store.deleteAccount(user.id);
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
