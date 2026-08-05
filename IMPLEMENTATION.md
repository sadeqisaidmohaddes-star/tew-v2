# Implementation Brief

This file is the build brief for whichever agent (Claude Code or otherwise)
is implementing `tew-v2`. Read `BRIEF.md` and `STATE.md` first for context;
this file is the how, not the why.

## The one hard-coded performance rule

**Target device: a 3-year-old budget Android phone on throttled 3G.**
Not a flagship, not a fast connection — TEWW's mission is explicitly aimed
at BLV users in the Global South, where that's the realistic device most
users actually own. This is fixed, not a "we'll optimize later" target:

- Gesture/tap → audible feedback: **under 100ms**, on that baseline device.
  A screen-reader-dependent user has no visual cue that something happened —
  latency here isn't a UX nit, it's the interface breaking.
- Cold start to first playable memo: **under 3 seconds** on a throttled
  connection, with a spoken loading state if it takes longer than 1 second
  (never a silent screen — non-negotiable #1).
- No screen may block on a network call for more than 500ms with no audio
  or haptic feedback of some kind.

Profile against an actual low/mid-tier emulator profile or a real budget
device, not whatever hardware you're developing on. If a feature can't hit
these numbers on that baseline, simplify the feature — don't raise the
target.

## Project structure

```
tew-v2/
├── BRIEF.md
├── STATE.md
├── IMPLEMENTATION.md
├── HANDLING_PROTOCOLS.md
├── GITHUB_WORKFLOW.md
├── backend/
│   └── README.md
└── android/
    ├── README.md
    ├── core/              (:core — API client, auth session, Media3
    │                        playback session, repositories. No UI code.)
    ├── feature-radio/      (:feature-radio — sequential timeline screen)
    ├── feature-carddeck/   (:feature-carddeck — swipeable card screen,
    │                        gesture handling, onboarding, on-device
    │                        narrator)
    ├── feature-account/    (:feature-account — sign-in, minimal profile,
    │                        report + appeal. Shared by both feed models,
    │                        doesn't belong to either.)
    └── app/                (:app — thin shell, DI wiring, the
                              moderator-only screen-toggle for the
                              usability test)
```

New code goes in the module whose boundary it belongs to. If you're
importing Retrofit, Media3, or a repository type from inside a feature
module, that's a boundary violation — it belongs in `:core`. If you're
building sign-in, profile, or report/appeal UI inside `feature-radio` or
`feature-carddeck`, that's also a boundary violation — it belongs in
`:feature-account`, so it isn't duplicated in both and doesn't bias the
comparison between them.

## Tech stack

**Backend** (`backend/`)
- Runtime: Node.js + Fastify
- Database: Postgres (self-hosted on existing VPS — not SQLite, not a
  managed/serverless DB)
- Auth: Firebase Auth (Google Sign-In)
- ASR: whisper.cpp, local CPU subprocess
- E2EE (DMs): libsignal — backend-only for now; no DM screen ships in the
  Android app this phase
- Explicitly out of scope for this phase: server-side TTS, IVR/Twilio,
  anything web-only

**Android** (`android/`)
- Language: Kotlin
- UI: Jetpack Compose
- Playback: Media3
- Narration (card-deck only): Android's native `TextToSpeech` — not a
  server round-trip
- Networking: Retrofit against the backend's cursor-paginated feed API
- Not in this build: DMs, and any in-app moderator review queue (reports
  get submitted from the app; reviewing them happens outside it for now)

## Build order

1. Spike the TalkBack/gesture-passthrough mechanism in isolation (see
   `android/README.md`) before writing any real `feature-carddeck` code.
   This is the one open technical risk the whole card-deck direction
   depends on — find out early if it holds up.
2. `:core` — auth, API client, playback session. Nothing depends on this
   being polished, only correct; both feature modules block on it.
3. `feature-radio`, `feature-carddeck`, and `feature-account` can then
   proceed in parallel (see `HANDLING_PROTOCOLS.md`'s multi-agent
   section) — none of them depend on each other, only on `:core`.
4. `:app` wiring + the moderator toggle, once all three features exist.

## Process

How to handle files, context loss, interruption/resume, multiple agents,
and testing: `HANDLING_PROTOCOLS.md`.
Branching, PRs, issues, releases: `GITHUB_WORKFLOW.md`.
