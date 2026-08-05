# GitHub Workflow

## Branches

Two long-lived branches, the same model the earlier `third-eye-world-mono`
already proved out:

- **`dev`** — integration branch. All feature work merges here first.
- **`prod`** — production. Only reached by promoting already-verified work
  from `dev`. Never committed to directly.

## Day-to-day cycle

1. Branch off `dev` for the change (`feature/...`, `fix/...`).
2. Commit as you go (see `HANDLING_PROTOCOLS.md` — small, frequent
   commits, not one giant commit at the end).
3. Push, open a PR into `dev`. CI runs (lint/typecheck/test/build at
   minimum).
4. Review the PR on GitHub — use the `/review` skill for this rather than
   an ad hoc read-through.
5. **If review or CI finds a real problem:** open a GitHub issue
   describing it (don't just fix it silently — the issue is the paper
   trail for why the fix exists). Comment on the PR referencing the issue.
   Fix it, commit the fix to the same branch, push. Once resolved, close
   the issue as solved and note the fixing commit.
6. Once CI is green and review is clean, merge into `dev`.
7. Once `dev` is stable — meaning the thing you just merged actually
   works, not just that it built — open a promotion PR from `dev` into
   `prod`. Same review standard applies. Merge once verified.

Use the `/ship` skill to execute this cycle (branch detection, tests,
review, version bump, push, PR) rather than doing each step by hand. Use
`/land-and-deploy` for the `dev` → `prod` promotion once something's ready
to go live.

## Versioning & releases

- Semantic version tags (`v0.1.0`, `v0.2.0`, ...) on `prod` merges.
- On tag push, CI builds the release APK (`./gradlew assembleRelease` or
  `bundleRelease`) and attaches it to a GitHub Release for that tag (via
  `gh release create` or an equivalent Action).
- Every release must be a downloadable `.apk` directly from the GitHub
  Releases page — no separate distribution channel for this phase.
- Release notes: what changed, in plain language — this is also the
  changelog a non-technical reviewer (including Said) can read to know
  what's new without reading a diff.
