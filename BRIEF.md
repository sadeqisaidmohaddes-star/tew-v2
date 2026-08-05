# TEW v2 — Brief

Third Eye World (TEW) is a voice-first social network for blind and
low-vision (BLV) people, built by Third Eye Worldwide (TEWW), a nonprofit
founded by Said Mohaddes Sadeqi. Every post is a short voice memo. The
interaction set is deliberately narrow: like, comment, skip. No follow
graph, no ranking algorithm, no engagement metrics. The product targets
loneliness, not engagement — the headline success metric is the UCLA
Loneliness Scale, not time-on-app.

## What this repo is

`tew-v2` is a fresh restart of TEW's backend and Android client, replacing
the backend/Android portions of the earlier `third-eye-world-mono`. This
phase is **Android-only** — no iOS, no web client is being built right now.

- [`backend/`](backend/README.md) — Node/Fastify + Postgres API.
- [`android/`](android/README.md) — Kotlin/Compose client.

## The two interaction models

TEW has two competing designs for how a user consumes the feed, and this
restart is deliberately building both as first-class citizens sharing one
core (see `android/README.md`) instead of picking a winner up front:

- **Radio timeline** — memos play sequentially, "like a radio station."
- **Card-deck** — a swipeable, single-card-at-a-time gesture interface.

Neither is the default yet. The plan is a moderated, on-device usability
test with real BLV testers, scored against the same task-completion bar,
before either becomes primary. See `android/README.md` for the gesture/
accessibility mechanics this depends on.

## The Ten Non-Negotiables

These are acceptance criteria, not aspirations. A feature that violates one
does not ship, regardless of how much engineering effort went into it.

1. Audio is the medium, not a fallback.
2. Voice-first, never voice-only.
3. The stream ends.
4. No engagement machinery.
5. Every action has three routes (media controls, voice, screen-reader menu).
6. No timed or precise gestures.
7. Voice is biometric data.
8. Moderation is visible and appealable.
9. Reachable without a smartphone.
10. Blind people govern it.

Non-negotiable #6 is in active, deliberate tension with the card-deck
model — see `android/README.md`'s de-risk note. This is a known, tracked
dissent, not an oversight, and it stays unresolved until the on-device
screen-reader pass actually happens.

## Who's who

Said Mohaddes Sadeqi is the project's real founder and owner — visually
impaired, without a software engineering background. Taha Mahmoodi is his
engineering collaborator, building this on Said's behalf.

## Start here

New to this repo, or resuming after a break? Read in this order:

1. This file.
2. `STATE.md` — what's actually done vs. in progress right now.
3. `IMPLEMENTATION.md` — the build brief and hard rules.
4. `HANDLING_PROTOCOLS.md` — how to work: files, context, interruption/
   resume, multi-agent, testing.
5. `GITHUB_WORKFLOW.md` — branches, PRs, releases.
