import { createReadStream, type ReadStream } from 'node:fs';
import { mkdir, rm, stat, writeFile } from 'node:fs/promises';
import { dirname, join, normalize, resolve } from 'node:path';

export interface StoredAudio {
  stream: ReadStream;
  contentType: string;
  sizeBytes: number;
}

/**
 * Where memo audio lives.
 *
 * An interface because the VPS will start with a directory on disk and may
 * later want object storage, and because the retention rule needs deletion to
 * be a real operation rather than a row update — non-negotiable #7 does not
 * accept "removed from the index but still on disk" as deleted.
 */
export interface AudioStore {
  put(key: string, data: Buffer, contentType: string): Promise<void>;
  get(key: string): Promise<StoredAudio | null>;
  /** Idempotent: deleting something already gone is not an error. */
  delete(key: string): Promise<void>;
}

/**
 * Filesystem-backed audio store.
 *
 * Keys are treated as untrusted. They arrive from the database, but the
 * database is written to by upload handlers, and a key like `../../etc/passwd`
 * reaching `join()` is the classic way a media endpoint becomes an arbitrary
 * file read. [safeKey] rejects anything that escapes the root, and it is
 * applied on read, write and delete rather than only on the path that looked
 * risky.
 */
export class FilesystemAudioStore implements AudioStore {
  private readonly root: string;

  constructor(root: string) {
    this.root = resolve(root);
  }

  async put(key: string, data: Buffer, contentType: string): Promise<void> {
    const path = this.pathFor(key);
    await mkdir(dirname(path), { recursive: true });
    await writeFile(path, data);
    await writeFile(`${path}.type`, contentType, 'utf8');
  }

  async get(key: string): Promise<StoredAudio | null> {
    const path = this.pathFor(key);
    try {
      const info = await stat(path);
      if (!info.isFile()) return null;
      return {
        stream: createReadStream(path),
        contentType: await this.contentTypeFor(path),
        sizeBytes: info.size,
      };
    } catch {
      return null;
    }
  }

  async delete(key: string): Promise<void> {
    const path = this.pathFor(key);
    await rm(path, { force: true });
    await rm(`${path}.type`, { force: true });
  }

  private async contentTypeFor(path: string): Promise<string> {
    try {
      const { readFile } = await import('node:fs/promises');
      return (await readFile(`${path}.type`, 'utf8')).trim() || DEFAULT_TYPE;
    } catch {
      return DEFAULT_TYPE;
    }
  }

  private pathFor(key: string): string {
    return join(this.root, safeKey(key));
  }
}

const DEFAULT_TYPE = 'audio/mp4';

/**
 * Reject any key that would escape the storage root.
 *
 * Throws rather than sanitising: a key that needed cleaning is a key that came
 * from somewhere unexpected, and quietly rewriting it would hide that.
 */
export function safeKey(key: string): string {
  if (!key || key.includes('\0')) throw new Error('invalid audio key');

  const cleaned = normalize(key).replace(/^(\.\.(\/|\\|$))+/, '');
  if (cleaned.startsWith('/') || cleaned.startsWith('\\') || cleaned.includes('..')) {
    throw new Error('invalid audio key');
  }
  if (cleaned !== key) throw new Error('invalid audio key');

  return cleaned;
}
