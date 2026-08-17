# DIRECTION — tew-android

The direction half. Loop 1 only. Loop 2 (`CRAFT.md`, technique assignment, the
broken-rules table) has not run and nothing below should be read as if it had.

Written by `direction-conductor`. Gate A was **not held** — see §1 and `SKIPS.md`.

---

## 0. The settled inputs

| | |
|---|---|
| **Branch** | **Reposition.** Taken by the session, not by this agent. There is no existing direction to correct: every colour and every type size on screen is a framework default. `TRANSLATE.md` row 5 bans the Material 3 baseline by name, so the conformance escape hatch does not apply |
| **Surface class** | **tool-shaped**, per `TRANSLATE.md` row 1. `§15` binds **[HARD]** — on Android that is D-pad traversal and switch access, which non-negotiable #5 already demands as the third route. Nine data states per screen, not three |
| **Platform mode** | **Android-native.** Material 3 as a substrate, never as an aesthetic. minSdk 26, budget hardware. 48×48 dp touch targets met even past visible bounds; type in `sp`; 8 dp between targets |
| **Comp mode** | **Coded comps.** Image mode requires an MCP image server this environment does not carry, so the mode was **unavailable, not unchosen**. All six workers confirmed this independently |
| **N** | **6 surfaces, 1 concept rendered = 6 coded comps.** Three concepts derived in full prose; one rendered |
| **`redesign-scout`** | Not dispatched. Recorded in `SKIPS.md` |

### The row-6 re-sample — run directly, not taken on trust

`§6` requires sampling the pixels rather than accepting the filled row's summary.
The source was re-opened. It confirms row 6 and adds four things row 6 did not
carry:

- **There is no launcher icon at all.** `AndroidManifest.xml` carries no
  `android:icon` and no `android:roundIcon`, and there is no `mipmap/` directory in
  any module. The app ships the stock Android robot.
- **`CURRENT.md` is wrong in a way that makes the build cheaper.** It says there is
  no `MaterialTheme` wrapper. There is one — `MaterialTheme {}` with an **empty
  argument list**, at `TewApp.kt:74`. Net effect identical (M3's baseline
  `lightColorScheme()`), but the theme has exactly one insertion point.
- **The only authored visual scale is spacing.** 39 `dp` usages, 8 distinct values,
  `padding(16.dp)` + `spacedBy(12.dp)` repeated near verbatim across six screens.
- **Zero `sp` anywhere.** Across 56 Kotlin files there is not one `fontSize`,
  `fontWeight`, `lineHeight` or `TextStyle`. `Color(` appears zero times. No
  `colors.xml`, no `res/font/`, no drawable, no SVG, no design source of any kind.

**So there is nothing to sample, and that is the finding.** The only owned binary
assets are 14 `.m4a` seed recordings. **The product's identity currently lives
entirely in sound and in copy.** `§6`'s no-brand fallback applies, discharged in §6a.

### The survival list this direction is built against

1. Every accessibility structure — live regions, merged semantic nodes, custom
   accessibility actions, heading semantics, focus order
2. The copy, verbatim
3. No counts, anywhere
4. Three routes to every action
5. No timed or precise gestures
6. The stream visibly ends

---

## 1. Deviations, recorded as decisions

`§16` permits skipping and forbids doing it quietly.

| Decision needed | What happens because it was deferred |
|---|---|
| **Gate A not held.** Taha: *"skip the gates too we are in a rush."* | The concept is chosen by the agent that generated it. The one comparison this loop exists to produce did not happen. A wrong direction is discovered at build time and costs a redo rather than a sentence |
| **The family-pass fresh judge** | Nobody matched label to concept before seeing which was which. The family pass in §4 is **entirely self-judged**, which its own text calls the weakest of the three tests |
| **Comps for one concept, not three** | Nothing to compare the rendered set against. The two rejected concepts exist as prose only |
| **N cut below the tool-shaped floor** | Empty, permission-denied and conflict get no comp of their own. They are specified in prose per surface in §7. Text is weaker than a comp, and this is where a build most often defaults to the framework |
| **`board.html` not built** | No side-by-side comparison and no human attending |
| **`TRANSLATE.md` rows 3 and 5 ASSUMED** | Row 3 is the row every concept pushes against. If it is wrong, all three concepts are wrong |
| **Gate B will not run** | Decision 19's semantics patterns ship unreviewed |

### What the derivation wanted from rows 3 and 5 and did not get

- **Row 3 gave a feel but not a memory.** *"Almost nothing on it, and every absence
  obviously deliberate"* is strong and usable. What it does not carry is a thing
  Taha would point at — an object, a room, something he has seen. `STYLES.md`'s
  derivation runs off exactly that, so step 1 was answered from the archetype's own
  sentences instead. Three plausible objects were derived where one named object
  would have decided it in a sentence.
- **Row 3 does not say which viewer wins on the screen an evaluator screenshots.**
  The moderator banner is where that tension lands and the row is silent on it.
- **Row 5 bans three lanes and names no permission.** All three bans were honoured;
  none of them generated anything.
- **`inbox/` is empty**, so every "what the references taught and what was left
  behind" line `loops/01` §4 requires is absent for the whole run.

---

## 2. The `ACCESS.md` §13 decisions

Run before any comp. `§10` is **[HARD]**. ⑂ rows answered for the chosen concept,
**record**.

### Both classes

| # | Decision | Answer |
|---|---|---|
| 1 | Target size route | **Size, not the spacing exception.** 48×48 dp touch, met even past visible bounds. **Corrected by the comps:** a 20 sp action line at 1.45 is ≈29 dp and +10 dp above and below reaches 49 dp *only at `fontScale` 1.0*. At Android's **0.85** font scale it drops to **44.7 dp** and fails. So type size alone cannot be relied on to produce a target: **every action row carries `defaultMinSize(minHeight = 48.dp)`**, and `sp` is never summed with `dp` to claim a target. The "no button chrome" move survives — the words still carry no fill and no border — but the target is asserted in `dp`, not inferred from type |
| 2 | Contrast boundary | **Corrected from an earlier draft.** `SURFACES.md` publishes no Android number, but Android does: *"If the text is smaller than 18sp, or if the text is bold and smaller than 14sp, use foreground and background colors that result in a color contrast ratio of at least 4.5:1. For all other text, set the color contrast ratio to at least 3:1."* (developer.android.com). Material restates the web's 18 pt / 14 pt-bold figures and attributes them to W3C. **The boundary is 18 sp regular / 14 sp bold — and this design declines to use it.** Every text pair is ≥4.5:1 regardless of size; body is ≥7:1. RNIB's Product Design Guide is the reason: *"a contrast of 4.5:1 … is the minimum required to include people with typical visual acuity at age 80… for important information… at least 7:1."* An exception that lowers contrast as type grows is backwards for this audience |
| 3 | Can the accent carry body text, a 3:1 role, or neither | **A 3:1 graphical role only — never body text.** It measures 6.77:1 on paper and would legally carry text; it is still barred, because its one job is to be the single mark in a record, and a colour with two jobs has none |
| 4 ⑂ | Focus indicator, drawn | **Two channels.** (a) The focused line **reverses** — paper on ink, 16.91:1 light / 16.26:1 dark. (b) A 4 dp `mark` rule at x = 8–12 dp in the leading margin, **outside the reversed block**. Forced by measurement: `mark` on `ink` is **2.50:1** and fails both bars. Two channels means it survives monochrome and survives a user who cannot see red. Not decoration — D-pad traversal is non-negotiable #5's third route, and switch-access scanning uses the same indicator |
| 5 ⑂ | Sticky chrome geometry | **Reserved layout space, `heightIn(min = 56.dp)`.** Corrected from a fixed 56 dp by the 02-radio comp: a 15 sp head resolves to 40.5 dp of text at 200% and clips inside a fixed 56 dp once padding exists. It never shrinks, never floats, never collapses on scroll — it grows |
| 6 | Non-drag affordance for every author-built drag | The deck's `forgivingSwipes` (right/left/up, 48 dp threshold, direction-only, no velocity or time threshold) is the only gesture. **Every swipe already has both a word on screen and a custom accessibility action**, and both are preserved. Tabulated per action in the 03 comp |
| 7 | Authentication path (3.3.8) | **N/A with reason.** No password field, no OTP, no cognitive function test. The 06 comp notes 3.3.9 conforms on the same grounds. Reopens if a password is added |
| 8 | Help's fixed slot | **The last item of the contents list: `How this works`**, re-opening onboarding at step 1. The app has no help route today |
| 9 | Landmark map and heading outline | Separate from the type scale. Running head = `heading()`; each entry's speaker line = `heading()`; **05-profile's sticky date rules are also headings**, which turns TalkBack's headings-navigation into day-by-day movement through forty rows — the fastest route through the list, free. **Fixes a live defect:** `SignInScreen.kt`'s `Third Eye World` is the only headline in the app with no `heading()` semantics |
| 10 | Accessible name for every icon-only control | **N/A with reason** — the app has zero icons and this direction adds none. **One unresolved collision, raised by the 06 comp:** Google's Sign-In branding guidelines require the `G` mark on the button. Either an exception for the one asset Google mandates, or a documented deviation from their guidelines. Not this agent's decision |
| 11 ⑂ | Reduced-motion still frame, art-directed | The app has no motion today. This direction adds one: the entry rule drawing left-to-right, 180 ms. Reduced-motion is **the same frame with the rule already drawn** — a composed still, not `animation: none` |
| 12 | Script, direction, expansion budget | **English, LTR, `en` only.** No `stringResource` call exists anywhere, so the app is not localisable today — a finding, not a design decision. The action line **wraps to as many lines as it needs and never truncates or shrinks**. Vertical, not horizontal |
| 13 ⑂ | Focus on route change, and on removal | **Route change:** focus to the new screen's running head, which is a heading, so the destination is announced. **Removal:** deck — the *new* card's speaker line, never the screen top; profile — the next entry, the previous if it was last, the running head if empty |

### Additionally, tool-shaped

| # | Decision | Answer |
|---|---|---|
| 14 | grid or table | **Neither — there is no tabular surface.** `collectionInfo` makes TalkBack speak *"item 3 of 40"*. **Precedent already exists and cuts both ways:** `RadioViewModel.describePosition` already announces *"Memo 3 of 12."* and `AccountViewModel` announces *"You have posted 3 memos."* Position and inventory counts already ship; non-negotiable #4 is about **engagement machinery**. Decision: `collectionInfo` **off** on feed and deck (it would duplicate `describePosition`), **on** for 05-profile |
| 15 | Combobox popup role | **N/A.** None exists; none added |
| 16 | Modal initial focus, plus fallback | **Initial focus on the dialog's heading, not its first control**, so *"Delete this memo?"* is spoken before *"Delete"*. Fallback when the invoker is gone: the entry's speaker line; then the running head |
| 17 | Genuinely a `menu`/`menubar`? | **No.** The contents list is a navigation list |
| 18 | Live-region triage | Below |
| 19 | Semantics patterns as Gate B cost lines | Three: the **merged entry node**, the **live region**, the **dialog**. **Gate B will not run**, so these ship unreviewed |

#### Row 18 in full — live-region triage

| Class | What it carries | Politeness |
|---|---|---|
| **Silent** | Entry rules drawing. Focus movement. Transcript appearing. Playback position ticking | none |
| **Advisory** | *"Playing. 4 seconds left."* · *"Paused."* · *"Liked."* · *"Skipped."* · *"Loading the audio…"* | `polite` |
| **Imperative** | Recording and posting — the existing `RecordScreen` announcement — plus one addition: *"This memo was removed while you were here."* | `assertive` |

- **Nothing advisory announces while memo audio is playing.** It queues.
- **A live bug the 04 comp found, which is not a design question.**
  `RecordViewModel` rewrites `announcement` every tick and `RecordScreen` marks that
  same text `Assertive` — **as written, TalkBack interrupts itself once per second
  for the length of the memo.** The spec splits printed cadence (1 s) from announced
  cadence (transitions only). Strings unchanged.

### Additionally, native

| # | Decision | Answer |
|---|---|---|
| 20 | Layout at 200% text | One column throughout. No side-by-side anything. Text wraps, never truncates. **`sp` values are never summed** — every padding and fixed height is `dp`, every type size is `sp`. `4sp + 20sp ≠ 24sp` |
| 21 | TalkBack grouping | **Every entry is one merged semantic node.** One refinement from the 03 comp: **the transcript is a separate focus stop, not merged into the speaker node** — merging it would make "deliberately not spoken" mean "unreachable," contradicting the shipped `cardAnnouncement` KDoc |
| 22 | Reduce Transparency / Increase Contrast | **Reduce Transparency is a no-op by construction** — no scrim, no blur, no translucency, no elevation shadow carrying meaning. High-contrast text adds nothing and breaks nothing |
| 23 | Pointer alternative for every gesture; no timer-only dismissal | Every swipe has a word. **There is no toast anywhere.** Every transient message is a printed line that stays until the next action displaces it |

---

## 3. Derivation — three directions

`STYLES.md`, **"derive, don't pick"** first, then **"Picking one"**.

**Row 4: patient, plainspoken, finite.** Shadow: **worthy**.
**Row 3: almost nothing on it, and every absence obviously deliberate.**

Physical experiences written before anything was drawn: a letter board outside a
village hall; a printed order of service; a talking-book cassette in its library
mailing case; an answering machine on a hall table; a hymn board; Hansard; a night
bus timetable; a large-print library book; a tide table; a card index drawer.

### The reflex generated and thrown out first

**A full-bleed dark screen with one large circular play control and the transcript
beneath it.** Generated, looked at, discarded under `STYLES.md`'s cliché fence: it
is guessable from "surface for a voice app" alone, and it is ban 3 with the waveform
deleted.

### Direction A — the letter board

> **A letter board outside a village hall: movable white plastic letters pushed into
> a black grooved felt panel, changed by hand once a week, holding nothing but what
> is on this week.**

Patient — it stays up all week. Plainspoken — no room for adjectives. Finite — there
is one board. Against the shadow: a letter board is **mundane**, the jumble sale
rather than a memorial. Structurally, one thing per screen; **nothing scrolls**, a
board gets changed; transitions are replacements.

### Direction B — the official report

> **Hansard: the official report of what was said today — one column, the speaker
> named, every entry timed, corrections and withdrawals printed in the record rather
> than hidden, and the day's report closes.**

Patient — a record is taken carefully and does not editorialise. Plainspoken —
verbatim is the point. Finite — each day's report closes. It reframes the product
from *feed* to **record of what people said today**, which is row 2's own sentence.
Against the shadow: a record is bureaucratic and slightly dry, and refuses to be
moved by its own subject.

### Direction C — the answering machine

> **A telephone answering machine on a hall table: a two-digit counter, four
> labelled keys that never move, and a tape that runs out.**

Patient — it waits all day. Plainspoken — the words are on the keys. Finite — the
tape ends. Structurally the inverse of A and B: **the control panel is the design
and the content is a status line.** The claim underneath: *the panel never moves*.

### The five picking inputs

1. **Category cluster.** Two, not one. Voice-social — Clubhouse, Spaces, Airchat,
   podcast players — is waveforms, avatars, round play buttons, near-black and
   violet. BLV assistive — Be My Eyes, Seeing AI, Aira — is clinical blue, rounded
   buttons, ear and eye pictograms. The app sits in the first by accident and is one
   palette from the second.
2. **The empty position.** Nobody in either cluster looks like a **printed record**.
   That lane is vacant. The appliance lane is vacant too, but adjacent to ban 1.
3. **Anti-positioning.** All three clear the bans. **C is closest to ban 1.**
4. **The viewer's risk appetite.** The BLV user's tolerance is near zero for
   anything costing a habit, and the habits are proved on hardware — which makes
   survival list #1 binding on all three. The evaluator: A is most photogenic and
   most likely to read as an art project; C reads deliberate but retro; B
   photographs as a serious document.
5. **What is already owned.** Nothing visual. But the copy is owned and it is
   **plainspoken second-person prose** — *"Short voice memos, from people who get
   it."*, *"There is no endless pile."*, *"The recording will be destroyed. It cannot
   be brought back, and anyone who has not heard it never will."*

---

## 4. The two distinctness tests, and the choice

### Swap test — tool-shaped form

| | Screen title | Primary action | Empty state |
|---|---|---|---|
| **A — noticeboard** | `TONIGHT` | `NEXT` | "The board is empty tonight." |
| **B — record** | "The record for Sunday 17 August" | "Play this entry" | "No record for today yet." |
| **C — handset** | "Message 1" | `PLAY` | "No messages." |

None of the nine moves unnoticed. **Pass.**

### Family pass

**noticeboard** · **record** · **handset**. **Pass on the artifact — and entirely
self-judged**, because Gate A was skipped and no human matched label to concept.

### Category-reflex check

None is guessable from *"a surface for a voice social network for blind people"*.
**Pass.**

### The choice: B, the record

Made by this agent in the human's absence, because the gate was skipped.

1. **A cannot hold density and B is made of it.** A holds one message per screen;
   forty of your own memos is forty screens. A would need a fork, and a forked
   direction is two directions.
2. **A gets worse for the low-vision half at 200%.** A transcript set enormous and
   centred is roughly five words per screen — worse than the paragraph they have
   today, which is the exact failure this run exists to fix.
3. **C leaves the content off the screen.** Row 3 is a screenshot row, and the only
   thing a sighted evaluator can see is the transcript.
4. **C's counter sits on a fault line.** Position counts already ship, so it is not
   banned — but putting the product's most-contested surface at the centre of a
   metaphor is asking for it to drift.
5. **B is the direction where the owned copy is the design.** In A the copy is
   content inside a design; in B the copy *is* the design.
6. **B is the only one that answers the moderator banner** rather than working
   around it.

---

## 5. The chosen concept — **record**

### The collision

> **An official report of the day's speaking, set as a large-print edition — the
> record's structure at a blind reader's type size, so the thing that makes it
> authoritative and the thing that makes it readable are the same decision.**

- **Structural parent: the official report.** One column, entry rules, speaker
  attribution, a running head, timed entries, a printed closing line, corrections
  and withdrawals printed rather than hidden.
- **Surface parent: the large-print edition**, at RNIB's published standard.

**What it produces that neither parent has.** A record is normally dense and small —
Hansard sets around 8 pt in two columns with the speaker in the margin. A
large-print edition is a novel with no structural apparatus. Force the record
through the large-print standard and **there is no margin left: the marginal
apparatus moves into the reading line.**

That is the invention: **a screen reader already reads linearly, so once the margin
collapses into the column, the visual design and the spoken design become the same
object.**

### The subversion — one rule broken, named

A record's law is that it is impersonal and complete. This breaks it exactly once:
**the record ends, and the ending is a designed surface rather than an empty state.**
Hansard closes with *"The House rose at 10.14 pm."* This closes with the product's
own owned copy, set as a closing line with a rule above and nothing below.

### Style under density — corrected by measurement

> **At forty rows this is a page of the record — and it holds by scrolling, not by
> density.**

**An earlier draft of this file claimed ~5 entries per screen. That was wrong and
the comps caught it by measuring.** A 24 sp speaker line, a three-line 20 sp
transcript, and two 49 dp action rows (five actions do not fit one line at 390 dp)
floor the radio entry at **326 dp**:

| Surface | Entry height | Per screen, default | At OS 200% | Forty rows |
|---|---|---|---|---|
| 02-radio | 326 dp | **2.3** (1.8 on the first screen, under the front matter) | ~0.9 | — |
| 05-profile | 229 dp Posted / 365 dp Taken-down | **~3** | ~1.5 | **13 screens** |

The finding survives the correction and is strengthened by it: this direction does
not buy density, it buys legibility, and it pays in scroll. That is the right trade
for this audience and it should be made with the real number in view.

**The measure conflict, stated rather than resolved.** At 20 sp on 390 dp with 24 dp
margins the column is 342 dp — roughly **30–38 characters**. Below `STYLES.md`'s
45–75 ch working range and below WCAG 2.2 SC 1.4.8's 80-character ceiling. **Large
print and a comfortable measure do not coexist on a 390 dp phone.** Mitigations:
short paragraphs, ragged-right, no hyphenation.

### Light and dark: two art directions

The surface parent is print, and a print substrate has no inverse. **Light** is a
cream page with black ink. **Dark** is the **reversed large-print edition** — a real
published object, not `filter: invert()`. White on near-black bleeds optically, so
on dark **every weight drops 50 units** (400 → 350, 600 → 550) and the rules
lighten. The accent's chroma is cut, not its hue.

---

## 6a. Palette — chosen, defended, measured

**`§6`'s fallback path.** No brand exists, so the choice is deliberate and defended
by one sentence of physical scene:

> **The printer's second ink — the one colour on a page that is otherwise black on
> cream, reached for only where the record has been corrected, withheld, or closed.**

That is a **rubric**: the red a scribe used for headings and corrections, from
*ruber*. It is the only colour in the system, it appears rarely, and **its rarity is
what makes it read as deliberate** — row 3 discharged in a palette rather than
asserted about one. It also lands where the product needs it: non-negotiable #8
makes moderation visible and appealable, and the second ink makes a withheld entry
visible without a badge, a pill, or an icon.

Bans cleared: not clinical blue or beige; not gradient violet; no waveform.

**Neutral ramp: hue 40 held across every step, chroma 0.008–0.014** — `STYLES.md`'s
tinted neutral. The accent sits at hue 28, four degrees away, which is why it reads
as *a second ink from the same press*.

All twelve values computed OKLCH → sRGB, **all in gamut, no clipping**.

### Light — "the page"

| Token | OKLCH | Hex | Pair | Ratio |
|---|---|---|---|---|
| `paper` | `oklch(0.970 0.012 40)` | `#FDF3EF` | `ink` on it | **16.91:1** |
| `paper-sunk` | `oklch(0.935 0.014 40)` | `#F2E7E3` | `ink` on it | **15.23:1** |
| | | | `ink-quiet` on it | **6.44:1** |
| | | | `mark` on it | **6.12:1** (graphical) |
| `rule` | `oklch(0.720 0.012 40)` | `#ACA29F` | — | 2.28:1 — **decorative only** |
| `ink-quiet` | `oklch(0.440 0.014 40)` | `#5A504D` | on `paper` | **7.14:1** |
| `ink` | `oklch(0.190 0.012 40)` | `#191210` | on `paper` | **16.91:1** |
| `mark` | `oklch(0.470 0.160 28)` | `#A12721` | on `paper` | **6.77:1** — graphical only |
| *focus* | `ink` ground | `#191210` | `paper` text | **16.91:1** |

### Dark — "the reversed edition"

| Token | OKLCH | Hex | Pair | Ratio |
|---|---|---|---|---|
| `board` | `oklch(0.160 0.010 40)` | `#110C0A` | `light` on it | **16.26:1** |
| `board-raised` | `oklch(0.210 0.012 40)` | `#1D1714` | `light` on it | **14.86:1** |
| `rule-d` | `oklch(0.420 0.010 40)` | `#524B49` | — | 2.29:1 — decorative only |
| `light-quiet` | `oklch(0.680 0.012 40)` | `#9F9693` | on `board` | **6.72:1** |
| `light` | `oklch(0.940 0.008 40)` | `#F0E9E7` | on `board` | **16.26:1** |
| `mark-d` | `oklch(0.620 0.130 28)` | `#C86459` | on `board` | **5.03:1** |
| *focus* | `light` ground | `#F0E9E7` | `board` text | **16.26:1** |

### Five constraints the measurement produced

- **`mark` on `ink` is 2.50:1 and fails both bars.** This forced the focus design:
  the mark rule sits **in the margin on paper**, outside the reversed block. One
  accent value for one role.
- **`paper` is at its chroma ceiling** — max in-gamut C at L 0.970 / H 40 is
  **0.0154**, and it sits at 0.012. The cream cannot get warmer without clipping.
- **`mark-d` on `board-raised` is 4.60:1**, barely over the body bar, so
  `board-raised` must never be lightened — **and `mark-d` is barred from the
  moderator notice block in dark mode** (03 comp).
- **Both hairlines land at ~2.28:1.** Correct for a decorative rule; **disqualifies
  them as a focus ring or as the sole boundary of any control** — which is why the
  04 comp's record button takes a 2 dp `ink` border rather than a `rule` one.
- **The record control cannot be a filled `ink` slab.** Reversal is this concept's
  focus device, so a permanently reversed button reads as permanently focused.
  Resolved as `paper-sunk` fill (15.23:1) with a 2 dp `ink` border.

**Status vocabulary** — never colour alone; a word **and** a shape, always:

| Status | Word (verbatim, shipped) | Shape | Colour |
|---|---|---|---|
| Visible | *"Posted. Anyone can hear this."* | no rule | — |
| Under review | *"Being checked. …"* | 4 dp margin rule, **dashed** | `mark` |
| Removed | *"Taken down. …"* | 4 dp margin rule, **solid** | `mark` |
| Appeal upheld | *"Your appeal succeeded and this memo is back."* | no rule | — |

The rule sits at x = 8, **outside the text column**, so a Posted entry and a
Taken-down entry set on exactly the same left edge. The status itself is 20 sp `ink`
at 16.91:1 — **text, never colour**.

No gradients exist anywhere in this direction, so `STYLES.md`'s interpolation-space
rule has nothing to bind to. Stated so the absence is not read as an omission.

---

## 6b. Type — one family, verified from the binary

**Atkinson Hyperlegible Next**, variable, roman only, `wght` 200–800, **SIL OFL
1.1**, self-hosted in `res/font/` per `§7`.

**Its reason, on the same line:** it disambiguates the exact character pairs
low-vision readers confuse — I/l/1, 0/O, b/d — by *drawing them differently* rather
than by weight, which survives Android's non-linear scaling curve and survives
central field loss. Roboto, the incumbent by absence, does none of that at any size.
Braille Institute: *"For low-vision readers, certain letters and numbers can be hard
to distinguish from one another. The Atkinson Hyperlegible® font uses special design
principles to differentiate characters and make each one unique."*

**One family, not two.** The second was budgeted for tabular figures and is not
needed: **`tnum` is present and real in Next**, verified by dumping the shipped
binary rather than trusting a specimen — default figures are proportional (advances
407–648/1000) and `tnum` substitutes a genuine `.tf` glyph set at a uniform
632/1000. A real substitution, not a no-op alias. Applied as
`TextStyle(fontFeatureSettings = "tnum")`.

**Bundled, not Downloadable Fonts.** `AtkinsonHyperlegibleNext[wght].ttf` is
**112 KB** and covers 200–800; two statics would be 132 KB and could not reach the
350/550 weights dark mode needs. Variable fonts in `res/font/` work from API 26,
exactly minSdk. **The italic VF (121 KB) is not shipped at all** — RNIB: *"Avoid
using italics."* A downloadable font on `IMPLEMENTATION.md`'s throttled-3G target
would paint in Roboto then reflow, and for a low-vision reader a reflow is a re-read.

**The scale is role-indexed, not ratio-derived** — a finished answer, not a gap.

| Role | Size | Weight light / dark | Line-height | Notes |
|---|---|---|---|---|
| Display | 34 sp | 600 / 550 | 1.25 | sign-in line, onboarding leaf, live duration |
| Speaker line | 24 sp | 600 / 550 | 1.30 | `heading()` |
| Body / transcript | 20 sp | 400 / 350 | 1.45 | the floor |
| Action line | 20 sp | 600 / 550 | 1.45 | weight, **never underline** |
| Time / secondary | 17 sp | 400 / 350 | 1.40 | `tnum` |
| Running head | 15 sp | 500 / 450 | 1.35 | `ink-quiet`, sentence case |

**The 20 sp body floor, justified.** RNIB: *"Large print is generally 16 to 18 point
size"*, and its Product Design Guide sets *"16-point Arial or equivalent or larger…
to be large print."* 20 sp clears it. Material's floor is 12 sp; the smallest value
here is 15 sp, carries no primary content, and at 7.14:1 clears the 4.5:1 bar.

**One refinement from the 04 comp:** Display applies to *the sentence carrying the
number*, not to the whole status string. `Recording. 13 seconds so far.` sets at
34 sp; the 90-second nudge and `Listen back, send it, or record it again.` set at
20 sp beneath. At 34 sp the nudge runs four lines and pushes the control off-screen.
Deliberate side effect: Idle has no number, so it sets at 20 sp — **the size jump
itself becomes the "it is listening" signal**, without a new string.

**Four rules from RNIB's Clear Print guidance, verbatim-sourced:**

- *"Align text to the left and avoid 'justifying' text."* → ragged-right always.
- *"Avoid using italics."* → no italic ships at all.
- *"Do not underline text."* → **corrects an earlier draft of this file**, which set
  the actions underlined. Actions carry weight (600) and a hairline *above* the
  action line.
- *"Large blocks of capitals should not be used"* — because *"the shape of the word
  is missing"*. → **no small caps and no all-caps anywhere**, including the running
  head, which a record would conventionally set in caps. The convention loses to the
  audience.

Leading clears RNIB's floor (*"at least 2pt above the size of the font"*): 20 sp at
1.45 is 29 sp. `paper`'s matt cream answers *"the substrate… should not be glossy or
reflective."*

**Not attributed to RNIB:** no-hyphenation. Their 2023 guidance does not mention it.
It is this run's craft decision, taken because hyphenation at a 30–38 character
measure breaks more words than it saves.

---

## 6c. Spacing

**8 dp base, ratio-named** — `space100 = 8dp`, `space200 = 16dp`, `space300 = 24dp`,
`space600 = 48dp`. Only values with a job get a token.

**`space150 = 12dp` is admitted as a nested unit** — the re-sample found `12.dp`
used 11 times, tied as the product's most-used value. Material's own scale admits
non-multiples where components use them. Pretending it away would have made the
migration a rewrite instead of a retheme.

**`96 dp` is a one-off, not a token.** `RecordScreen`'s `BIG_CONTROL_DP = 96` is the
one control in the app that genuinely earns a large target.

---

## 7. The six surfaces — **record**

**Derived screen flow:** `06-sign-in → 01-onboarding → 02-radio → 03-card-deck →
04-record → 05-profile`. **Rendered in the dispatched order**, the same sequence
rotated so onboarding leads.

### The seventh element — the moderator banner and the five-item nav

**It stops being chrome and becomes the record's own apparatus**, set as *front
matter* — running head, contents line, notice, stacked and reserved, consuming
185 dp at the top of 02-radio rather than floating over it.

- **The running head** — `heightIn(min = 56.dp)`, reserved not floating, never
  collapsing, carrying date and section at 15 sp `ink-quiet`, sentence case, with a
  hairline under it.
- **The contents line** — **corrected by the 01 comp: it appears only where a record
  exists.** It is absent on 01-onboarding (the preface is *what it opens*; a nav line
  there breaks one-idea-per-leaf) and absent on 06-sign-in (no record yet).
  Destinations: Today's record · Record a memo · Your record · How this works.
  *Server* and *Switch to the card deck* are **moderator-only**. A real user sees four
  destinations, a moderator six — today everyone sees five, two of which they cannot
  use.
- **The moderator notice** — the shipped string verbatim, in a `paper-sunk` block,
  **absent entirely on a live build**. It takes 72 dp and carries the surface's only
  `mark` rule, because it is literally an annotation saying the record is not real.
  **It is a traversal stop, not decoration** — it is information. Cost, logged: one
  extra stop on every screen, every session, for every user. That cost is the best
  argument for deleting it on a live build, and it only becomes visible once the
  notice is set as part of the record.

### The nine data states

| State | The record's answer |
|---|---|
| **Empty** | Shipped `EMPTY_FEED` verbatim — *"There are no memos waiting for you right now. Come back later, or record one yourself."* |
| **Loading** | Running head and rules draw immediately; each entry is a held-height skeleton with the speaker line's box reserved. **Layout never shifts.** *"Finding memos for you."* |
| **Partial** | What loaded is printed; a line names what did not, plus Try again |
| **Error** | `TewResult.Failure.spoken` verbatim, printed as a notice in the column, never a toast |
| **Permission denied** | *"TEW needs permission to use the microphone before you can record."* Designed on 04 only |
| **Offline / degraded** | Needs one new line; the existing offline string does not say what still plays |
| **Stale** | The running head carries the fetch time, with a Refresh line. `tnum` |
| **Conflict** | *"This memo was removed while you were here. Your appeal was not sent."* `assertive`. **Not handled anywhere today** |
| **Bulk** | **N/A with reason.** One memo at a time is the interaction model |

### 01-onboarding — five leaves of a preface

`top-left-lead` / `flat-surface`. Enters at top-left with the running head and the
34 sp idea, and lets the field fall empty to a pinned two-line action block at the
bottom — the held spacer (24 dp min, ≈161 dp at scale 1.0) **is** the composition,
and it is the first thing that collapses as type grows.

The shipped structure is preserved: one fixed headline across all five, a `Step N of
5` counter, one body string per step. What changes: the counter moves **into the
running head**, spoken as part of the section name; the body sets at 34 sp, one idea
per leaf; a hairline separates it from the actions. Since a preface is undated,
`3 of 5` takes the date's slot.

**Buttons are the shipped words: `Next` and `Back`, and `Start listening` on leaf 5.**
An earlier draft invented a `Skip`. There is no Skip in this product — deleted.

**No progress dots** — a count rendered as a graphic, invisible to this audience.

**`Next leaf` / `Previous leaf` are mandatory custom actions here**, because leaf 3's
own copy promises *"every one is also in your screen reader's actions menu."* The
leaf that makes the claim is the first thing that must honour it.

**Open copy question:** leaf 1's `Back` has no defined destination. Not a design
decision.

### 02-radio — the sequential feed

`dense-grid` / `flat-surface`. The densest surface in the set, and the one carrying
the full front matter. The record is already open at today's page — no hero, no
welcome, no bottom tab bar.

The entry: hairline → speaker line (24 sp/600, `heading()`, shipped
`From ${authorUsername}`) → time line (17 sp, `tnum`) → transcript (20 sp,
ragged-right, unhyphenated) → action line.

**Actions, shipped words and order:** `Pause`/`Play`, `Unlike`/`Like`, `Skip`, then
`Reply` and `Report`.

**Transport:** the shipped `playbackLine` as a `polite` region printed **in the
column** under the playing entry, not a floating bar.

**Closing line:** `RadioViewModel.END_OF_STREAM` verbatim — *"That is everything for
now. The stream has ended — there is no more to scroll. Come back later, or record a
memo of your own."* **An earlier draft flagged a copy gap here that does not exist:**
the radio has its own terminal string, distinct from the deck's, and the two must not
be collapsed.

### 03-card-deck — one memo at a time

`top-left-lead` / `flat-surface`. The column hangs from the top-left gutter and
**stops at y = 555 of an 844 dp frame, leaving 240 dp — 29% of the surface — as bare
`paper` below the last rule.** That emptiness is the only thing distinguishing this
from 02-radio: **the type table is row-for-row identical**, including the 20 sp body
and 24 sp speaker line. Neighbours differ by density and control size, never by
scale. Bands 24 / 56 / 716 / 48, summing to 844, bottom reserved-and-empty because
the nav moved into the contents line.

Actions: `Like`, `Skip`, `Play`/`Again`, then `Reply` and `Report`. Three routes
tabulated per action; the swipe documented as never-only. `collectionInfo` not set.
Terminal: `CardDeckViewModel.END_OF_STREAM` verbatim.

**Unresolved, named not fixed:** the moderator notice takes 72 dp and this surface is
only reachable from moderator controls today — so the comped screen and any live
screen differ by 72 dp. A product question about whether the deck ships to real
users, not a layout one.

### 04-record — the composer

`bottom-anchored` / `flat-surface`. The page is empty through its middle third and
heavy at the bottom — **337 dp of open field** between the 34 sp live duration and
the 96 dp control, so the one physical target in the app is findable by feel with
nothing else near it. That inverted weight is what no neighbour does.

**`BIG_CONTROL_DP = 96` is preserved**, and **scoped to the act performed without
looking — speaking.** It is not shown in the Finished state; its 96+24 dp goes to the
four action rows. `Allow microphone` reuses the identical slot, matching shipped code.

Action rows are **48 dp boxes with 8 dp gaps** (24 dp before `Cancel`), not 20 sp
lines with 12 dp gaps — 29 dp line boxes extended to 48 would have overlapped by
3.5 dp per pair.

`Cancel` is destructive-adjacent, distinct by weight, **never the default focus**.

### 05-profile — your own memos, moderation, appeal

`dense-grid` / `flat-surface`. **Where the concept earns itself.** The margin is the
surface's whole argument: a 4 dp status rule at x = 8, dashed for under-review, solid
for removed, outside the text column. Date-grouped with **sticky date rules that are
also headings**, so TalkBack's headings-navigation becomes day-by-day movement.

`collectionInfo` IS set here. `DeleteConfirmation` keeps its shipped copy —
*"The recording will be destroyed. It cannot be brought back, and anyone who has not
heard it never will."* — with initial focus on the title.

**Two new strings need sign-off:** the contents-line label `Contents: Your record`,
and the conflict string. Entry 2's transcript in the comp is a placeholder: the
product ships no under-review sample memo, and the worker correctly refused to invent
shipped copy.

### 06-sign-in — one line and one button

`top-left-lead` / `flat-surface`. Top-anchored directly under the reserved-and-empty
head band, left edge shared at 24 dp, with **~441 dp of bare `paper` — 62% of the
content band — falling beneath it, deliberately unfilled: the page the record has not
started on.**

**The running head is absent here and only here**, because there is no record yet to
head. The band is still reserved at its full 56 dp with an explicit builder note that
adding a running head deletes the idea. The worker declined RNIB's permission to
centre the wordmark, so the surface has exactly one left edge and one ragged right.

`Third Eye World` gains `heading()`. Copy is the owned four strings, unchanged.

### Two live bugs found while reading, which are not design questions

- **`ReportScreen` renders a placeholder to real users.** `TewApp.kt:138` passes
  `authorUsername = "this memo's author"`, so the screen reads *"By this memo's
  author. Choose what is wrong with it."*
- **`RecordViewModel`'s assertive tick.** Detailed under decision 18.

---

## 8. The composition log

Read off the sidecars the workers wrote, not out of their return messages.

| # | Surface | Composition anchor | Background mode |
|---|---|---|---|
| 1 | 01-onboarding | `top-left-lead` | `flat-surface` |
| 2 | 02-radio | `dense-grid` | `flat-surface` |
| 3 | 03-card-deck | `top-left-lead` | `flat-surface` |
| 4 | 04-record | `bottom-anchored` | `flat-surface` |
| 5 | 05-profile | `dense-grid` | `flat-surface` |
| 6 | 06-sign-in | `top-left-lead` | `flat-surface` |

### The set-level check — run, and suspended twice, out loud

**Not self-graded.** Six independent workers each picked and logged its own token
from the closed menus; none could see its neighbours. Each received only a one-line
brief naming what the others were doing, which is what `loops/01` §9 requires. The
conductor assigned no tokens, so the check keeps its full strength.

**Suspension 1 — the whole set is tool-shaped**, so `loops/01` §10 suspends all
three criteria, not only the background one. Each would fire on a compliant console:
six screens honestly log `flat-surface` throughout, and `dense-grid` is the *correct*
anchor for the two record columns. Stated out loud so a later reader does not read
uniformity as an oversight.

**Suspension 2 — the page-shaped subset.** §10 requires running all three across
genuinely page-shaped surfaces as their own subset. Here that is **01-onboarding and
06-sign-in**, a teaching sequence and a landing. Results: the anchor criterion cannot
fire at N=2 (it needs three in a row); the background criterion cannot fire (it needs
four). **The third criterion — no full-bleed treatment anywhere — does fire.** It is
suspended under §10's minimal-brief clause, and the brief qualifies twice over: row 3
is literally *"an app with almost nothing on it, and every absence obviously
deliberate"*, and the direction is typography-only, with no image, gradient or
texture anywhere in it. A full-bleed treatment would mean inventing an image for a
blind audience, which is ban 3's territory. **Sameness is the design here, and the
check would be arguing with the brief.**

**What the set does not vary — checked and confirmed.** Palette, type hierarchy,
component family and surface treatment are identical across all six. 03-card-deck
states its type table is row-for-row identical to 02-radio; all six transcribed the
same twelve hexes. That is the requirement, not a failure. Variation lives where it
belongs: 04 inverts the weight to the bottom, 03 leaves 29% bare below the last rule,
06 leaves 62% bare below the block, 01 holds a collapsing spacer, and 02 and 05 fill
their columns.

---

## 9. What this hands to the Compose implementation

The implementer makes no aesthetic decisions. Settled and not to be re-decided: the
role-indexed scale in `sp` with its 20 sp floor and RNIB justification; every colour
as a measured pair per mode; five spacing tokens on an 8 dp base plus the 12 dp
nested unit and the 96 dp one-off; **48 dp targets asserted via
`defaultMinSize(minHeight = 48.dp)`, never inferred from type size**; the nine states
per surface; the live-region triage; the traversal contract; the `collectionInfo`
split; and the four RNIB typographic rules — ragged-right, no italic, no underline,
no capitals.

The theme's single insertion point is **`TewApp.kt:74`**, currently `MaterialTheme {}`
with an empty argument list.

`tokens.json` is **not** the carrier for these numbers. DTCG `dimension` closes its
unit set to `px` and `rem`, so `dp` and `sp` are not expressible. Every native number
lives in this file's prose or in `$extensions` — a token file that silently rounds
20 sp to 20 px has produced a wrong layout with a valid schema.

### Open questions that are not this agent's to close

1. **The Google Sign-In `G` mark** versus decision 10's zero-icon rule.
2. **Leaf 1's `Back` destination** in onboarding.
3. **Two new strings** on 05-profile: `Contents: Your record`, and the conflict line.
4. **One new string** for the offline state: what still plays.
5. **Whether the card deck ships to real users**, which decides a 72 dp layout delta.
