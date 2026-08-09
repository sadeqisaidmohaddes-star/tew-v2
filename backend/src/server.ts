import { buildApp } from './app.ts';
import { createVerifier } from './auth/verifier.ts';
import { createPool, hasDatabase } from './db/pool.ts';
import { MemoryStore } from './store/memory-store.ts';
import { PostgresStore } from './store/postgres-store.ts';
import type { Store } from './store/types.ts';

/**
 * Entry point.
 *
 * Postgres when `DATABASE_URL` is set, in-memory otherwise. The fallback is
 * for local development and nothing else — it loses every memo on restart,
 * which is why it refuses to be the choice in production rather than quietly
 * being one.
 */
const port = Number(process.env.PORT ?? 8080);
const host = process.env.HOST ?? '0.0.0.0';

function createStore(): Store {
  if (hasDatabase(process.env)) {
    return new PostgresStore(createPool(process.env));
  }

  if (process.env.NODE_ENV === 'production') {
    throw new Error(
      'DATABASE_URL is not set. Refusing to start in production on the in-memory ' +
        'store — it loses every memo on restart, and losing a user\'s recording ' +
        'silently is worse than failing to boot.',
    );
  }

  console.warn('DATABASE_URL not set — using the in-memory store. Data will not survive restart.');
  return new MemoryStore();
}

const app = buildApp({
  store: createStore(),
  verifier: createVerifier(process.env),
  logger: true,
});

app.listen({ port, host }).catch((error: unknown) => {
  app.log.error(error);
  process.exitCode = 1;
});
