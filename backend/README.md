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
npm run dev        # http://localhost:8080
npm test           # 25 tests, no database needed
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

## Still open — these need a decision, not code

- **How the feed is ordered.** `BRIEF.md` forbids a ranking algorithm, but
  "not ranked" is not "no rule". The default implemented in
  `src/feed/ordering.ts` is: newest first, over memos you have not heard, in a
  seven-day window, **one memo per author per page**. That last rule is the
  closest thing here to ranking — it demotes real memos for reasons the poster
  did not choose. It exists because on a small network one person's ten memos
  would otherwise be everyone's entire day. It is a single config value and can
  be switched off. **This is Said's call, not an engineering default.**
- **Voice retention.** Non-negotiable #7 makes voice biometric data. How long
  is audio kept? Is it encrypted at rest? Does a removed memo's audio get
  destroyed or only hidden? The schema deletes a user's audio on account
  deletion and nothing else is decided. This is policy and it is the most
  important open question here.
- **Where moderation actually happens.** `android/README.md` cuts the in-app
  queue, so reports go somewhere else — a CLI, a small page, direct SQL. That
  choice changes what this service needs to expose.
- **Firebase project.** Unchosen. `TokenVerifier` is an interface with a stub
  behind it; the stub refuses to run in production.
- **VPS specs.** whisper.cpp model size depends on available CPU and RAM.

## Status

**API implemented against an in-memory store; Postgres store is the next
piece.** Schema, migration runner and the `Store` interface all exist —
swapping is one line in `src/server.ts`. Running on memory first means the
Android client can be pointed at a real HTTP server today, which is worth more
right now than persistence nobody is reading.

Not implemented yet: ASR (whisper.cpp), audio upload and object storage,
rate limiting, and the Firebase verifier.
