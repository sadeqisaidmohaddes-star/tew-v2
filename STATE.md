# STATE

Last updated: 2026-08-05

## Where things actually are

- Design stage only. No implementation code exists yet in `backend/` or
  `android/` beyond their `README.md` design docs.
- Backend design: locked (see `backend/README.md`).
- Android design: locked (see `android/README.md`) — module structure,
  tech stack, and the TalkBack/gesture spike are all decided.
- Governance docs written: `BRIEF.md`, `IMPLEMENTATION.md`,
  `HANDLING_PROTOCOLS.md`, `GITHUB_WORKFLOW.md`, this file.

## Not started yet

- The TalkBack/gesture-passthrough spike (`android/README.md`) — the
  recommended first real implementation step, before any feature code.
- `:core` module.
- Backend implementation (currently design-doc only).
- CI/CD (lint/test/build pipeline, release-on-tag APK build, `dev`/`prod`
  branches not yet created on this repo).

## Next step

Start with the gesture spike (`IMPLEMENTATION.md` → Build order, step 1).
