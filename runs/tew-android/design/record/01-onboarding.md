# record: Surface 1 of 6 — 01-onboarding

**Coded comp.** No image is attached: this environment has no image-generation MCP
server available, so this surface ships as a spec block only. Every number below is
stated rather than rendered.

**Frame:** one phone, **390 dp wide**, 844 dp reference height, light mode ("the
page"). Dark mode ("the board") stated per row, not drawn separately.
**Platform:** Android-native, minSdk 26, budget hardware. Type in `sp`, padding in
dp, and **no `sp` value is ever summed** — line-height is a multiplier on the type
box, block heights belong to the text engine, and every fixed number below is dp.

**Leaf rendered: 3 of 5.**

---

## 1. The move

Five numbered leaves of a preface. **One idea per leaf and nothing else on the
leaf.** The step counter is not a line on the page — it is set into the running
head, where a screen reader speaks it as part of the section name, so the position
arrives with the section rather than as a stray fragment above the idea. The idea
itself sets at **display, 34 sp**, filling the column ragged-right and wrapping
freely. A hairline separates it from the actions. Nothing else is on the leaf.

**No progress dots.** A dot row is a count rendered as a graphic, and to this
audience it is a count rendered as nothing at all. The count lives in the head,
spoken.

**This is the emptiest surface in the set, deliberately.** 02-radio and 05-profile
are dense scrolling columns; 03-card-deck is one entry alone; 04-record is a big
low-sitting control; 06-sign-in is a short centred run. This one is a single large
sentence in a mostly empty field, and the emptiness between the idea and the
actions is a held spacer with a stated minimum, not a leftover.

### Vertical spec, top to bottom, 390 dp frame

| # | Element | Number |
|---|---|---|
| 1 | Status inset | System `statusBars` inset, reserved, never painted under. 24 dp nominal on the reference device; the layout consumes the real inset, whatever a cutout makes it |
| 2 | **Running head band** | Fixed **56 dp minimum**, reserved not floating, does not collapse on scroll. Grows with font scale rather than clipping. Text `How this works · 3 of 5`, 15 sp / 500, `ink-quiet`, sentence case, left at the 24 dp screen margin, vertically centred in the band |
| 3 | Head rule | **1 dp** hairline, `rule`, full bleed 0 → 390 dp. Decorative only |
| 4 | Gap | **24 dp** (`space300`) |
| 5 | **The idea** | Column **342 dp** (390 − 24 − 24). 34 sp / 600, line-height **1.25**, ragged-right, no hyphenation, no justification. At 100% scale this copy sets to **9 lines** at ≈18–22 characters per line (derived from the direction's measured 30–38 ch at 20 sp on the same 342 dp column). Height is the text engine's, never a fixed dp |
| 6 | **Held space** | Flexible spacer, weight-filled, **24 dp minimum**. ≈161 dp at 100% scale on an 844 dp frame. This is the first thing to collapse as type grows; after it reaches 24 dp the content column scrolls |
| 7 | Action rule | **1 dp** hairline, `rule`, inset to the 24 dp margins (342 dp wide) — it separates the idea from the actions and is not a boundary, a focus ring, or an underline |
| 8 | Gap | **16 dp** (`space200`) |
| 9 | **`Next`** | Action line, 20 sp / 600, `ink`, left at the margin. Target = the text box (≈29 dp at 100% scale) + **10 dp** above and below = **49 dp**, floor-clamped to **48 dp**; the target spans the full 342 dp column, past the visible glyphs |
| 10 | Gap between targets | **8 dp** |
| 11 | **`Back`** | Identical geometry to `Next` |
| 12 | Gap | **24 dp** (`space300`) |
| 13 | Bottom inset | **48 dp reserved** — enough for 3-button navigation; gesture navigation's 24 dp sits inside it. Reserved whichever is active, so the actions never move between devices |

Sum of the fixed dp at 100% scale: 24 + 56 + 1 + 24 + [9 lines] + [spacer] + 1 + 16
+ 49 + 8 + 49 + 24 + 48. Nothing here is an `sp` added to a dp.

**At 200% font scale:** the head band grows (15 sp → 30 sp, band ≥56 dp), the idea
runs to ≈18 lines, the held spacer collapses to 24 dp, and the content column
scrolls between a fixed head and a **pinned** action block. Nothing truncates,
nothing ellipsises, and the actions never scroll off — chrome that moves is chrome
a switch-access user has to re-find.

---

## 2. Type table

One family: **Atkinson Hyperlegible Next**, variable `wght` 200–800, roman only,
SIL OFL 1.1, bundled at `res/font/AtkinsonHyperlegibleNext[wght].ttf` (112 KB).
Bundled, not downloadable — a reflow is a re-read. **No italic ships at all.**

| Role | Face | Size | Weight light / dark | Line-height | On this leaf |
|---|---|---|---|---|---|
| Display | Atkinson Hyperlegible Next | **34 sp** | 600 / 550 | 1.25 | the idea |
| Speaker line | " | 24 sp | 600 / 550 | 1.30 | not used here |
| Body / transcript | " | **20 sp** (the floor) | 400 / 350 | 1.45 | not used here |
| Action line | " | **20 sp** | 600 / 550 | 1.45 | `Next`, `Back` |
| Time / secondary | " | 17 sp | 400 / 350 | 1.40, `tnum` | not used here |
| Running head | " | **15 sp** | 500 / 450 | 1.35 | `How this works · 3 of 5` |

Three of six roles are unused on this leaf. That is the leaf working.

**Four hard rules, held:** ragged-right always, never justified · no italics ·
**never underline text** — the actions carry weight (600) and a hairline *above*
them, and a rule above a line is not an underline · no all-caps and no small caps
anywhere, including the running head, which a record would conventionally set in
caps and which loses to the audience here.

**Dark mode drops every weight 50 units** (400→350, 600→550, 500→450) because the
same weight blooms on a dark ground.

---

## 3. Colour, measured

Light — "the page":

| Token | Hex | Against | Ratio | On this leaf |
|---|---|---|---|---|
| `paper` | `#FDF3EF` | `ink` | **16.91:1** | the substrate, whole frame |
| `paper-sunk` | `#F2E7E3` | `ink` | **15.23:1** | not used here — no notice block on a preface leaf |
| `rule` | `#ACA29F` | on `paper` | 2.28:1 | head rule, action rule. **Decorative only** — never a boundary, never a focus ring |
| `ink-quiet` | `#5A504D` | on `paper` | **7.14:1** | the running head |
| `ink` | `#191210` | on `paper` | **16.91:1** | the idea, `Next`, `Back` |
| `mark` | `#A12721` | on `paper` | **6.77:1** | the focus rule only. Graphical, **never text** |
| *focus, reversed* | `ink` ground, `paper` text | — | **16.91:1** | the focused line |

Dark — "the board":

| Token | Hex | Against | Ratio |
|---|---|---|---|
| `board` | `#110C0A` | `light` | **16.26:1** |
| `board-raised` | `#1D1714` | `light` | **14.86:1** |
| `rule-d` | `#524B49` | on `board` | 2.29:1 |
| `light-quiet` | `#9F9693` | on `board` | **6.72:1** |
| `light` | `#F0E9E7` | on `board` | **16.26:1** |
| `mark-d` | `#C86459` | on `board` | **5.03:1** |
| *focus, reversed* | `light` ground, `board` text | — | **16.26:1** |

**The one accent appears nowhere at rest on this leaf.** `mark` is held entirely in
reserve and shows only as the focus rule. A preface leaf with no focus on it is
two inks on paper and a hairline, and that is correct — a colour with two jobs has
none.

**Focus state, both channels.** (a) The focused line **reverses**: `ink` ground,
`paper` text, 16.91:1 light / 16.26:1 dark, block running the full 342 dp column
with its text inset 12 dp inside it. (b) A **4 dp `mark` rule in the leading
margin**, x = 8–12 dp, spanning the focus block's full height, with 12 dp clear
between it and the block edge — **outside** the reversed block, because `mark` on
`ink` measures **2.50:1** and a mark rule inside a reversed block is a dark-on-dark
smudge. Two channels means the indicator survives monochrome and survives a reader
who cannot see red.

**Reduce Transparency is a no-op by construction** — no scrim, no blur, no
translucency, no elevation shadow carrying meaning anywhere on this leaf.

---

## 4. The four bands

| Band | On this leaf | Reserved or drawn |
|---|---|---|
| **Status** | System status bar | **Reserved.** Real inset consumed; `paper` runs under it as substrate but no glyph, rule, or target enters it |
| **Title / nav** | The running head, `How this works · 3 of 5`, + its 1 dp rule | **Drawn.** Fixed 56 dp minimum, never floating, never collapsing |
| **Content** | The idea at 34 sp, then the held spacer | **Drawn**, scrolls at large type |
| **Bottom** | Action rule, `Next`, `Back`, then the 48 dp navigation inset | **Drawn + reserved.** The 48 dp inset is empty and its height is held |

Nothing runs edge-to-edge in all four directions. The two hairlines are the only
full-bleed elements and they are horizontal.

**One deliberate deviation from the running-head convention, flagged:** the running
head elsewhere carries the date. A preface is undated — it precedes the record
rather than sitting in it — so the counter takes the date's slot. Flagged for the
conductor rather than assumed.

**One open collision, flagged:** the direction says the contents line is the first
focusable element after the running head **on every screen**. This leaf has no
contents line, because "one idea and nothing else on the leaf" and a navigation
line on the leaf cannot both be true, and the preface is what the contents line
*opens*. The exit route from the preface is `Back` from leaf 1; what leaf 1's
`Back` does is not specified in the shipped copy and is **not invented here**.

---

## 5. Traversal — D-pad, switch access, TalkBack

Four nodes on this leaf. Not five.

1. **Running head** — `heading()`. Receives focus on leaf change (route-change
   rule), so "How this works, 3 of 5" is spoken before the idea.
2. **The idea** — one focusable node. Explicitly focusable so D-pad and switch
   scanning reach it; on this surface the idea *is* the screen, and a scan that
   skips it skips everything.
3. **`Next`** — primary, first in order so linear traversal and switch scanning
   reach the forward move before the backward one.
4. **`Back`**.

**Advance is button-driven, never gesture-driven** — a swipe may not reach the app
under TalkBack. Routes: the `Next` control · D-pad right · the shipped voice
command · **and the screen reader's custom actions menu**, carrying `Next leaf`
and `Previous leaf`. That last route is not optional here: this leaf's copy claims
every button is also in the actions menu, and the leaf making the claim has to be
the first thing that honours it.

---

## 6. Motion / reduced motion

The system adds exactly one motion: the entry rule drawing left-to-right as the
record advances, **180 ms**. On this leaf that is the **action rule** redrawing as
a leaf turns. Under `ANIMATOR_DURATION_SCALE == 0` the reduced state is **the same
composed frame with the rule already at full length** — a still that was
art-directed, not `animation: none`.

**Leaf changes never slide horizontally.** A horizontal transition implies a swipe
affordance that does not exist here, and teaching a gesture the product refuses is
worse than no motion. Content is replaced in place; focus moves to the running head.

---

## 7. The nine data states

**All nine are N/A, with reason.** The copy is local and compiled in, nothing is
fetched, nothing can fail, and the microphone is not requested until 04-record.

| State | Why N/A here |
|---|---|
| Empty | The leaf's content is a fixed string; there is no collection that can be empty |
| Loading | Nothing is fetched. No skeleton, because there is no wait |
| Partial | Nothing is fetched, so nothing can arrive partly |
| Error | No I/O on this surface |
| Permission denied | No permission is requested. The microphone is first requested on 04-record |
| Offline / degraded | The preface is fully functional with the radio off |
| Stale | Local constants do not go stale; the running head carries no fetch time here |
| Conflict | Nothing on this surface is user-owned data that another actor can remove |
| Bulk | One leaf at a time is the interaction model |

Stated rather than left silent, so a builder does not go looking for the branch.

---

## 8. Copy — verbatim, unchanged

- Running head: **`How this works · 3 of 5`**
- Idea: **`Every one of those has a button on screen, and every one is also in your
  screen reader's actions menu. You never have to find a particular spot on the
  glass.`**
- Actions: **`Next`**, **`Back`**. Leaf 5's primary is `Start listening`.

**There is no `Skip` control in this product and none is drawn.** No invented
strings, no placeholder, no marketing line, no fabricated number.

**Content direction, one line:** the leaf teaches one promise in the product's own
plain voice and then gets out of the way — it must read like a preface paragraph a
person wrote, not a tour card, so the type does the emphasis and nothing else does.

---

## 9. The collision, checkable

> *"An official report of the day's speaking, set as a large-print edition — the
> record's structure at a blind reader's type size, so the thing that makes it
> authoritative and the thing that makes it readable are the same decision."*

Structure from the official report: one column, a running head with the section
name and position, a hairline entry rule, one idea per numbered leaf, actions set
as lines in the column rather than as buttons. Surface from the large-print
edition: 34 sp for the idea, a 20 sp floor, ragged-right, no italics, no
underlines, no caps, 16.91:1 body contrast — RNIB's standard, not WCAG's minimum.
**The invention holds:** there is no margin left at these sizes, so the marginal
apparatus — the step count, which a printed report would set in the outer margin —
moves into the reading line, into the running head, where it is spoken.

---

## 10. Tokens, logged

- **Composition anchor:** `top-left-lead`
- **Background mode:** `flat-surface`

Logged as used, not as scored. The eye enters at the running head and the 34 sp
idea in the top-left and the support falls to the bottom of the field; the
substrate is one flat sheet of `paper` with two hairlines and no image, gradient,
texture, or second field anywhere. A record is printed on one paper, so the rest of
this set will very likely also be `flat-surface` — that sameness is the design, and
the set check should be read knowing it.
