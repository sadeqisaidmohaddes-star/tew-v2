import pg from 'pg';

/**
 * Postgres connection pool.
 *
 * Self-hosted on the project's own VPS per `IMPLEMENTATION.md` — not a managed
 * or serverless database, and not SQLite. The pool is small on purpose: this
 * runs alongside whisper.cpp on shared hardware, and a large idle pool spends
 * memory the ASR pass needs more.
 */
export function createPool(env: NodeJS.ProcessEnv): pg.Pool {
  const connectionString = env.DATABASE_URL;
  if (!connectionString) {
    throw new Error('DATABASE_URL is not set. See backend/.env.example.');
  }

  return new pg.Pool({
    connectionString,
    max: Number(env.PGPOOL_MAX ?? 8),
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 5_000,
  });
}

/** True when a database is configured — used to skip integration tests. */
export function hasDatabase(env: NodeJS.ProcessEnv): boolean {
  return Boolean(env.DATABASE_URL);
}
