# STATE

Last updated: 2026-08-17

## Where things actually are

Backend: **API implemented** (Node/Fastify + TypeScript). Auth, feed with
keyset cursors, like/skip, comments, report, own-memos, appeal — matching
`android/core/API_CONTRACT.md` — plus multipart audio upload, authenticated
audio serving, and deletion honouring the retention rule. `PostgresStore` is
wired and selected by `DATABASE_URL` (`src/server.ts:20`); the in-memory store
is the fallback and what the tests run against. 34 tests without a database,
46 with one. Not implemented: ASR/transcripts, rate limiting, the Firebase
verifier.

**The app talks to the backend.** Set a server address in the app (Moderator
controls → Server) and it uses the real API, real playback over HTTP and real
posting; leave it empty and it runs on built-in sample memos so it is still
testable with nothing reachable. Verified end to end against Postgres with
seeded audio.

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
| `:app` | Hand-rolled DI container, `when`-based navigation, moderator feed toggle, **server address screen** |

Toolchain unchanged: AGP 9.3.0, Gradle 9.6.1, Kotlin 2.4.10, Compose BOM
2026.06.01, minSdk 26 / compileSdk 36 / JDK 17.

- Governance docs: `BRIEF.md`, `IMPLEMENTATION.md`, `HANDLING_PROTOCOLS.md`,
  `GITHUB_WORKFLOW.md`, `CLAUDE.md`, this file.
- Repo is **public** (deliberate — branch protection on the free plan).
- `dev` (default) and `prod` are protected: no direct/force push, required
  CI, required `CODEOWNERS` review (`@sadeqisaidmohaddes-star`). `prod`
  enforces even for admins.
- Taha-Mahmoodi has collaborator (push) access to this repo.
- CI runs `backend` and `android` jobs on PRs into `dev`/`prod`, plus on
  merges into `dev` and manual dispatch, and publishes the debug APK as a
  **`tew-debug-apk`** artifact (30-day retention) so a tester needs no
  toolchain. The `backend` job now runs for real — `backend/package.json`
  exists, so lint, typecheck, test and build all execute.

## The device session — 2026-08-16

Run by Taha on a real phone, ~7.5 minutes, screen-recorded (video only, no
audio track — the sound findings below are the tester's report, not something
the recording can be re-checked against). This is the first time any of this
has been seen running. It clears `HANDLING_PROTOCOLS.md`'s "seen running
once, for real" bar for every screen listed here.

**Reached and usable:** radio timeline, card deck, onboarding (all five
steps), report reasons, record-a-reply, sign-in, the server address screen,
and the moderator feed toggle.

**Works:**

- **TalkBack reads everything.** Every screen, with focus order and labels
  intact.
- **The screen-reader route works.** The custom actions menu opens and
  offers "Report this memo" — that is the third leg of non-negotiable #5,
  confirmed on hardware rather than inferred from a unit test.
- **The in-app narrator speaks.**
- **Memo audio plays.** "Playing. 4 seconds left." and "Paused." both
  observed on the radio screen.
- **The card deck's own gestures work.** The last stretch of the session ran
  with TalkBack switched off, driven entirely by the deck's swipes.
- **Recording captures.** "Recording. 13 seconds so far." — `MediaRecorder`
  and the microphone permission flow have now run for real.

**Broken — the one bug the session found:**

- **The three voices collide.** TalkBack, the in-app narrator and the memo
  audio all speak at once, over each other. Every one of them works; nothing
  arbitrates between them. Diagnosis below.

**Not measured:** latency against the under-100ms rule. The session was a
functional pass, not a timed one.

## The clashing voices — root cause

Worth writing down, because the code contains a comment asserting the
opposite and that comment is why this reached a device.

There are three sound sources and no single owner of "who is speaking":

| Source | Audio usage | What it does about focus |
| --- | --- | --- |
| Memo audio, `Media3PlaybackSession.kt:71` | `USAGE_MEDIA` | Requests focus, `handleAudioFocus = true` |
| In-app narrator, `Narrator.kt:68` | TTS default — `USAGE_MEDIA` | **Requests none.** Mixes straight over the memo |
| TalkBack | `USAGE_ASSISTANCE_ACCESSIBILITY` | Ducks other apps only if the user has turned on TalkBack's own "audio ducking" setting, which is off by default |

So both collisions are explained:

- **Screen reader off:** the narrator speaks while a memo plays. Two voices,
  full volume, guaranteed. `handleAudioFocus = true` does not help — it
  makes *our* player react when *something else* takes focus, and the
  narrator never takes any.
- **Screen reader on:** `Narrator.shouldNarrate` (`Narrator.kt:49`) correctly
  goes quiet, so the narrator is not the problem here — the memo is. It keeps
  playing at full volume underneath TalkBack's speech, because TalkBack does
  not take audio focus by default and there is no public API for "TalkBack is
  speaking right now".

The comment at `Media3PlaybackSession.kt:27-30` claims focus handling means
"announcements duck the memo instead of colliding with it". That is
backwards, and it is the kind of claim that only fails on a device.

## Known gaps in the MVP — deliberate, not forgotten

- **Route coverage is complete (3 of 3); the screen-reader leg is now
  confirmed on hardware.** Media controls (headset/lock-screen via
  `MediaSession`), voice (press-to-talk), and the screen-reader actions
  menu. All three funnel through one `FeedCommand` enum so none can drift.
  The actions menu was exercised in the 2026-08-16 session. **Headset
  buttons and speech recognition still have not been.**
- **Voice is press-to-talk, never always-listening.** A product decision, not
  a limitation — non-negotiable #7 makes voice biometric data, and an open
  mic in this app would capture a private space rather than a command.
- **Recording captures; the upload leg is still unproven.** `MediaRecorder`
  and the microphone permission flow ran in the 2026-08-16 session. That
  session used the built-in sample memos, so posting the recording to a
  server has still only been exercised in tests.
- **The card deck does not depend on the spike's answer.** Custom
  accessibility actions are the primary route; swipes are an enhancement for
  non-screen-reader users. If the spike fails, nothing needs rewriting. If it
  passes, swipes become a third route — an addition, not a redesign.
- **The formal TalkBack spike still has not been run.** `SPIKE.md`'s three
  passes were not part of the 2026-08-16 session. What that session did show
  is that the deck's swipes were used with TalkBack switched off, and the
  actions menu was used with it on — which is the app working as designed,
  not an answer to the spike's question.

## Deliberately deferred, not forgotten

- **Firebase project / Google Sign-In** — not needed for the internal
  prototype test, per Said. `StubAuthSession` satisfies the `AuthSession`
  interface in the meantime. Swapping is two lines in `app/TewContainer.kt`
  and no feature-module change. Must not reach a public build.
- **Backend Postgres store** — schema and migrations written, store interface
  settled, implementation pending. The four open questions in
  `core/API_CONTRACT.md` are now answered in `backend/README.md`.
- **Account deletion has no screen.** Deleting a *memo* now works from the
  profile — a Delete button per memo, behind a confirmation, calling the
  backend route that destroys the recording. Deleting your whole account does
  not: `DELETE /v1/me` exists on the backend and nothing in the app calls it.
  So "you can delete your audio" is now true one memo at a time, and not yet
  true all at once. `android/README.md` never scoped account deletion into
  this build, so this is a decision to make rather than an oversight to fix.
- **Media3 reads the auth token once**, when the player is first created.
  Fine for the stub session, whose token never changes. When Firebase lands
  and tokens expire, this needs a DataSource that re-reads per request or
  memos will start failing mid-session.
- **Postgres hosting** — self-hosted on Said's VPS, managed by another
  session. No provisioning done, no blocker.
- **Release-signing keystore** — undecided, blocks the first real GitHub
  Release.
- **Moderator review queue** — reports submit from the app; reviewing happens
  outside it. Deliberate scope cut in `android/README.md`.
- **DMs** — libsignal stays backend-only this phase.

## Decisions taken, so they are not relitigated

- **Feed ordering: newest to oldest.** Strict reverse-chronological over
  unheard memos. The per-author quota an earlier draft had is removed and
  there is a test guarding against its return.
- **Voice retention: until the person deletes it.** Memo deletion or account
  deletion destroys the audio. No expiry, no soft delete, no `deleted_at`.

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

## The agreed plan, in order

1. ~~**Cut `v0.1.0-test1`**~~ — done 2026-08-09. Note the tag points at
   `7924cc6`, which predates PR #31 (debug signing) and #32 (audio inside the
   APK), so the published prerelease is **not** what was tested. The
   2026-08-16 session ran a newer build off `dev`.
2. ~~**Run the device session.**~~ — done 2026-08-16.
3. ~~**Record what was found**~~ — done, above.
4. ~~**Fix the clashing voices.**~~ — merged 2026-08-16, PR #34. Not yet heard
   on a phone, which is what actually settles it.
5. ~~**Promote `dev` → `prod`.**~~ — merged 2026-08-16, PR #35. The first
   promotion this repo has had; `prod` held nothing but governance docs until
   now. Per-memo delete (PR #36) landed just before it, so `prod` and `dev`
   are identical.
6. **Tag `v0.1.0`** so there is a downloadable APK to test.
   ```
   git fetch origin && git tag v0.1.0 origin/prod && git push origin v0.1.0
   ```
7. **The second device session.** See "Next step".

Promotion was meant to be last, and it was taken one step early on purpose.
`GITHUB_WORKFLOW.md` asks that `dev` be stable — *"meaning the thing you just
merged actually works, not just that it built"* — and the voice fix has only
ever been unit-tested. Taha's call, made so that a versioned build exists to
test against rather than testing an untagged branch. Worth remembering if
`v0.1.0` turns out to need a `v0.1.1` quickly: that is the expected shape of
this, not a surprise.

## Step 4, one voice at a time — the fix, on PR #34

The rule the fix implements: **audio starts only after whatever is speaking
has stopped.**

1. **The narrator says when it has finished, and that is what starts the
   memo.** `Narrator.say` takes a callback fired from
   `UtteranceProgressListener.onDone`. An utterance interrupted by a newer one
   has its callback *dropped* rather than fired, so a card the listener has
   already left never starts playing behind them.
2. **Under a screen reader nothing starts on its own.** Considered and
   rejected: starting the memo quietly and raising it after a beat. There is
   no API for "TalkBack has finished speaking", so that is a guess at a
   duration, and it is wrong for anyone running speech at a rate other than
   the one guessed for — which is most people who rely on it. The feed
   announces the memo and waits to be asked, and the announcement says so.
3. **Nothing plays underneath onboarding.** The feed still loads during it;
   only the sound waits.
4. **The transcript is no longer spoken.** It duplicated the recording word
   for word. It stays on the card as text.

**What this costs, so it is not discovered later:** under a screen reader the
radio timeline stops being hands-off — auto-advance announces the next memo
and waits. That is the feed model's whole premise, given up under TalkBack. A
test is named for it so it is not quietly undone. It also means that for
screen-reader testers the radio and card-deck models are now closer together
than they were, which the usability comparison has to account for.

Not on hardware yet. The fix is about *when* sound starts, and the recording
from the last session has no audio track, so the next device pass is what
confirms it.

## The visual design — new, 2026-08-17

The app had **no theme at all** until today. `TewApp` wrapped everything in
`MaterialTheme {}` with an empty argument list, so every colour and every type
size in all six modules resolved to Compose's baseline. The violet in the
device recording was an absence, not a brand — and Material's baseline was on
the design run's own ban list as the category default, so the app was wearing
the one thing the redesign was forbidden to produce.

`runs/tew-android/` holds the inter.face run that fixed it: the six-row brief,
the measured current state, `DIRECTION.md`, six coded comps, and `SKIPS.md`.

**The direction: the record, set as a large-print edition.** An official report
— one column, the speaker named, entries timed, corrections printed rather than
hidden — at the type size a low-vision reader actually needs. The two parents
fight, and that is the point: a record is dense and small, a large-print edition
has no structural apparatus, and forcing one through the other leaves no margin,
so the marginal apparatus moves into the reading line. A screen reader already
reads linearly, so the visual design and the spoken design become one object.

- **Landed in PR #38:** cream `#FDF3EF` on ink `#191210` at 16.91:1, one rubric
  red `#A12721` used only where a record has been corrected, withheld or closed.
  Dark is a *reversed edition* rather than an inverted page — every weight drops
  50 units, because white on near-black bleeds. Atkinson Hyperlegible Next,
  bundled, 112 KB, SIL OFL 1.1, chosen because it disambiguates I/l/1 and b/d by
  drawing them differently rather than by weight. 20 sp body floor against
  RNIB's 16–18 pt large-print standard.
- **Landed in PR #39:** onboarding rebuilt as five leaves of a preface — the
  step counter moved into a running head, one idea per leaf at 34 sp, actions as
  lines with weight and a hairline instead of filled buttons, and `Next leaf` /
  `Previous leaf` as custom accessibility actions. That last one closed a
  promise the product had been making and not keeping: leaf 3's own copy says
  every button is also in the screen reader's actions menu, and onboarding had
  none.

**Both gates of the design run were skipped** at Taha's instruction, so nobody
chose this concept — the agent that derived it did. The two rejected concepts
and the reasons they lost are in `DIRECTION.md`. `TRANSLATE.md` rows 3 and 5 are
marked ASSUMED. Row 3 is the row every concept pushes against; if it is wrong,
they all are.

**The named risk, worth re-reading before the next session:** a record is one
bad execution from the assistive-tech-institutional lane the run explicitly
banned. The cheapest test is to show one person who has never seen the app the
radio screen and ask what it is.

**What is themed but not yet redesigned.** PR #38 gave every screen the palette,
scale and family; PR #39 rebuilt one screen to the design. Radio, card deck,
record, profile and sign-in are re-skinned, not relaid-out — `DIRECTION.md` §7's
front matter, running head, entry rules and closing surface are specified and
unbuilt.

**Nothing has been rendered.** No image backend was available to the design run,
so every contrast ratio in `DIRECTION.md` is computed rather than observed. The
next device session is the first time any of it becomes pixels.

## Two bugs fixed on the way, both of the same family

Found by reading the code for the theme, both verified before fixing:

- **TalkBack interrupted itself once a second during every recording.**
  `RecordViewModel` rewrote its status on a one-second ticker while
  `RecordScreen` marked that same text an assertive live region, and assertive
  re-announces on every change. Status and elapsed count are now two nodes and
  only one is a live region. Same family as the clashing voices — something
  speaking over something else because nobody owned the question.
- **`ReportScreen` printed *"By this memo's author."*** to real users. Its
  `memo` parameter existed for that one line and navigation only ever carried an
  id, so the parameter and the fake `Memo` built to satisfy it are both gone.

## v0.1.0 is out — 2026-08-17

**https://github.com/sadeqisaidmohaddes-star/tew-v2/releases/tag/v0.1.0** —
`tew-v0.1.0.apk`, 16.4 MB, downloadable with no GitHub sign-in and not wrapped
in a zip, which is what matters when the installer is using a screen reader on a
phone.

Cut from `prod` after the second promotion (PR #41), so `prod` and `dev` are
level. Marked **prerelease**, which is the honest label: nothing in it has been
seen running.

Two things that blocked earlier sessions did **not** recur, worth recording so
nobody plans around them again: `gh pr merge` went through, and the tag push did
not 403. The workflow's own note about Claude Code being unable to cut releases
is out of date.

The APK is debug-signed on purpose — the release keystore is still undecided and
`assembleRelease` would produce something unsigned that will not install.
Signature verified with `apksigner`: `CN=TEW Debug, OU=Third Eye Worldwide`.

## Next step

**The second device session.** Everything is now waiting on a phone; nothing is
blocked on code. This is also the first time any of the visual design becomes
pixels — every contrast ratio in `DIRECTION.md` was computed and none of it has
ever been rendered, so anything that reads wrong is new information rather than
a regression.

In rough order of what matters:

1. **One voice at a time**, TalkBack on and off — the deck, the radio, and
   onboarding. Still the reason this build exists.
2. **Whether waiting to press play is tolerable** on the radio timeline under
   TalkBack, or whether it costs that feed model too much to keep in the
   comparison. A judgement only a listener can make, and the one most likely to
   send the design back a step.
3. **The recording screen**, which should now let TalkBack finish a sentence.
4. **The design**, at default text size and at 200%. Whether it reads as
   deliberate or as institutional — the named risk of the record concept.
5. **The delete confirmation** under a screen reader — whether "Delete" and
   "Keep it" are distinguishable heard rather than read.

Then latency against the under-100ms rule, which no session has measured, and
the gesture spike in `feature-carddeck/SPIKE.md`, which has never been run.

**Write what is found back into this file.** The first session's results only
survived because that happened, and `HANDLING_PROTOCOLS.md` is right that an
unwritten result does not outlive the session it was found in.

## After that, the road to something presentable to a team

Taha's ordering, 2026-08-17: **tier 1 is deliberately skipped for now** — this
is the test version, and real auth and hosting are not part of it.

**Still unbuilt, in the order that unblocks the most:**

- **Real sign-in.** `StubAuthSession` gives every tester the same identity —
  `stub-user`, "test-user". Two people on the same server are one account, so a
  multi-person demo is not possible today. This is the gate, not a polish item.
- **The backend actually hosted.** `DEPLOY.md` says it outright: it has never
  been run on the box.
- **A server address baked into the build**, so a tester does not have to type a
  URL into a text field with a screen reader running.
- **ASR transcripts.** `addMemo` returns `transcript` NULL, so every memo a real
  person posts has no text at all. Seeded memos have transcripts, which means
  this looks fine right up until somebody posts.
- **Rate limiting**, before any public URL exists.
- **Account deletion**, the half of the retention promise with no screen.
- **The remaining five surfaces relaid out** — radio, card deck, record, profile
  and sign-in are themed but not redesigned. `DIRECTION.md` §7 specifies them.

The prototype is feature-complete against `android/README.md`'s scope for this
build, minus the two deliberate cuts (no DMs, no in-app moderator queue). What it
has never had is contact with a real backend: every session so far has run on the
built-in sample memos.
