# record: Surface 3 of 6 — 03-card-deck

**Coded comp.** No image is attached: this environment has no image-generation MCP
server loaded, so image mode was unavailable and this ran as a spec block.

- **Composition anchor:** `top-left-lead`
- **Background mode:** `flat-surface`

Platform mode: **Android-native**, minSdk 26, budget hardware. Type in `sp`, spacing in
`dp`. **No `sp` value is summed with any other value anywhere below** — every stated
block height is `dp` derived from one `sp` size × its line-height, and every gap is `dp`.

---

## 1. The frame and the four bands

One phone frame, **390 × 844 dp**, portrait, `fontScale = 1.0`, light mode.

| Band | y (dp) | Height | What holds it |
|---|---|---|---|
| **Status** | 0 – 24 | 24 dp | Reserved. System-drawn, `paper` behind it, light status icons. Nothing of ours enters it |
| **Title / nav** | 24 – 80 | **56 dp** | The running head. Fixed band, **reserved not floating**, hairline at its foot |
| **Content** | 80 – 796 | 716 dp | Contents line, moderator notice, one entry, action lines, the last rule, the empty field |
| **Bottom** | 796 – 844 | **48 dp** | Reserved for the gesture-navigation inset. **Deliberately empty — nothing is painted here.** There is no bottom nav bar; the five shipped nav items moved into the contents line |

The bottom band being empty is a reservation, not an oversight: it is the height the
system inset consumes, and the empty field above it must not run into it.

---

## 2. Layout move, with numbers

**One column, hung from the top-left, ending early.** Horizontal gutter **24 dp** both
sides → text column **342 dp** wide. Every rule spans the text column (x 24 → 366), not
the frame. Ragged-right throughout; nothing centres.

The move is: everything the screen has is finished by **y = 555**, and the remaining
**241 dp — 29% of the frame — is `paper` with nothing on it.** That field is the design.
02-radio runs the identical unit at the identical sizes and does not stop; this one stops.

| y (dp) | Height | Element |
|---|---|---|
| 24 – 80 | 56 | **Running head** — `Sunday 17 August · One at a time`, 15 sp, `ink-quiet`, sentence case, vertically centred in the band |
| 79 – 80 | 1 | Hairline, `rule`, **full-bleed x 0 → 390** (this one is chrome, so it spans the frame) |
| 80 – 129 | **49** | **Contents line** — `Contents · Card deck`, action-line style. 10 dp pad, 29 dp line, 10 dp pad |
| 129 – 201 | 72 | **Moderator notice** — `paper-sunk` block, 12 dp inset, two lines at 17 sp |
| 201 – 225 | 24 | Field |
| 225 – 226 | 1 | **Entry rule**, `rule`, x 24 → 366. The one animated element on the screen |
| 226 – 238 | 12 | Nested pad |
| 238 – 269 | 31 | **Speaker line** — `From ruthm`, 24 sp / 600 / 1.30, `heading()` |
| 269 – 273 | 4 | |
| 273 – 297 | 24 | **Time line** — `7:42 pm · 24 seconds`, 17 sp, `tnum`, `ink-quiet` |
| 297 – 309 | 12 | |
| 309 – 396 | 87 | **Transcript** — 3 lines × 29 dp, 20 sp / 400 / 1.45, ragged-right, **unhyphenated** |
| 396 – 408 | 12 | |
| 408 – 432 | **24** | **Transport line** — height held whether or not it has text. Empty in the comp |
| 432 – 444 | 12 | |
| 444 – 445 | 1 | Hairline **above** the action lines, x 24 → 366 |
| 445 – 494 | **49** | **Action line 1** — `Like` · `Skip` · `Play` |
| 494 – 543 | **49** | **Action line 2** — `Reply` · `Report` |
| 543 – 555 | 12 | |
| 555 – 556 | 1 | **THE LAST RULE**, `rule`, x 24 → 366 |
| 556 – 796 | **240** | **Empty. `paper`. Nothing.** |

**Why the transport line's 24 dp is held empty:** it carries the shipped
`Loading the audio…` and the playback line. If it collapsed when idle, the action lines
would jump 36 dp the instant audio buffered — under a magnifier that is a lost place.
Held height, `invisibleToUser` while blank so traversal does not stop on nothing.

### The action words are words, not buttons

`Like` at 20 sp / 600 measures ≈ 40 dp wide × 29 dp tall. Each word takes:

- **10 dp padding above and below → 49 dp**, clearing the 48 dp touch floor with no chrome.
- **Touch bounds expanded horizontally past the visible glyph box to 48 dp minimum**,
  symmetric — ≈4 dp of invisible overhang each side on the shortest word.
- **16 dp visible gap between words**, which leaves ≥8 dp of clear space between two
  expanded rects. **No two targets overlap.** Separator is a `·` in `rule`, decorative.
- Three words on line 1 at 40 + 16 + 38 + 16 + 42 ≈ 152 dp of 342. Two on line 2 ≈ 118 dp.
  Both lines have room to grow before they wrap.

No underline (RNIB, and it is banned outright). No box, no fill, no ripple rectangle —
weight 600 and the hairline above are the whole affordance.

**No count sits beside any action.** No like total, no card number, no "3 of 40".

---

## 3. Type table

Atkinson Hyperlegible Next, variable, **roman only**, bundled as
`AtkinsonHyperlegibleNext[wght].ttf`. **Identical to 02-radio in every row** — that is
the point of this surface, and the temptation to scale up into the empty field is refused.

| Role | Used for | Size | Weight light / dark | Line-height | Rendered block | Colour |
|---|---|---|---|---|---|---|
| Running head | `Sunday 17 August · One at a time` | 15 sp | 500 / 450 | 1.35 | 20 dp | `ink-quiet` |
| Speaker line | `From ruthm` | 24 sp | 600 / 550 | 1.30 | 31 dp | `ink` |
| Body | transcript | 20 sp | 400 / 350 | 1.45 | 29 dp / line | `ink` |
| Action line | `Like` `Skip` `Play` `Reply` `Report`, `Contents · Card deck` | 20 sp | 600 / 550 | 1.45 | 29 dp | `ink` |
| Time / secondary | `7:42 pm · 24 seconds`, transport line, moderator notice | 17 sp | 400 / 350 | 1.40 | 24 dp | `ink-quiet` |
| Display 34 sp | **not used on this surface** | — | — | — | — | — |

`tnum` on the time line only. Sentence case everywhere — **no caps, no small caps, no
italic, no underline**, including on the running head a printed record would set in caps.

**Font-scale behaviour.** The `dp` gaps in §2 are fixed; the text blocks grow. At
`fontScale 1.3` the transcript's three lines become four and the column ends at ≈ 640 dp —
the empty field absorbs it and nothing scrolls. Past ≈ **1.6** the column scrolls, and
**the last rule scrolls with it**; it is content, not a footer pinned to the frame.

---

## 4. Colour, measured

Every pair below was recomputed from the hexes and agrees with `DIRECTION.md` §6a to
within 0.05 (WCAG rounding of the sRGB channels; the file's figures are canonical).

### Light

| Fg | Bg | Hexes | Ratio | Role on this screen |
|---|---|---|---|---|
| `ink` | `paper` | `#191210` on `#FDF3EF` | **16.91:1** | speaker line, transcript, all five action words, contents line |
| `ink-quiet` | `paper` | `#5A504D` on `#FDF3EF` | **7.14:1** | running head, time line, transport line |
| `ink-quiet` | `paper-sunk` | `#5A504D` on `#F2E7E3` | **6.44:1** | moderator notice text |
| `rule` | `paper` | `#ACA29F` on `#FDF3EF` | **2.28:1** | all four hairlines and the `·` separators — **decorative only, never text** |
| `paper` | `ink` | `#FDF3EF` on `#191210` | **16.91:1** | the reversed focused line |
| `mark` | `paper` | `#A12721` on `#FDF3EF` | **6.77:1** | the 4 dp focus rule in the margin — **graphical only, never text** |
| ~~`mark`~~ | ~~`ink`~~ | `#A12721` on `#191210` | **2.50:1 — FAILS** | why the focus rule is outside the reversed block, not inside it |

### Dark

`board #110C0A` · `board-raised #1D1714` · `rule-d #524B49` · `light-quiet #9F9693` ·
`light #F0E9E7` · `mark-d #C86459`. Weights drop 50 units per the table in §3.

| Fg | Bg | Ratio | Note |
|---|---|---|---|
| `light` on `board` | `#F0E9E7` / `#110C0A` | **16.26:1** | body, speaker, actions |
| `light-quiet` on `board` | `#9F9693` / `#110C0A` | **6.72:1** | running head, time line |
| `light-quiet` on `board-raised` | | **6.13:1** | moderator notice — still clears 4.5:1 |
| `mark-d` on `board` | | **5.03:1** | focus margin rule |
| `mark-d` on `board-raised` | | **4.58:1** | **barely over the bar — `mark-d` never lands on `board-raised` on this screen**, and the moderator notice is the only `board-raised` block here |
| `rule-d` on `board` | | **2.28:1** | hairlines, decorative |

**Two colour fields total.** `paper` everywhere, `paper-sunk` for the moderator notice
only. No third surface, no elevation, no shadow, no card fill behind the entry — the
entry is defined by its two rules and its spacing, not by a container. That is what makes
the empty field below the last rule read as absence rather than as an unfilled box.

### Focus, drawn

Two channels, both required, on every focusable stop:

1. **The line reverses.** `ink` ground, `paper` text, **16.91:1**. The reversed block
   bleeds **8 dp beyond the text column on each side (x 16 → 374) and 8 dp above and
   below** the glyph box. Text never moves — the block grows around it, so focusing
   causes zero reflow.
2. **A 4 dp `mark` rule in the leading margin, at x 8 → 12**, full height of the reversed
   block, with 4 dp of clear `paper` between it and the block's left edge at x 16.
   **On `paper` at 6.77:1, outside the reversed block** — inside it, it measures 2.50:1
   and is a dark smudge.

Survives monochrome (channel 1) and survives a red-blind reader (channel 1 again). Same
indicator for D-pad traversal and for switch-access scanning.

---

## 5. Content direction

One memo, real length, in the record's plainspoken voice; nothing invented and no
fabricated metrics.

- Running head: `Sunday 17 August · One at a time`
- Contents line: `Contents · Card deck` (opens contents as a **full screen**, not a sheet)
- Moderator notice, shipped verbatim from `TewApp.kt:209` with the `CARD_DECK` parameter:
  `Moderator controls. Currently showing: the card deck. Using built-in sample memos.`
  Present on this surface because the deck is reachable only from moderator controls;
  **absent entirely on a live build**, and the 72 dp collapses with it.
- Speaker line, shipped pattern `From ${authorUsername}`: `From ruthm`
- Time line: `7:42 pm · 24 seconds`
- Transcript, three lines of plausible domain-real memo, ragged-right, unhyphenated:
  `Got the new talking scales working in the end — the trick was pairing them before you
  plug the kettle in, not after. Took me two evenings. Worth it.`
- Action lines: `Like` `Skip` `Play` — `Reply` `Report`

`Play` becomes `Again` only after this card's audio has been used, per the shipped
`playLabel` rule. Under a screen reader nothing autoplays, so `Play` is honest on arrival.

---

## 6. Traversal order

Linear, top to bottom, one column, no traps.

| # | Stop | Announced as |
|---|---|---|
| 1 | Running head | `Sunday 17 August. One at a time. Heading.` — the one top-level heading |
| 2 | Contents line | `Contents. Card deck. Button.` — first focusable after the head, on every screen |
| 3 | Moderator notice | polite region; moderator builds only |
| 4 | **Speaker + time, merged** | `From ruthm. Heading. 7:42 pm. 24 seconds.` — carries all seven custom actions |
| 5 | Transcript | its own stop, reachable **on request** |
| 6 | Transport line | skipped while blank (`invisibleToUser`); a stop only when it has text |
| 7–11 | `Like`, `Skip`, `Play`, `Reply`, `Report` | five separate stops, in that order |
| — | The last rule | `invisibleToUser`. **Nothing focusable exists below it** |

**The transcript is not merged into stop 4** and is not in the polite announcement — the
recording says those same words in the author's voice a second later, and both at once is
followable by nobody. It is not hidden, just not read over the audio.

**`collectionInfo` is NOT set** on any node on this screen. Setting it makes TalkBack say
*"item 3 of 40"*, which is a count, and there are no counts here. `describePosition` is
also not used on the deck.

**Three routes to every action, non-negotiable:**

| Action | Route 1 — the word | Route 2 — custom action on stop 4 | Route 3 — gesture |
|---|---|---|---|
| Like | `Like` | `Like this memo and move on` | swipe **right** |
| Skip | `Skip` | `Skip this memo` | swipe **left** |
| Reply | `Reply` | `Reply with a voice memo` | swipe **up** |
| Play | `Play` / `Again` | `Play this memo` / `Play this memo again`, `Play or pause` | — |
| Report | `Report` | `Report this memo` | — |
| Voice | — | `Speak a command` | — |

Swipes are **direction-only**: 48 dp accumulated over the whole drag, **no velocity
threshold, no time limit**, so a slow or wandering gesture still counts. **No double-tap,
no long-press.** The swipe is an enhancement and is never the only route — the words are
always on screen, which is why the action lines are not collapsible.

### Focus on replacement

A card acted on and replaced hands focus to **stop 4 of the new card** — never the screen
top, never stop 1. Announced **polite**, via the shipped `cardAnnouncement`:
`Memo from ruthm. Like, skip, or reply.` (under a screen reader:
`Memo from ruthm. Press play, or like, skip, or reply.`). The transcript is not in it.

---

## 7. States on this block

| State | Layout |
|---|---|
| **Loading** | Running head, contents line and the entry rule draw immediately. Below it, held-height `paper-sunk` skeletons at the **exact** heights of §2 — 31 dp speaker box, 24 dp time, 87 dp transcript, 24 dp transport. Action lines present, `ink-quiet`, `disabled` semantics. The last rule is already at y 556. **Layout never shifts when content lands.** Polite: `Finding memos for you.` |
| **Empty** | Entry rule, then the shipped `EMPTY_FEED` verbatim — `There are no memos waiting for you right now. Come back later, or record one yourself.` — at 20 sp body, then one action word `Record a memo`, then the last rule, then the field |
| **Terminal** | Entry rule, then the closing line verbatim, then the last rule, then nothing. **No action lines** — there is nothing to like, skip or report |
| **Error** | `You seem to be offline. Check your connection and try again.` printed in the column as a notice, **never a toast**, with a `Try again` word beneath |
| **Conflict** | `This memo was removed while you were here.` — **assertive**, and focus stays put |
| **Bulk** | N/A with reason: one memo at a time is the entire interaction model of this surface |

### The closing line, verbatim

> That is the last card. The stream has ended — there is no more to swipe through. Come
> back later, or record a memo of your own.

`CardDeckViewModel.END_OF_STREAM`, set at 20 sp / 400 / 1.45, four lines ≈ 116 dp, rule
above at y 225, **nothing below**. This is the **deck's** string. `RadioViewModel` has a
different one ending *"no more to scroll"* — 02-radio uses that. **They are not merged.**

---

## 8. Motion and reduced motion

One animation on this surface and no other: **the entry rule at y 225 draws left to right
over 180 ms** as a card is replaced. Nothing translates, nothing fades, **the card does
not fly off-screen on a swipe** — the swipe commits and the content is replaced in place.

Reduced motion (`ANIMATOR_DURATION_SCALE == 0`): **the same frame with the rule already at
full length from frame one.** A composed still, not `animation: none` — the comp is
identical in both settings except for the 180 ms.

---

## 9. Distinction from the neighbours

01-onboarding is one 34 sp idea in an empty field. 02-radio is this exact unit at these
exact sizes, repeated down a scrolling column. 04-record is a 96 dp control sitting low.
05-profile is a dense date-grouped column with `collectionInfo` on. 06-sign-in is a short
centred run with no running head.

**This surface's difference from 02 is singularity and the 240 dp below the last rule —
not one point of type.** Same scale, same words, same order, same place. A concept that
grew its type because a screen had room would have shipped two products.

---

## Read-back

- Every hex on the board is from the palette table; no value invented. `mark` appears once
  and only as a 4 dp graphic. `rule` appears only as hairlines and separators.
- All body-carrying pairs measured: lowest is **6.44:1** (`ink-quiet` on `paper-sunk`),
  clearing 4.5:1. The two sub-3:1 values are `rule` and `mark`-on-`ink`, and neither
  carries text anywhere in this spec.
- Four bands present: 24 / 56 / 716 / 48, summing to 844.
- Smallest touch target is 48 dp; the action words reach it by type size plus 10 dp of
  padding, with expanded horizontal bounds and no overlap.
- No `sp` value was added to any other value. No counts. No `collectionInfo`. Both
  terminal strings kept separate.
- `top-left-lead` and `flat-surface` describe what is actually specified above.

## Unsatisfied

- **No image.** Image mode was requested as unavailable and is unavailable — no image MCP
  server is loaded in this environment. This is the coded comp in its place.
- The moderator notice occupies 72 dp on the only build where this surface is reachable,
  which means **the shipped screen and the live screen differ by 72 dp**. Named, not
  resolved: it is a product question about whether the deck ever ships to real users.
