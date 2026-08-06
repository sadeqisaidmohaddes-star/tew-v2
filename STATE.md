# STATE

Last updated: 2026-08-06

## Where things actually are

- Design stage only. No implementation code exists yet in `backend/` or
  `android/` beyond their `README.md` design docs.
- Backend design: locked (see `backend/README.md`).
- Android design: locked (see `android/README.md`) — module structure
  (including `:feature-account`), tech stack, v1 scope (no DMs, report+
  appeal in-app, moderator queue deferred), and the TalkBack/gesture spike
  are all decided.
- Governance docs written: `BRIEF.md`, `IMPLEMENTATION.md`,
  `HANDLING_PROTOCOLS.md`, `GITHUB_WORKFLOW.md`, `CLAUDE.md`, this file.
- Repo is **public** (deliberate — required for branch protection on the
  free GitHub plan; can revisit later).
- `dev` (default) and `prod` branches exist and are both protected: no
  direct/force push, no deletion, required passing CI, required approving
  review from `CODEOWNERS` (`@sadeqisaidmohaddes-star`) — `prod` enforces
  this even for the repo admin. Because a PR author can't approve their
  own PR, the only working flow is **Taha opens the PR, Said reviews and
  approves it**. Proven working end to end (PR #1).
- CI (`.github/workflows/ci.yml`) runs a `backend` and an `android` job on
  every PR into `dev`/`prod`. Both gracefully no-op until real code exists
  (`backend/package.json`, `android/gradlew`) — so it's live now and will
  start actually testing the moment implementation lands, no further
  changes needed to the workflow itself.
- Taha-Mahmoodi has collaborator (push) access to this repo.

## Deliberately deferred, not forgotten

- **Firebase project / Google Sign-In** — holding off until the prototype
  is ready, per Taha. No account chosen yet.
- **Postgres hosting** — `lucifers-vps` (in local SSH config) is Taha's
  personal VPS, **not** for this project. Said's projects have a separate
  VPS; connection details not yet gathered. Get these when backend
  implementation actually starts, not before.

## Not started yet

- The TalkBack/gesture-passthrough spike (`android/README.md`) — the
  recommended first real implementation step, before any feature code.
- `:core` module.
- Backend implementation (currently design-doc only).
- Release-signing keystore for `assembleRelease`/`bundleRelease` — not yet
  decided, will block the first real GitHub Release.

## Next step

Start with the gesture spike (`IMPLEMENTATION.md` → Build order, step 1).
When picking up backend work, first get Said's VPS connection details
(above) before provisioning anything.
