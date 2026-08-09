# STATE

Last updated: 2026-08-09

## Where things actually are

Android: **all six modules implemented.** `IMPLEMENTATION.md`'s build order
is complete, plus the two gaps that were open after it — route coverage and
recording. The app builds, installs, and runs end to end against in-memory
fakes.

| Module | State |
| --- | --- |
| `:core` | API client (Retrofit 3 + kotlinx-serialization), `AuthSession` interface + stub, Media3 playback session + `MediaSession`, voice command parsing, memo recorder, repository layer, in-memory fakes |
| `:feature-account` | Sign-in, profile, report, appeal |
| `:feature-radio` | Sequential timeline, auto-advance on completion, prefetch, explicit end-of-stream |
| `:feature-carddeck` | Card deck, onboarding, on-device narrator, **plus** the unrun TalkBack spike |
| `:feature-record` | Memo composer and reply composer. New module — `IMPLEMENTATION.md`'s structure updated to match |
| `:app` | Hand-rolled DI container, `when`-based navigation, moderator feed toggle |

Toolchain unchanged: AGP 9.3.0, Gradle 9.6.1, Kotlin 2.4.10, Compose BOM
2026.06.01, minSdk 26 / compileSdk 36 / JDK 17.

- Governance docs: `BRIEF.md`, `IMPLEMENTATION.md`, `HANDLING_PROTOCOLS.md`,
  `GITHUB_WORKFLOW.md`, `CLAUDE.md`, this file.
- Repo is **public** (deliberate — branch protection on the free plan).
- `dev` (default) and `prod` are protected: no direct/force push, required
  CI, required `CODEOWNERS` review (`@sadeqisaidmohaddes-star`). `prod`
  enforces even for admins.
- CI runs `backend` and `android` jobs on PRs into `dev`/`prod`, plus on
  merges into `dev` and manual dispatch, and publishes the debug APK as a
  **`tew-debug-apk`** artifact (30-day retention) so a tester needs no
  toolchain. `backend` still no-ops — no `backend/package.json`.

## Blocked on

- **The TalkBack spike has still not been run.** Merged in PR #7, ready to
  install, never executed with TalkBack on. No emulator is possible in the
  build sandbox (`/dev/kvm` absent, no `vmx`/`svm`), and TalkBack ships with
  Google Play services. **Someone with an Android phone needs to run the
  three passes in `feature-carddeck/SPIKE.md`.** Debug builds now show a
  "TEW gesture spike" launcher icon, so this is a tap, not an adb command.
- **Nothing has been seen running.** `HANDLING_PROTOCOLS.md` is explicit that
  a change to a feature module isn't done until it's been seen running once,
  for real. That has not happened for any screen in this build. Everything
  below is "compiles and is unit-tested", not "works".
- **No latency measured** against the under-100ms rule, for the same reason.

## Known gaps in the MVP — deliberate, not forgotten

- **Route coverage is complete (3 of 3), but untested on hardware.** Media
  controls (headset/lock-screen via `MediaSession`), voice (press-to-talk),
  and the screen-reader actions menu. All three funnel through one
  `FeedCommand` enum so none can drift. Headset buttons and speech
  recognition are the two things least verifiable without a device.
- **Voice is press-to-talk, never always-listening.** A product decision, not
  a limitation — non-negotiable #7 makes voice biometric data, and an open
  mic in this app would capture a private space rather than a command.
- **Recording exists but has never captured real audio.** `MediaRecorder`
  behaviour, microphone permission flow and file upload are all unexercised
  outside unit tests.
- **The card deck does not depend on the spike's answer.** Custom
  accessibility actions are the primary route; swipes are an enhancement for
  non-screen-reader users. If the spike fails, nothing needs rewriting. If it
  passes, swipes become a third route — an addition, not a redesign.

## Deliberately deferred, not forgotten

- **Firebase project / Google Sign-In** — no account chosen. `StubAuthSession`
  satisfies the `AuthSession` interface in the meantime. Swapping is two lines
  in `app/TewContainer.kt` and no feature-module change.
- **Backend** — design-stage. `core/API_CONTRACT.md` is the Android client's
  **proposal**, not an agreed contract, with four open questions listed for
  whoever writes the backend. In-memory fakes stand in so the app runs.
- **Postgres hosting** — self-hosted on Said's VPS, managed by another
  session. No provisioning done, no blocker.
- **Release-signing keystore** — undecided, blocks the first real GitHub
  Release.
- **Moderator review queue** — reports submit from the app; reviewing happens
  outside it. Deliberate scope cut in `android/README.md`.
- **DMs** — libsignal stays backend-only this phase.

## Product rules enforced in code, not just documented

Worth knowing before changing anything:

- **No counts anywhere.** `Memo` has `likedByMe` and no totals — non-negotiable
  #4. A number on a screen is how that decision gets quietly reversed.
- **The stream ends.** A null cursor is end-of-feed, tested in `:core`,
  `:feature-radio` and `:feature-carddeck` — non-negotiable #3.
- **Failures speak.** `TewResult.Failure.spoken` is plain second-person
  language; no status codes reach the user.
- **The narrator goes silent under a screen reader.** TalkBack is already
  speech; a second voice the user can't silence is worse than useless.
- **No timed or precise gestures.** Direction-only swipes, no velocity
  threshold, no double-tap, no long-press — non-negotiable #6.

## Next step

**Run the spike, and run the app.** Both need the same thing: a phone, the
`tew-debug-apk` artifact from the latest CI run on `dev`, and someone to use
them. The spike answers the card-deck question; the app itself has never been
seen running by anyone.

The prototype is now feature-complete against `android/README.md`'s scope for
this build, minus the two deliberate cuts (no DMs, no in-app moderator queue).
What it has never had is contact with a real device or a real backend.
