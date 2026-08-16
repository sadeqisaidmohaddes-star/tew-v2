# TEW v2 — Backend

Fresh, Android-only backend for Third Eye World, restructured out of the
original `third-eye-world-mono` service. This repo covers the API and data
layer only — the Android client (shared core + two UI shells: radio-timeline
and card-deck) lives in its own repo.

## Why a restructure

The original backend carried web, iOS, Android, and IVR concerns at once.
This phase only serves an Android client, so the backend is scoped down to
match — not because anything was wrong with it, but because a lot of it
(TTS service, IVR call handling, web-only auth) has no consumer right now.

## What's in scope

- **Auth** — Firebase Auth (Google Sign-In). Replaces the old magic-link
  flow, which depended on an email/SMS provider that was never arranged.
  Android already requires a Google account for Play Store access, so this
  removes an infra dependency instead of adding one.
- **Feed** — one cursor-paginated API for memos, likes, and skip/next.
  Radio-timeline (auto-play sequentially) and card-deck (swipe through) are
  both just client-side ways of consuming the same ordered feed — there is
  no per-UI-model fork in the API.
- **ASR** — whisper.cpp run as a local CPU subprocess, ported as-is from the
  validated original implementation. A transcript is generated once per
  memo and stored, not regenerated per listener.
- **Moderation** — report queue with appeals, ported as-is.
- **Direct messages** — end-to-end encryption via libsignal, ported as-is.

## Explicitly out of scope for this phase

- **TTS** — moves on-device, using Android's native `TextToSpeech` engine
  for in-app narration. No server-side TTS service in this repo.
- **IVR** (phone-call access via Twilio/TwiML) — irrelevant to an Android
  client; can come back if a phone-access channel is ever built again.
- Anything web-specific.

## Tech stack

- **Runtime:** Node.js + Fastify, ported from the validated original service.
- **Database:** Postgres, self-hosted on existing VPS infra.
- **ASR:** whisper.cpp (CPU subprocess).
- **E2EE:** libsignal (prebuilt bindings).
- **Auth:** Firebase Auth.

## Running it

```
cp .env.example .env
npm install
npm run db:up      # local Postgres in Docker
npm run migrate    # applies migrations/
npm run seed       # 7 users, 10 memos, real spoken audio
npm run dev        # http://localhost:8080
npm test           # 34 tests, no database needed

# With a database, the Postgres store is exercised too (46 total):
DATABASE_URL=postgres://tew:tew@localhost:5433/tew npm test
```

Tests run against an in-memory store rather than Postgres, deliberately — they
cover feed behaviour (does the stream end, can one author fill a page, does a
skipped memo come back), and a suite that needs a database is a suite that
stops being run. The in-memory store implements the same ordering and cursor
rules as the real one, so those tests are meaningful rather than decorative.

`HANDLING_PROTOCOLS.md` still applies for real database work: local instance
with seed data, never the production VPS.

## Decisions that were open, and how they are settled

`android/core/API_CONTRACT.md` listed four questions for whoever wrote this.
Answers, now implemented:

| Question | Answer |
| --- | --- |
| Is the cursor opaque? | Yes — base64url keyset on `(posted_at, id)`. Not an offset; the feed changes under the reader. |
| Do own memos carry human-readable removal reasons? | Yes, stored as prose. Moderation wording changes without an app release. |
| What audio container? | Client uploads AAC/MP4; the server transcodes for whisper.cpp. The client should not have to know what the ASR wants. |
| Rate limiting? | Planned as `429` + `Retry-After`. Not implemented yet. |

## Seed data

`npm run seed` creates seven invented people and ten memos, with **real spoken
audio** generated from each transcript by espeak-ng and encoded to the same
AAC/MP4 the client records. A different voice per person, so they are
distinguishable by ear.

Why not a placeholder tone: TEW is for people who cannot see the screen. A feed
of identical beeps proves playback starts and tells you nothing about whether
the app is *usable* — whether memos are tellable apart, whether the narrator
collides with the audio, whether skipping mid-sentence feels right. The audio is
obviously synthetic, which is fine for a mechanical test and is not a substitute
for real BLV testers reading their own words.

The people are invented on purpose. Non-negotiable #7 makes voice biometric
data, and seeding a prototype with recordings of real people would be the wrong
way to begin a project about consent.

Seeding is destructive, so it **refuses to run against a database holding any
user it did not create** — it cannot wipe a live internal test by accident.

Needs `espeak-ng` and `ffmpeg` on the machine running it (`apt install espeak-ng
ffmpeg`), not on the server.

One seeded memo is deliberately in the `removed` state with a reason, so the
moderation and appeal screens have something real to open.

## Audio storage

Audio lives on disk under `AUDIO_DIR` (default `./data/audio`) behind an
`AudioStore` interface, so object storage can replace it later without touching
route code. **Mount a volume there in production** or recordings disappear on a
container rebuild.

`GET /v1/audio/:key` serves it, authenticated like everything else — these are
recordings of identifiable people, and an open media endpoint would make every
memo downloadable by anyone who guessed a key. Keys that would escape the
storage root are rejected rather than sanitised.

## Decided by Said

**Feed ordering: newest to oldest.** Strict reverse-chronological over memos
you have not heard. No scoring, no personalisation, no per-author quota. An
earlier draft capped each author to one memo per page; that is gone, because it
demoted real memos for reasons the poster did not choose, which `BRIEF.md`
rules out. There is a test asserting nothing reorders the feed by author.

**Voice retention: audio exists until the person deletes it.** Either they
delete the memo (`DELETE /v1/memos/:id`) or they delete their account
(`DELETE /v1/me`), and then it is gone — row removed, cascade takes comments,
likes and heard-markers, audio object deleted from storage. **No time-based
expiry and no soft delete.** A "deleted" recording still sitting on disk has
not been deleted, and non-negotiable #7 leaves no room for that distinction.
There is no `deleted_at` column, and its absence is the policy.

## Still open

- **Where moderation actually happens.** `android/README.md` cuts the in-app
  queue, so reports go somewhere else — a CLI, a small page, direct SQL. That
  choice changes what this service needs to expose.
- **Firebase project.** Not needed for the internal prototype test, per Said.
  `TokenVerifier` is an interface with a stub behind it; the stub refuses to
  run with `NODE_ENV=production`.
- **VPS specs.** whisper.cpp model size depends on available CPU and RAM.
- **The Android app has no delete UI yet.** The API honours the retention rule;
  nothing in the client calls it. Until that lands, the promise is only half
  deliverable.

## Deploying

See [`DEPLOY.md`](DEPLOY.md). Docker image, production compose, and a runbook
for the VPS — including the one decision that has to be made before it goes on
a public hostname: the stub verifier accepts any token, and the API refuses to
start in production because of it.

## Status

**API implemented, on Postgres.** `DATABASE_URL` selects the Postgres store;
without it the in-memory store is used for local development only, and the
service refuses to start that way in production — losing a user's recording
silently is worse than failing to boot.

Audio upload, storage and serving now work end to end: `POST /v1/memos` and
`POST /v1/memos/:id/comments` accept multipart audio, `GET /v1/audio/:key`
serves it, and deleting a memo or an account deletes the files too.

Not implemented yet: ASR (whisper.cpp), rate limiting, and the Firebase
verifier.
