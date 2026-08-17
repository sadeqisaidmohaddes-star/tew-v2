# record: Surface 2 of 6 — 02-radio

**Coded comp.** No image MCP server is present in this environment, so this is a spec
block, not a render.

- **Composition anchor:** `dense-grid`
- **Background mode:** `flat-surface`
- **Platform mode:** Android-native, minSdk 26, budget hardware
- **Frame:** one phone, **390 × 844 dp**, portrait, light mode ("the page"). All
  geometry below is dp at `fontScale = 1.0` unless a line says otherwise.
- **The rule that governs every number here:** *`sp` values are never summed.* Every
  padding, inset, band height and rule width is a `dp` constant. Every text height is
  `size(sp) × line-height`, resolved at the user's `fontScale`. The two are only ever
  added at layout time, which is why every touch target in this comp gets **larger**
  as the user turns type up, never smaller.

---

## 1. The layout move

The record is already open at today's page. There is no hero, no welcome, **no bottom
tab bar**. The apparatus that used to be chrome — the moderator banner and the
five-item text nav — is set as the record's own front matter: a running head, a
contents line, a printed notice. Then entry 1, immediately.

This is the densest surface in the set and it is meant to be. 01-onboarding is one
idea in an empty field; 03-card-deck is a single entry at *these same sizes*; this is
the page those two are excerpts from.

### Vertical arithmetic, top to bottom

| Element | dp | Note |
|---|---|---|
| Status band (reserved) | 24 | `paper`, no content painted into it |
| **Running head** | **56** | fixed, reserved, never floats, never collapses on scroll |
| Hairline under head | 1 | `rule`, decorative |
| Contents line | 49 | 10 + (20 sp × 1.45 ≈ 29) + 10 |
| `space200` | 16 | |
| **Moderator notice** block | 104 | 16 + (3 × 17 sp × 1.40 ≈ 72) + 16 |
| `space200` | 16 | |
| Entry 1 (playing) | 362 | 326 + 36 transport line |
| Entry 2 | 326 | |
| Entry 3 | — | rule + speaker line + time line visible at the fold |
| Bottom band (reserved) | 48 | gesture/home indicator, `paper`, holds no control |

### The entry, which is the repeating unit

| Part | dp | Type / colour |
|---|---|---|
| Hairline rule | 1 | `rule` `#ACA29F` — full measure, 24 → 366 |
| `space300` | 24 | |
| Speaker line — `From ruthm` | 31 | 24 sp / 600 / 1.30, `ink`, `heading()` |
| `space100` | 8 | |
| Time line — `7:42 pm · 24 seconds` | 24 | 17 sp / 400 / 1.40, `tnum`, `ink-quiet` |
| `space150` | 12 | |
| Transcript, 3 lines | 87 | 20 sp / 400 / 1.45, `ink`, ragged-right, unhyphenated |
| `space200` | 16 | |
| Hairline above the action line | 1 | `rule`. **A rule above text is not underlining it** |
| Action row 1 — `Pause` `Unlike` `Skip` | 49 | 10 + 29 + 10 |
| Action row 2 — `Reply` `Report` | 49 | 10 + 29 + 10 |
| `space300` | 24 | before the next entry's rule |
| **Entry total** | **326** | |
| *+ transport line, playing entry only* | *+36* | 12 + (17 sp × 1.40 ≈ 24) |

**The arithmetic is the design.** A 20 sp action word at 1.45 is ≈29 dp tall; +10 dp
above and +10 dp below is 49 dp — over Android's 48 dp floor **with no button chrome
at all**. Horizontally each word carries +12 dp either side, so the shortest word in
the set, `Skip` (≈42 dp of glyph), presents a 66 × 49 dp target. Targets overlap the
type's own leading and extend past the visible glyph bounds; nothing is drawn to show
it. Actions are words, and the words are big enough.

**No counts beside any action. Ever.** `collectionInfo` is off on this surface —
`RadioViewModel.describePosition` already speaks *"Memo 3 of 12."* and a second count
would be redundant, not richer.

### Horizontal

- Screen padding **24 dp**; text measure **342 dp**.
- At 20 sp Atkinson that is ~33 characters per line — inside the 30–38 character
  measure the no-hyphenation decision was taken for.
- Action words sit on one baseline run with `space300` (24 dp) between them, wrapping
  to a second row rather than shrinking. `Pause` `Unlike` `Skip` fills row 1 at 20 sp;
  by `fontScale 1.6` it has wrapped to three rows. The layout is a flow, not a grid.

### Density, stated honestly

- **fontScale 1.0:** content viewport 763 dp ÷ 326 dp = **2.3 entries per screen**;
  the first screen shows 1.8 because the front matter takes 185 dp.
- **OS 200% (fontScale 2.0):** the entry grows to ≈836 dp — transcript reflows 3 → 7
  lines, actions wrap to three rows — so **≈0.9 entries per screen**.
- At forty rows this is a page of the record.
- **Correction to the brief:** the "~5 entries per screen at default / ~2.5 at 200%"
  figure does not survive the arithmetic. A 24 sp speaker line plus a 20 sp
  three-line transcript plus two 49 dp action rows cannot be compressed below ~326 dp
  without breaking the type floor, which is the one thing this concept will not do.
  The real numbers are above.

---

## 2. The seventh element — the apparatus

### Running head — 56 dp, reserved

`Sunday 17 August · Today's record`

15 sp / 500 / 1.35, `ink-quiet` `#5A504D` (**7.14:1** on `paper`), sentence case,
left at 24 dp, vertically centred in the band, hairline `rule` beneath. It is a
`heading()`; route change moves focus here so the destination is spoken. It does not
collapse, does not float, does not become a toolbar, and holds no control.

**One deviation from "fixed":** the band is 56 dp up to `fontScale 1.6`; above that it
grows to fit its single line (`heightIn(min = 56.dp)`). At 200% a 15 sp head resolves
to 30 sp / 40.5 dp of text, and a clipped date is worse than a taller band. It still
never shrinks and never moves.

### Contents line — the first focusable element on every screen

`Contents · Today's record`

20 sp / 600 / 1.45, `ink`, 10 dp above and below = **49 dp**. Role button, spoken as
*"Contents. Currently: Today's record."* Opens **a full screen, not a sheet** —
a sheet puts a scrim over the record and a second traversal universe behind it.

Destinations, in order: **Today's record · Record a memo · Your record · How this
works.** *Server* and *Switch to the card deck* are moderator-only and live under the
notice, not in the contents.

### The moderator notice — the shipped string, verbatim

> Moderator controls. Currently showing: the radio timeline. Using built-in sample
> memos.

17 sp / 400 / 1.40, `ink` `#191210` on `paper-sunk` `#F2E7E3` (**15.23:1**), block
inset 24 → 366, 16 dp padding all round, no radius, no border. A **4 dp solid `mark`
rule `#A12721` in the leading margin at x = 8..12**, outside the block, running its
full height. That is the second ink doing its one job: the record has been annotated,
and the annotation says these entries are not real.

It is **absent entirely on a live build** — the block, the rule, and its traversal
stop all disappear together. Today it is welded on permanently, so it is designed as
front matter rather than hidden as a debug affordance.

**Traversal cost, named:** the notice is a non-actionable text node and it is one
extra stop before entry 1 on every screen, every session. That cost is the strongest
argument for its removal, and it is only visible once it is set as part of the record
instead of floating above it.

---

## 3. Type table

Atkinson Hyperlegible Next, variable, **roman only**, bundled in `res/font/`
(112 KB, `wght` 200–800). No italic file ships. Dark mode drops every weight by 50.

| Role | Size | Weight (light / dark) | Line-height | Where, on this surface |
|---|---|---|---|---|
| Display | 34 sp | 600 / 550 | 1.25 | *not used here* |
| Speaker line | 24 sp | 600 / 550 | 1.30 | `From ruthm`, `heading()` |
| Body / transcript | 20 sp | 400 / 350 | 1.45 | the transcript, the closing line — **the floor** |
| Action line | 20 sp | 600 / 550 | 1.45 | contents line, `Pause` `Unlike` `Skip` `Reply` `Report` |
| Time / secondary | 17 sp | 400 / 350 | 1.40 | time line, transport line, notice — `fontFeatureSettings = "tnum"` on the two numeric ones |
| Running head | 15 sp | 500 / 450 | 1.35 | `ink-quiet`, sentence case |

**Hard rules, held on every line of this comp:** ragged-right, never justified · no
italics anywhere · **no underline on text, ever** — the action line's hairline sits
*above* it · no all-caps and no small caps, including the running head, which a real
record would set in caps and this one does not, because the audience outranks the
convention · no hyphenation.

---

## 4. Colour pairs, measured

Read back against the palette table before returning; every ratio below is from it,
none computed here.

### Light — "the page"

| Pair | Hex on hex | Ratio | Used for |
|---|---|---|---|
| `ink` on `paper` | `#191210` on `#FDF3EF` | **16.91:1** | speaker line, transcript, action words, contents line |
| `ink-quiet` on `paper` | `#5A504D` on `#FDF3EF` | **7.14:1** | running head, time line, transport line |
| `ink` on `paper-sunk` | `#191210` on `#F2E7E3` | **15.23:1** | the moderator notice |
| `rule` on `paper` | `#ACA29F` on `#FDF3EF` | 2.28:1 | hairlines only — **decorative**, never a focus ring, never the sole boundary of a control |
| `mark` on `paper` | `#A12721` on `#FDF3EF` | **6.77:1** | 4 dp margin rules only — **graphical, never text** |
| **focus, reversed** | `#FDF3EF` on `#191210` | **16.91:1** | the focused entry |
| *`mark` on `ink`* | *`#A12721` on `#191210`* | *2.50:1 — fails* | *why the mark rule stays outside the reversed block* |

### Dark — "the reversed edition"

`board #110C0A` · `board-raised #1D1714` · `rule-d #524B49` (2.29:1, decorative) ·
`light-quiet #9F9693` (**6.72:1**) · `light #F0E9E7` (**16.26:1**) · `mark-d #C86459`
(**5.03:1** on `board`, 4.60:1 on `board-raised` — `board-raised` must never be
lightened). Focus reverses to `board` text on a `light` ground, **16.26:1**.

### Focus, geometrically

The focused entry — the whole merged node, rule to action line — **reverses**: ground
`ink` `#191210` spanning x = 12..378, text `paper` `#FDF3EF`, 12 dp of inner padding
so the 24 dp text column is unmoved and no glyph touches the ground edge. A **4 dp
`mark` rule at x = 0..4, on `paper`, outside the reversed block**, full node height,
8 dp of clear paper between rule and block. The mark never enters the black. Hairlines
inside a focused entry switch to `paper` at 30% — still decorative, still not a
boundary.

---

## 5. Four bands

| Band | Height | Treatment |
|---|---|---|
| **Status** | 24 dp | Reserved. `paper`, light status-bar icons. Nothing paints into it; the list scrolls *under* it only in the sense that the first hairline stops at its edge |
| **Title / nav** | 56 dp | The running head. Reserved, opaque `paper`, hairline beneath. Not a Material `TopAppBar`, not scroll-linked, not collapsing |
| **Content** | 763 dp | The record. Scrolls. First item is the contents line, last is the closing line |
| **Bottom** | 48 dp | Reserved for the gesture/home indicator. `paper`, **no tab bar**, no FAB, no control of any kind. The list's terminal padding is 48 dp so the closing line clears it fully |

Edge-to-edge is on; insets are consumed as padding, not ignored.

---

## 6. Content direction — shipped strings, verbatim

Everything below is either already in the product or plausible-length placeholder from
the product's own domain. No superlatives, no invented brand, no fabricated metric.

- Running head: `Sunday 17 August · Today's record`
- Contents line: `Contents · Today's record`
- Notice: `Moderator controls. Currently showing: the radio timeline. Using built-in sample memos.`
- Entry 1 — `From ruthm` / `7:42 pm · 24 seconds` / *"The new crossing by the library has a beeper now. It took two years of asking but it is there."*
- Transport, under entry 1 only: `Playing. 4 seconds left.` — 17 sp, `ink-quiet`,
  `tnum`, printed **in the column**, `liveRegion = Polite`. Not a floating bar, not a
  sheet, not a persistent mini-player.
- Entry 2 — `From tomh` / `7:15 pm · 41 seconds` / *"I found the audio description track on the second remote button, not the first. Took me a week."*
- Entry 3 — `From nadias` / `6:58 pm · 19 seconds`, transcript cut by the fold
- Actions, shipped words and shipped order: **`Pause`/`Play` · `Unlike`/`Like` ·
  `Skip` · `Reply` · `Report`**
- Closing line — hairline `rule`, `space300`, then `RadioViewModel.END_OF_STREAM`
  verbatim at 20 sp / 400, `ink`, ragged-right:

  > That is everything for now. The stream has ended — there is no more to scroll.
  > Come back later, or record a memo of your own.

  Then nothing. No "load more", no spinner, no suggested accounts. **This is the
  radio's own string and it is not the deck's** — the two are distinct and must not be
  collapsed.

---

## 7. Traversal — D-pad, switch access, TalkBack

1. **Running head** (heading; also the route-change focus target)
2. **Contents line** (button, first focusable element on every screen)
3. *Moderator notice* (text node, one stop — gone on a live build)
4. **Entry 1 — one merged node.** Spoken: speaker, time, transcript, then status.
   The five actions are **custom actions inside the node**, not separate stops
5. **Entry 2** — one node
6. **Entry 3** — one node …
7. **Closing line** (text node, terminal)

One stop per entry. A forty-row record is forty-three stops, not two hundred. The
action words remain touch targets inside a focused entry; they are not focus stops,
and the entry's reversal is the only focus indicator drawn.

The radio and the deck deliberately phrase the same action differently — *"Like this
memo"* here, *"Like this memo and move on"* on 03 — because the deck advances and the
radio does not. That asymmetry is preserved, not normalised.

---

## 8. Motion, and its reduced-motion frame

**One motion in the whole direction:** the entry's hairline rule draws left-to-right,
24 → 366 dp, over **180 ms**, as the record advances to that entry.

**Reduced motion is the same frame with the rule already drawn.** Under
`ANIMATOR_DURATION_SCALE == 0` the rule is at full length from the first frame — a
composed still, not `animation: none`, and not a missing rule. Nothing else on this
surface moves: no crossfade, no scroll-linked head, no ripple, no shimmer.

---

## 9. What this surface does that its neighbours do not

It is the only surface carrying the **full** apparatus — running head, contents line
and notice, stacked as front matter — and the only one where the repeating unit
repeats. 03-card-deck shows this exact entry at these exact sizes, alone; 05-profile
shows this exact column again, date-grouped with status printed inline. The argument
of the concept is that those are the same object at three densities, so nothing here
is allowed to soften: same 20 sp floor, same action words in the same order in the
same place, same 326 dp unit.
