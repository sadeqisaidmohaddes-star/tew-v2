# TEW v2 — Android

Fresh Android client for Third Eye World. Only Android is being built in this
phase — no iOS, no web. Talks to the backend in [`../backend`](../backend).

## Why this structure

Both `third-eye-world`'s two competing interaction models — the sequential
"radio" timeline and the swipeable card-deck — are being built as first-class
citizens this time, sharing one core, instead of one being the app and the
other a bolted-on branch. That's what makes it possible to run a fair,
moderated, on-device usability comparison between them with the same real
testers, which is the actual blocker (per the old project's own status
report) to deciding which one wins.

## Scope for this build

- **In:** sign-in, onboarding, the feed itself (radio-timeline or
  card-deck), recording/posting a memo, reporting a memo, seeing why your
  own memo was moderated and appealing it, a minimal profile (your own
  username + your own posted memos, sign out).
- **Out:** direct messages (E2EE/libsignal stays backend-only for now — no
  DM screen in this build), and the moderator's review queue. Reports get
  submitted from the app, but reviewing them happens outside it for
  now — a small internal tool or a direct query, not a dedicated
  in-app admin screen. That's a deliberate scope cut, not an oversight: at
  closed-beta scale, report volume doesn't yet justify building a second,
  admin-facing UI inside a consumer app. Reporting and appeal *do* ship —
  those are non-negotiable #8, not optional.

## Modules

- **`:core`** — API client (Retrofit, against the backend's cursor-paginated
  feed/auth/moderation endpoints), Firebase Auth session, Media3 playback
  session, repository layer for likes/skips/comments/reports. Neither UI
  shell talks to the network or the playback engine directly.
- **`:feature-radio`** — the sequential radio-timeline screen (Compose).
- **`:feature-carddeck`** — the swipeable card-deck screen (Compose): the
  gesture vocabulary, the onboarding flow that drills it, and the on-device
  narrator (Android's native `TextToSpeech` — no server-side TTS).
- **`:feature-account`** — sign-in, minimal profile, report + appeal
  screens. These don't belong to either feed model and shouldn't be
  duplicated in both or bias the comparison between them, so they get
  their own module rather than living in `:feature-radio` or
  `:feature-carddeck`.
- **`:app`** — thin shell wiring the above together, plus a moderator-only
  toggle to switch which feed screen loads, so a tester can try both
  without separate installs.

## Tech stack

Kotlin, Jetpack Compose, Media3 — ported from the original build's choices,
not reconsidered.

## The one thing to de-risk before building either screen out

The card-deck model depends on a gesture surface that TalkBack doesn't
intercept before the app sees it. Android has no single clean "direct touch"
trait the way iOS does — the real pattern is marking the gesture surface
not-important-for-accessibility and handling raw touch dispatch yourself,
which is fiddlier and less first-class. Spike this in isolation — one
throwaway screen, TalkBack on, prove gestures reach the app and TalkBack
still works everywhere else — before writing real feature code. If it
doesn't hold up, that's a finding that changes the design, better caught on
day one than after both screens are built.

## Status

Design stage. No implementation yet.
