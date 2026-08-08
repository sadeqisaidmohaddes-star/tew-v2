# STATE

Last updated: 2026-08-08

## Where things actually are

- Backend: still design stage only. No implementation code exists yet
  beyond `backend/README.md`.
- Android: **scaffold implemented and merged** (PR #3). Real Gradle
  project now exists at `android/` with 5 modules (`:app`, `:core`,
  `:feature-radio`, `:feature-carddeck`, `:feature-account`) matching
  `android/README.md`'s design. AGP 9.3.0, Gradle 9.6.1, Kotlin 2.4.10
  (AGP's built-in Kotlin support — no separate `kotlin-android` plugin),
  Compose BOM 2026.06.01, minSdk 26 / compileSdk 36 / JDK 17. Each module
  currently has placeholder code only — no real feature logic yet.
  `./gradlew build --no-daemon` passes locally (all modules compile,
  lint clean, debug APK produced) and in CI.
- Governance docs written: `BRIEF.md`, `IMPLEMENTATION.md`,
  `HANDLING_PROTOCOLS.md`, `GITHUB_WORKFLOW.md`, `CLAUDE.md`, this file.
- Repo is **public** (deliberate — required for branch protection on the
  free GitHub plan; can revisit later).
- `dev` (default) and `prod` branches exist and are both protected: no
  direct/force push, no deletion, required passing CI, required approving
  review from `CODEOWNERS` (`@sadeqisaidmohaddes-star`) — `prod` enforces
  this even for the repo admin. Because a PR author can't approve their
  own PR, the only working flow is **Taha opens the PR, Said reviews and
  approves it**. Proven working end to end for docs (PR #1, #2) and now
  for real code (PR #3, including the first genuine `android` CI run —
  passed in 3m53s, not a no-op).
- CI (`.github/workflows/ci.yml`) runs a `backend` and an `android` job on
  every PR into `dev`/`prod`. `backend` still no-ops (no
  `backend/package.json` yet). `android` now runs for real
  (`./gradlew build test lint`) since `android/gradlew` exists.
- Taha-Mahmoodi has collaborator (push) access to this repo.

## Deliberately deferred, not forgotten

- **Firebase project / Google Sign-In** — holding off until the prototype
  is ready, per Taha. No account chosen yet.
- **Postgres hosting** — self-hosted on Said's VPS (Ubuntu + aaPanel,
  already running other sites/apps). That VPS is managed by another
  session with full access details — not `lucifers-vps` (Taha's personal
  box, unrelated to this project). No provisioning done yet, but no
  blocker on getting there.

## Not started yet

- The TalkBack/gesture-passthrough spike (`android/README.md`) — the
  recommended first real implementation step, now that the scaffold
  exists to build it in.
- Real logic in `:core` (API client, Firebase Auth session, Media3
  playback, repository layer) — module exists, currently a stub.
- Backend implementation (currently design-doc only).
- Release-signing keystore for `assembleRelease`/`bundleRelease` — not yet
  decided, will block the first real GitHub Release.

## Next step

Start with the gesture spike (`IMPLEMENTATION.md` → Build order, step 1),
built inside `:feature-carddeck` on a new branch, same PR flow as always.
