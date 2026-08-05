# Handling Protocols

Operational rules for how any agent (or human) works in this repo. These
aren't style preferences — several exist specifically because of this
project's real constraints: unreliable power, multiple people/agents
touching the same repo, and a mission-driven need to not waste compute on
busywork.

## File & folder handling

- Root-level docs (`BRIEF.md`, `STATE.md`, `IMPLEMENTATION.md`,
  `HANDLING_PROTOCOLS.md`, `GITHUB_WORKFLOW.md`) are ALL-CAPS — this is the
  "read me first" governance layer, kept visually distinct from regular
  source.
- New source respects the module boundaries in `IMPLEMENTATION.md`'s
  project structure. Don't create a new top-level folder without updating
  that structure first — an undocumented folder is invisible to the next
  agent that resumes.
- Naming: Kotlin — PascalCase for classes/Composables, camelCase for
  functions/vals. Backend (TS/JS) — camelCase for functions/vars,
  PascalCase for classes, kebab-case for filenames. No abbreviations that
  aren't already standard (`vm`, `repo` are fine; inventing new ones isn't).
- A file that outgrows a single clear responsibility gets split, not
  extended — same rule for docs as for code.

## Context handling (session/context loss)

Assume any session can end without warning — see the interruption section
below. The rule that follows from that: **a decision doesn't exist until
it's written down somewhere durable.** Chat context, an agent's reasoning,
a verbal "let's do X" — none of that survives a lost session. A design doc,
`STATE.md`, a code comment, or a commit message does.

Concretely:
- Before ending a work session (or every ~30 minutes of active work,
  whichever comes first), update `STATE.md` with what's done, what's
  in-progress, and what's next.
- Any new session, any new agent, starts by reading `STATE.md` — not by
  re-deriving project state from scratch by re-reading the whole codebase.
- If you're mid-decision when a session is about to end (context limit,
  need to pause), write the decision and its reasoning to `STATE.md` even
  if it's not fully implemented yet. A written half-decision is
  recoverable; an unwritten one is gone.

## Failure & interruption / resume protocol

Power outages here are frequent and unpredictable — treat "the process
dies with zero warning" as a normal event, not an edge case to handle
eventually.

- **Commit small, commit often.** Never let a logically-complete unit of
  work (a function, a passing test, a finished screen) sit uncommitted
  longer than necessary. The cost of an extra commit is nothing; the cost
  of losing real work to a power cut is real.
- **`STATE.md` is updated at every milestone, not just at sign-off.**
  There is often no clean "end of session" here — it just cuts off. Update
  it *as you go*, not as a final step you might not reach.
- **On resume, before writing any new code:**
  1. Read `STATE.md`.
  2. Run `git status` and `git log` — check for uncommitted or unpushed
     work from before the interruption.
  3. Check `TaskList` (if tasks were being tracked) for anything left
     `in_progress`.
  4. Reconcile all three against each other before continuing. If they
     disagree, trust the git history over `STATE.md` (a commit is a fact;
     `STATE.md` can be stale) — then fix `STATE.md` to match.
- Never leave uncommitted, unpushed work as the only copy of something
  that took real effort. If it's worth keeping, it's worth a commit.

## Multi-tasking protocols (multiple agents/phases/tasks)

- Use the harness's task tools (`TaskCreate`/`TaskList`/`TaskUpdate`) to
  track phase and task state — this is how concurrent agents avoid
  duplicating or colliding on the same work. Claim a task (set yourself as
  owner) before starting it.
- **What can run in parallel:** anything that only depends on an
  already-fixed contract. `feature-radio` and `feature-carddeck` can be
  built concurrently once `:core`'s interfaces are defined, because
  neither depends on the other. Backend and Android can proceed
  concurrently once the API contract in `backend/README.md` is fixed,
  because Android only needs the contract, not the backend's internals.
- **What must not run in parallel:** two agents editing the same module at
  the same time without coordination. If work genuinely needs to touch the
  same files concurrently, use a git worktree per agent (isolated working
  directory, same repo) rather than both editing the same checkout.
- When spawning a sub-agent for a piece of work, give it a self-contained
  brief — the module it owns, the contract it must satisfy, where to write
  `STATE.md` updates — not "go figure out what's needed," which forces it
  to re-explore context that's already written down.

## Sandbox test protocol

- Backend: run against a local/test Postgres instance with seed data —
  never against the production VPS database. A local Postgres instance
  (docker-compose or equivalent) is the default dev/test target.
- Android: run in an emulator or a designated test device profile — never
  treat "it built" as "it works." A build passing doesn't mean the app
  runs; see the visual test protocol below for what actually confirms
  that.
- Destructive or data-mutating flows (delete account, moderation actions,
  DM send) are tested against sandboxed data only, ever.

## Visual test protocol

This project has two different kinds of "visual" surface, and they use
different tools — don't conflate them:

- **Any web-facing surface** (e.g., a moderation console, if/when the
  backend grows one) — use the `/browse` skill for verification: navigate
  it, check console errors, take screenshots, confirm the actual rendered
  state. This matches the standing rule for all web browsing in this
  environment.
- **The Android app itself** is not a web page — "open the browser"
  doesn't apply to it. Verify it by running the actual build on an
  emulator or device and capturing real screenshots (`adb screencap`, or
  Compose UI test screenshot assertions), not by reading the code and
  assuming it renders as intended. A change to `feature-radio` or
  `feature-carddeck` isn't done until it's been seen running, once, for
  real.

## Token optimization protocol

The goal: outcomes that would need a huge context window, without actually
spending anywhere near that many tokens getting there.

- **Don't re-explore what's already written down.** Check `STATE.md` and
  the relevant `README.md` before grepping the codebase to rediscover
  something a doc already answers.
- **Delegate wide/exploratory search, don't inline it.** A broad "find
  every place X happens" search belongs in a sub-agent whose raw output
  doesn't pollute the main session's context — bring back only the answer,
  not the search trail.
- **Read narrow, not wide.** Use targeted greps and line-ranged reads
  instead of reading whole large files when only one section is relevant.
- **Keep the governance docs dense.** Tables and bullets over prose in
  `STATE.md` and the READMEs — they're meant to be re-read every session,
  so every unnecessary word is a recurring cost, not a one-time one.
- **Batch independent tool calls.** If lookups don't depend on each
  other's results, issue them together rather than one round-trip at a
  time.
- **Hand off self-contained work.** A well-scoped sub-agent with a tight
  brief produces a better result per token than doing the same exploration
  inline in the main session.
