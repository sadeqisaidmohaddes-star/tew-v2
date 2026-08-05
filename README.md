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

## Status

Design stage. No implementation yet.
