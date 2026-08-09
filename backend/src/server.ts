import { buildApp } from './app.ts';
import { createVerifier } from './auth/verifier.ts';
import { MemoryStore } from './store/memory-store.ts';

/**
 * Entry point.
 *
 * Currently starts against [MemoryStore]. The Postgres store is the next piece
 * of work — the schema and migration runner exist, the `Store` interface is
 * settled, and swapping is one line here. Running on the in-memory store means
 * the Android client can be pointed at a real HTTP server today, which is worth
 * more right now than persistence nobody is reading yet.
 */
const port = Number(process.env.PORT ?? 8080);
const host = process.env.HOST ?? '0.0.0.0';

const app = buildApp({
  store: new MemoryStore(),
  verifier: createVerifier(process.env),
  logger: true,
});

app.listen({ port, host }).catch((error: unknown) => {
  app.log.error(error);
  process.exitCode = 1;
});
