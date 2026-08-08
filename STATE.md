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
- **TalkBack/gesture-passthrough spike: written, builds, NOT yet run on a
  device.** Lives in `:feature-carddeck` under `spike/`, protocol in
  `feature-carddeck/SPIKE.md`. Tests the mechanism `android/README.md`
  proposes (gesture surface with semantics cleared + raw touch dispatch),
  records every touch/hover reaching the app, and times earcon and speech
  latency separately. 15 JVM unit tests cover the verdict and latency
  logic — i.e. how the result is read, **not** the result.
  `./gradlew build test lint` verified locally against a real Android SDK
  (platform 36 / build-tools 36, installed into the sandbox): green, lint
  clean, debug APK produced, spike activity present in the merged
  manifest. The empirical answer needs a phone with TalkBack on; see
  "Blocked on" below.
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
  (`./gradlew build test lint`) since `android/gradlew` exists. CI also
  runs on merges into `dev` and on manual dispatch, and publishes the
  **debug APK as a `tew-debug-apk` artifact** (30-day retention) so the
  spike can be installed on a phone without a development machine —
  `feature-carddeck/SPIKE.md` has the steps and the two frictions
  (GitHub sign-in required, artifact arrives as a `.zip`).
- Taha-Mahmoodi has collaborator (push) access to this repo.

## Deliberately deferred, not forgotten

- **Firebase project / Google Sign-In** — holding off until the prototype
  is ready, per Taha. No account chosen yet.
- **Postgres hosting** — self-hosted on Said's VPS (Ubuntu + aaPanel,
  already running other sites/apps). That VPS is managed by another
  session with full access details — not `lucifers-vps` (Taha's personal
  box, unrelated to this project). No provisioning done yet, but no
  blocker on getting there.

## Blocked on

- **The spike's actual answer.** The code is merged into `dev` (PR #7,
  approved by Said with three conditions: no `feature-carddeck` code
  built on the raw-touch assumption until the run happens, the run is the
  next thing rather than something that drifts, and the `Blocked on` and
  `SPIKE.md` notes stay until a verdict is recorded). Running it is not
  something the build environment can do. Claude Code's sandbox has no
  hardware virtualisation (`/dev/kvm` absent, no `vmx`/`svm` in
  `/proc/cpuinfo`), so no Android emulator — and TalkBack ships with
  Google Play services, so even a working emulator needs a Google APIs
  image. `HANDLING_PROTOCOLS.md` already forbids treating "it built" as
  "it works". **Someone with an Android phone needs to run the three
  passes in `feature-carddeck/SPIKE.md` and report the verdict.** Until
  that happens the card-deck direction is unvalidated, and the whole
  question of whether `:feature-carddeck` is buildable as designed
  stays open.
- Same constraint blocks the under-100ms gesture-to-audio rule. Nothing
  measured yet. The in-app numbers, once collected, are lower bounds
  only — the audio output path past the API call is invisible from
  inside the process, so certifying the rule needs external measurement
  on the budget device `IMPLEMENTATION.md` names.

## Not started yet

- Real logic in `:core` (API client, auth session, Media3 playback,
  repository layer) — module exists, currently a stub. Deliberately not
  started in the same session as the spike: `IMPLEMENTATION.md`'s build
  order puts it second, and the spike's result may change what the
  feature modules need from it.
- Backend implementation (currently design-doc only).
- Release-signing keystore for `assembleRelease`/`bundleRelease` — not yet
  decided, will block the first real GitHub Release.

## Next step

**Run the spike on a real phone** — `feature-carddeck/SPIKE.md`, three
passes, ~15 minutes. That result decides whether the card deck is built on
raw touch or on semantic accessibility actions, and it gates step 2 of
`IMPLEMENTATION.md`'s build order.

`:core` was explicitly gated behind a working spike for this session, so it
was not started. Strictly by dependency it could run in parallel — `:core`
does not depend on the spike's outcome, only the feature modules do — but
that is Said's call to make, not an assumption to act on.
