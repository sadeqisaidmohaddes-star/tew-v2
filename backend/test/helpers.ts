import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { FilesystemAudioStore } from '../src/storage/audio-store.ts';

/** A throwaway audio store per test run, so tests never share files. */
export function tempAudioStore(): FilesystemAudioStore {
  return new FilesystemAudioStore(mkdtempSync(join(tmpdir(), 'tew-audio-')));
}
