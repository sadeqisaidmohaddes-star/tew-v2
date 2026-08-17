# record: Surface 4 of 6 — 04-record

**Coded comp.** No image generated: this environment has no image-generation MCP
server loaded, so this surface ships as a spec block. Every number below is stated
rather than rendered.

- **Composition anchor:** `bottom-anchored`
- **Background mode:** `flat-surface`
- **Frame:** one phone, **390 × 844 dp**, portrait, `fontScale 1.0`. Bare screen, no
  device bezel.
- **Platform mode:** Android-native, minSdk 26. Type in `sp`, every box and gap in
  `dp`. **No dp figure below is derived by adding an sp figure to a dp figure.**

---

## 1. The layout move, with numbers

**The page is empty in the middle and heavy at the bottom.** One live number sets
near the top at the largest size in the product; then roughly a third of the screen
is nothing at all; then the one real button in the app, sitting where a thumb lands
without the phone being looked at.

That emptiness is the move. Every other surface in this set fills its column —
02-radio and 05-profile are dense scrolling runs, 03-card-deck is one entry filling
the field, 01-onboarding is one idea centred, 06-sign-in is a short centred run.
This is the only surface whose weight sits **low**, and the only one carrying a
physical control. The gap between the number and the button is what makes the button
findable by feel: there is nothing else down there to hit.

### Vertical, top to bottom (Recording state, `fontScale 1.0`)

| y (dp) | Height | Element |
|---|---|---|
| 0–24 | 24 | **Band 1 — status.** Reserved, not painted. `paper` runs under it; no text, no target enters it. Height is the real inset, 24 dp is the floor |
| 24–80 | 56 | **Band 2 — running head.** Fixed, reserved, never floating. `Record a memo`, 15 sp, `ink-quiet`, sentence case, left at x=24 |
| 80–81 | 1 | Hairline, `rule`, x 24→366. Decorative only |
| 81–122 | 41 | **Contents line.** 12 dp top padding + one 20 sp line. `Contents`, action-line weight, `ink`. First focusable after the running head |
| 122–146 | 24 | `space300` |
| 146–274 | **min 128** | **Status block.** The live line. Min-height = 3 display lines at 34 sp/1.25; it is a *minimum* and grows with font scale, never a cap |
| 274–286 | 12 | `space150` |
| 286–315 | 29 | Status remainder line, 20 sp/1.45 — present only past 90 s |
| 315–652 | **flex** | **Band 3 — content field.** The open page. 337 dp here; `weight(1f)`, absorbs every state difference. Min 48 dp |
| 652–676 | 24 | `space300` |
| 676–772 | **96** | **The control.** `BIG_CONTROL_DP = 96`, full measure, x 24→366 |
| 772–796 | 24 | `space300` |
| 796–844 | 48 | **Band 4 — bottom.** Reserved for the system navigation inset: 24 dp gesture handle + 24 dp keep-out. No app chrome on this screen — it is a task leaf, the nav lives in the contents line. Nothing focusable enters it, so a swipe-up never competes with the control |

Horizontal: screen padding 24 dp both sides. Measure = **342 dp**. Ragged-right, no
hyphenation, nothing centred.

### Finished state — the same frame, the bottom third redistributed

The 96 dp control is **not shown** after a stop; its 96 dp plus the 24 dp above it
goes to the action words. Bottom-up:

| y (dp) | Height | Element |
|---|---|---|
| 516–528 | 12 | `space150` |
| 528–529 | 1 | Hairline, `rule`, x 24→366 — the rule sits **above** the action group, never under a word |
| 540–588 | 48 | `Listen back` |
| 596–644 | 48 | `Post memo` / `Send reply` |
| 652–700 | 48 | `Record again` |
| 700–724 | 24 | `space300` — the gap that separates the reversible actions from the one that is not |
| 724–772 | 48 | `Cancel` |
| 772–796 | 24 | `space300` |
| 796–844 | 48 | Band 4 |

Field flexes to 332–516 (184 dp). The status block keeps its 128 dp minimum in every
state, so **the top edge of the field never moves between Idle, Recording and
Finished.** Nothing under the user reflows while they are speaking.

### Why the action rows are 48 dp and the gaps are 8/24, not 12

An action word is a 20 sp line — a 29 dp line box, under the floor. Each word is
therefore laid as a **48 dp row** with the word optically centred, so the target is
48×48 dp with no invisible extension needed and no arithmetic that mixes sp into dp.
8 dp between the three reversible actions; **24 dp before `Cancel`**. A 12 dp gap
between 29 dp lines with extended 48 dp boxes would have overlapped by 3.5 dp each
pair — the reason the rows are boxes rather than padded text.

### Why the one button is allowed to exist

Everywhere else in **record**, an action is a word with no chrome. Here the exception
is load-bearing and the line is narrow: **the record control is the only action in
the product performed while not looking at the screen and while about to speak.**
Posting, replaying and cancelling are decisions taken with attention on the phone;
aiming a thumb at a button before you start talking is not. A 96 dp full-measure slab
is findable by feel from the bottom edge. `Post memo` is a decision, so it stays a
word — the exception does not spread to it.

`Allow microphone` reuses the identical 96 dp slot, matching the shipped
`RecordScreen`, because it is the same act: the one thing that must be pressed before
speaking is possible.

---

## 2. Type table — this surface

Atkinson Hyperlegible Next, variable, roman only, bundled in `res/font/`. Weights are
light / dark; dark drops 50. Ragged-right, no italics, no underline, no caps.

| Element | Role index | Size | Weight | Line-height | Feature |
|---|---|---|---|---|---|
| `Record a memo` (running head) | Running head | 15 sp | 500 / 450 | 1.35 | — |
| `Contents` | Action line | 20 sp | 600 / 550 | 1.45 | — |
| `Recording. 13 seconds so far.` | **Display** | **34 sp** | 600 / 550 | 1.25 | **`"tnum"`** |
| `Recorded 24 seconds.` | **Display** | **34 sp** | 600 / 550 | 1.25 | **`"tnum"`** |
| ` This is getting long — …` | Body | 20 sp | 400 / 350 | 1.45 | — |
| `Listen back, send it, or record it again.` | Body | 20 sp | 400 / 350 | 1.45 | — |
| `Ready. Press Start recording …` | Body | 20 sp | 400 / 350 | 1.45 | — |
| Permission notice | Body | 20 sp | 400 / 350 | 1.45 | — |
| Control label | Action line | 20 sp | 600 / 550 | 1.45 | — |
| `Listen back`, `Post memo`, `Record again` | Action line | 20 sp | 600 / 550 | 1.45 | — |
| `Cancel` | Body | 20 sp | **400 / 350** | 1.45 | — |

**`tnum` is on this screen and this screen only** among the six. It is the one live
number in the product, it advances once a second, and without tabular figures the
whole line shifts left and right as a `1` replaces a `4`. For a low-vision reader
tracking the number by shape at arm's length, that jitter is the failure.

**The 34 sp is the number, not the sentence.** The rule, applied identically in both
states that carry a duration: **the sentence containing the duration sets at Display;
the rest of the shipped string sets at Body underneath it.** So
`Recording. 13 seconds so far.` at 34 sp, ` This is getting long — short memos are
easier to listen to.` at 20 sp beneath; `Recorded 24 seconds.` at 34 sp,
`Listen back, send it, or record it again.` at 20 sp beneath. **No character of
shipped copy changes and the spoken string is still assembled whole** — only the
setting is split. Setting the 90-second nudge at 34 sp would run to four lines and
push the control off the screen.

Idle carries no number, so its whole line sets at Body 20 sp inside the same 128 dp
block. The jump to 34 sp is therefore itself the signal: **the type gets big exactly
when the phone is listening**, which is legible across a room without reading a word.

`Cancel` is distinguished by **weight alone** — 400 against the other three actions'
600. Not colour, not a box, not red. It reads as a lighter thing on the page, which
is what it is.

---

## 3. Colour, paired and measured

Read back against the `DIRECTION.md` §6a table; the independent recompute agreed
within OKLCH→hex rounding.

### Light — "the page"

| Pair | Hexes | Ratio | Where |
|---|---|---|---|
| `ink` on `paper` | `#191210` on `#FDF3EF` | **16.91:1** | status line, contents line, all three action words, `Cancel` |
| `ink-quiet` on `paper` | `#5A504D` on `#FDF3EF` | **7.14:1** | running head only |
| `ink` on `paper-sunk` | `#191210` on `#F2E7E3` | **15.23:1** | control label at rest; permission notice |
| `paper` on `ink` (focus reversal) | `#FDF3EF` on `#191210` | **16.91:1** | the focused row, whatever it is |
| `mark` on `paper` | `#A12721` on `#FDF3EF` | **6.77:1** — graphical only | 4 dp focus rule in the leading margin |
| `mark` on `paper-sunk` | `#A12721` on `#F2E7E3` | **6.12:1** — graphical only | 4 dp recording rule on the control's leading edge |
| `rule` on `paper` | `#ACA29F` on `#FDF3EF` | 2.28:1 — **decorative only** | the two hairlines, and nothing else |

### Dark — "the reversed edition"

`board #110C0A` substrate · `board-raised #1D1714` control fill and notice block ·
`light #F0E9E7` body at **16.26:1** on board, **14.86:1** on board-raised ·
`light-quiet #9F9693` running head at **6.72:1** · `mark-d #C86459` at **5.03:1**,
graphical only · `rule-d #524B49` hairlines, decorative · focus reverses to `light`
ground with `board` text at **16.26:1**.

### Three colour decisions this screen forced

1. **The control cannot be a filled `ink` slab.** Reversal is the focus device on
   every surface in this concept; a permanently reversed button would read as
   permanently focused. So the control rests at `paper-sunk` fill with `ink` label
   (15.23:1) and a **2 dp `ink` border** — the `rule` hairline at 2.28:1 is
   explicitly disqualified as the sole boundary of a control, so the control's edge
   is drawn in ink at 16.91:1 against the page.
2. **Focus, on the control and on every action word:** the row reverses to `ink`
   ground / `paper` text, and the **4 dp `mark` rule sits at x 8–12 dp — outside the
   reversed block**, which starts at x=24. `mark` on `ink` measures **2.50:1** and
   fails both bars, so it is never allowed inside the reversal. 12 dp of clear page
   between rule and block.
3. **Recording is signalled without colour doing the work alone.** While recording,
   the control gains a 4 dp `mark` rule down its full leading edge (6.12:1 on
   `paper-sunk`, graphical) **and** its label changes to `Stop recording` **and** the
   status line jumps to 34 sp. Three channels; colour is the least of them.

Nothing on this surface is a gradient, and no accent carries text anywhere.

---

## 4. The four bands

| Band | Height | Treatment |
|---|---|---|
| 1 · Status | 24 dp min, real inset | **Reserved.** `paper` runs beneath; no text, no target, nothing painted |
| 2 · Running head | 56 dp fixed | Rendered. Date deliberately omitted — a record dates its pages, not the form you speak into. Hairline under it |
| 3 · Content | flex | The status block, the open field, the control, the action words |
| 4 · Bottom | 48 dp | **Reserved**, empty by design: 24 dp gesture handle + 24 dp keep-out. No bottom nav on a task leaf. Guarantees the 96 dp control is never within a swipe-up's reach |

Nothing bleeds under any band.

---

## 5. Content direction

Four states of one screen, every string verbatim from `RecordCopy.kt` and
`RecordScreen.kt`, nothing invented:

- **Idle** — `Ready. Press Start recording when you want to speak your memo.` (reply
  variant swaps *memo* for *reply*). Control: `Start recording`. `Cancel` beneath.
- **Recording** — `Recording. 13 seconds so far.` at 34 sp/`tnum`, and past 90 s the
  shipped append ` This is getting long — short memos are easier to listen to.` at
  20 sp. Control: `Stop recording`. No other action on screen.
- **Finished** — `Recorded 24 seconds.` at 34 sp/`tnum`, then
  `Listen back, send it, or record it again.` at 20 sp; then the words `Listen back`,
  `Post memo` / `Send reply`, `Record again`, and after a 24 dp gap, `Cancel`.
- **Permission denied** — designed, and it lives only here, because this is the only
  place the microphone is asked for. The status block becomes a printed notice on
  `paper-sunk`, full measure, 16 dp inner padding:
  `TEW needs permission to use the microphone before you can record.` The 96 dp slot
  reads `Allow microphone`. `Cancel` beneath it.
- **Failure** — `Your recording is still here — try sending again.` printed into the
  status block, appended to `TewResult.Failure.spoken`, `assertive`. **The Finished
  action words stay on screen and the recording is never cleared by a failure.**

The only number on this screen is a duration. No plays, no listeners, no replies, no
position, no tally of any kind.

The one label not drawn from a shipped string is the contents line's `Contents`,
which comes from `DIRECTION.md` §7's own vocabulary. One structural word, no prose
invented.

---

## 6. Traversal — D-pad and switch access

Linear, five stops maximum, no traps.

1. **Running head** — `heading()`, and the focus target on route change (decision 13)
2. **Contents line** — first focusable after the head, on every surface
3. **Status block** — focusable text, so the duration can be re-read on demand
4. **The 96 dp control** — `Start recording` / `Stop recording` / `Allow microphone`
5. *(Finished only)* `Listen back` → `Post memo` → `Record again`
6. **`Cancel`** — **always last, never the default focus, never reached before every
   non-destructive action has been passed**

**Switch access:** in the Recording state there are exactly **four** scan stops, so
`Stop recording` is at most four steps away for a switch user mid-memo — the single
most time-critical target on the screen. This is the reason nothing decorative is
made focusable here. Custom actions (`Listen back`, `Send`, `Record again`,
`Cancel and go back`) remain on the screen container as shipped, plus "Go to…".

### One finding for the builder — the live region cadence

`RecordViewModel` rewrites `announcement` every `TICK_MS` and `RecordScreen` marks
that same text `LiveRegionMode.Assertive`. As laid out, TalkBack interrupts itself
once a second for the whole memo. **The printed 34 sp line updates every second; the
assertive region must announce only on transitions** — start, the 90-second nudge
once, stop, and failure. The strings do not change; the cadence does. The duration
stays available on demand at stop 3. This screen keeps the only assertive region in
the app and that is exactly why it has to be quiet between events.

---

## 7. Reduced motion

**This surface has no animation at all.** The one motion in the whole direction — the
entry rule drawing left-to-right, 180 ms — belongs to list surfaces and there is no
entry rule here. The counting digits are a text swap, not a transition: **no
odometer roll, no crossfade, no pulse on the control, no waveform.** Under
`ANIMATOR_DURATION_SCALE == 0` this screen is byte-identical to its normal state,
which is a property of the design rather than an override.

---

## 8. Logged, verbatim

- **Composition anchor:** `bottom-anchored`
- **Background mode:** `flat-surface`
