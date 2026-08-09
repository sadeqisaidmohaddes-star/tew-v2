import { readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createPool } from './pool.ts';

/**
 * Applies every `.sql` file in `migrations/` in filename order, once each.
 *
 * Deliberately not a migration framework. There is one file today, the VPS is
 * hand-managed, and a framework would be another dependency to keep working on
 * hardware nobody wants to debug at 2am. Revisit when migrations need to be
 * reversible.
 */
const migrationsDir = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'migrations');

export async function migrate(env: NodeJS.ProcessEnv = process.env): Promise<string[]> {
  const pool = createPool(env);
  const applied: string[] = [];

  try {
    await pool.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        filename   TEXT PRIMARY KEY,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      )
    `);

    const files = (await readdir(migrationsDir)).filter((f) => f.endsWith('.sql')).sort();

    for (const file of files) {
      const { rowCount } = await pool.query(
        'SELECT 1 FROM schema_migrations WHERE filename = $1',
        [file],
      );
      if (rowCount && rowCount > 0) continue;

      const sql = await readFile(join(migrationsDir, file), 'utf8');

      // One transaction per migration: a half-applied schema on a hand-managed
      // box is a much worse afternoon than a failed migration.
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        await client.query(sql);
        await client.query('INSERT INTO schema_migrations (filename) VALUES ($1)', [file]);
        await client.query('COMMIT');
        applied.push(file);
      } catch (error) {
        await client.query('ROLLBACK');
        throw error;
      } finally {
        client.release();
      }
    }
  } finally {
    await pool.end();
  }

  return applied;
}

// Run directly: `npm run migrate`
if (import.meta.url === `file://${process.argv[1]}`) {
  migrate()
    .then((applied) => {
      console.log(applied.length ? `Applied: ${applied.join(', ')}` : 'Nothing to apply.');
    })
    .catch((error: unknown) => {
      console.error(error);
      process.exitCode = 1;
    });
}
