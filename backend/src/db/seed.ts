import { execFile } from 'node:child_process';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';
import { createPool } from './pool.ts';
import { FilesystemAudioStore } from '../storage/audio-store.ts';

const run = promisify(execFile);

/**
 * Seed data for the internal prototype test.
 *
 * ## Why the audio is generated rather than a placeholder tone
 *
 * TEW is a voice-first app for people who cannot see the screen. A feed of
 * identical beeps would let you check that playback *starts*, and tell you
 * nothing about whether the app is usable — whether you can tell memos apart,
 * whether the narrator collides with the audio, whether skipping mid-sentence
 * feels right. So each memo is synthesised from its own transcript with
 * espeak-ng, with a different voice per person, and encoded to the same
 * AAC/MP4 the client records.
 *
 * They are obviously synthetic. That is fine for a mechanical test and is not
 * a substitute for real BLV testers reading their own words — but it is much
 * closer to the real thing than a bell.
 *
 * ## The people are invented
 *
 * Deliberately. Non-negotiable #7 makes voice biometric data, and seeding a
 * database with recordings of real people to test a prototype would be the
 * wrong way to start a project about consent.
 *
 * Safe to run repeatedly: it clears seeded rows first. It refuses to run
 * against a database that has non-seed users, so it cannot wipe a real test
 * session by accident.
 */

interface SeedUser {
  id: string;
  username: string;
  /** espeak-ng voice, so people sound different from each other. */
  voice: string;
}

interface SeedMemo {
  author: string;
  minutesAgo: number;
  transcript: string;
  removed?: { reason: string };
}

const USERS: SeedUser[] = [
  { id: 'seed-amina', username: 'amina', voice: 'en-gb+f3' },
  { id: 'seed-joseph', username: 'joseph', voice: 'en-gb+m3' },
  { id: 'seed-fatima', username: 'fatima', voice: 'en-us+f4' },
  { id: 'seed-daniel', username: 'daniel', voice: 'en-us+m2' },
  { id: 'seed-grace', username: 'grace', voice: 'en-gb+f2' },
  { id: 'seed-samuel', username: 'samuel', voice: 'en+m5' },
  // The account a tester signs in as. The Android stub reports this id.
  { id: 'stub-user', username: 'test-user', voice: 'en+f1' },
];

const MEMOS: SeedMemo[] = [
  {
    author: 'seed-amina', minutesAgo: 4,
    transcript:
      'Good morning everyone. I walked to the market on my own today for the first time since I moved here. I am still shaking a bit, but I did it.',
  },
  {
    author: 'seed-joseph', minutesAgo: 19,
    transcript:
      'Does anyone else find the bus announcements too quiet? I keep missing my stop and having to walk back.',
  },
  {
    author: 'seed-fatima', minutesAgo: 47,
    transcript:
      'My daughter recorded her school song for me this morning. I have listened to it about nine times now. I am not sorry.',
  },
  {
    author: 'seed-daniel', minutesAgo: 96,
    transcript:
      'Question for the group. What is your trick for telling shampoo and conditioner apart? I have tried elastic bands and I keep getting it wrong.',
  },
  {
    author: 'seed-grace', minutesAgo: 140,
    transcript:
      'I just wanted to say that hearing other people voices in the evening helps more than I expected it to. That is all. Goodnight.',
  },
  {
    author: 'seed-samuel', minutesAgo: 200,
    transcript: 'Rain on a tin roof. That is the whole memo. Enjoy it.',
  },
  {
    author: 'seed-joseph', minutesAgo: 260,
    transcript:
      'Following up on the bus thing. Someone suggested asking the driver to call out my stop, and it worked. Thank you whoever that was.',
  },
  {
    author: 'seed-amina', minutesAgo: 320,
    transcript:
      'Small thing. The tactile paving outside the library has been repaired. It has been broken for two years.',
  },
  // The tester's own memos, so the profile screen is not empty.
  {
    author: 'stub-user', minutesAgo: 30,
    transcript: 'Testing this out. Hello from my kitchen.',
  },
  {
    // Gives the moderation reason and appeal flow something real to show.
    author: 'stub-user', minutesAgo: 400,
    transcript: 'This memo was taken down, so the appeal screen has something to open.',
    removed: {
      reason:
        'A listener reported this memo for harassment, and a moderator agreed it broke the community rules. If you think that was wrong, you can ask for another look.',
    },
  },
];

/** Speak [text] and encode it the way the client records: AAC in MP4. */
async function synthesise(text: string, voice: string): Promise<{ data: Buffer; durationMs: number }> {
  const dir = await mkdtemp(join(tmpdir(), 'tew-seed-'));
  const wav = join(dir, 'speech.wav');
  const m4a = join(dir, 'speech.m4a');

  try {
    await run('espeak-ng', ['-v', voice, '-s', '150', '-w', wav, text]);
    await run('ffmpeg', [
      '-hide_banner', '-loglevel', 'error', '-y',
      '-i', wav,
      '-c:a', 'aac', '-b:a', '32k', '-ar', '22050', '-ac', '1',
      m4a,
    ]);

    const data = await readFile(m4a);
    const { stdout } = await run('ffprobe', [
      '-v', 'error', '-show_entries', 'format=duration',
      '-of', 'default=noprint_wrappers=1:nokey=1', m4a,
    ]);
    return { data, durationMs: Math.round(parseFloat(stdout.trim()) * 1000) };
  } finally {
    await rm(dir, { recursive: true, force: true });
  }
}

export async function seed(env: NodeJS.ProcessEnv = process.env): Promise<void> {
  const pool = createPool(env);
  const audio = new FilesystemAudioStore(env.AUDIO_DIR ?? './data/audio');
  const seedIds = USERS.map((u) => u.id);

  try {
    // Refuse to touch a database holding anyone this script did not create.
    // Seeding is destructive, and wiping a live internal test because someone
    // ran the wrong command would lose real people's recordings.
    const { rows: strangers } = await pool.query(
      `SELECT id FROM users WHERE id <> ALL($1::text[]) LIMIT 1`,
      [seedIds],
    );
    if (strangers.length) {
      throw new Error(
        `Refusing to seed: this database has users that were not created by the seed ` +
          `script (for example "${strangers[0].id}"). Seeding deletes data. If you are ` +
          `sure, drop those rows yourself first.`,
      );
    }

    // Clear previous seed audio before the rows that name it are removed.
    const { rows: oldKeys } = await pool.query(
      `SELECT audio_key FROM memos UNION ALL SELECT audio_key FROM comments`,
    );
    await Promise.all(oldKeys.map((r: { audio_key: string }) => audio.delete(r.audio_key)));
    await pool.query(`DELETE FROM users WHERE id = ANY($1::text[])`, [seedIds]);

    for (const user of USERS) {
      await pool.query(`INSERT INTO users (id, username) VALUES ($1, $2)`, [
        user.id, user.username,
      ]);
    }

    const voices = new Map(USERS.map((u) => [u.id, u.voice]));
    let created = 0;

    for (const memo of MEMOS) {
      const voice = voices.get(memo.author) ?? 'en';
      const { data, durationMs } = await synthesise(memo.transcript, voice);

      const key = `seed-${memo.author}-${memo.minutesAgo}.m4a`;
      await audio.put(key, data, 'audio/mp4');

      await pool.query(
        `INSERT INTO memos (id, author_id, audio_key, duration_ms, posted_at, transcript,
                            moderation_state, moderation_reason, appeal_state)
         VALUES (gen_random_uuid()::text, $1, $2, $3, NOW() - ($4 || ' minutes')::interval,
                 $5, $6, $7, $8)`,
        [
          memo.author, key, durationMs, String(memo.minutesAgo), memo.transcript,
          memo.removed ? 'removed' : 'visible',
          memo.removed?.reason ?? null,
          memo.removed ? 'available' : 'not_applicable',
        ],
      );
      created++;
      console.log(`  ${memo.author.padEnd(14)} ${(durationMs / 1000).toFixed(1)}s  ${key}`);
    }

    console.log(`\nSeeded ${USERS.length} users and ${created} memos.`);
    console.log('Sign in on the Android app to see them — the stub signs you in as "test-user".');
  } finally {
    await pool.end();
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  seed()
    .catch((error: unknown) => {
      console.error(error instanceof Error ? error.message : error);
      process.exitCode = 1;
    });
}
