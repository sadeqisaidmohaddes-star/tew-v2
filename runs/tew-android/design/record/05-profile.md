# record: Surface 5 of 6 — 05-profile

Coded comp. One surface, one concept. Android-native, minSdk 26.
Frame: **one phone, 390 × 844 dp, light mode primary; dark deltas stated inline.**

Composition anchor: `dense-grid` · Background mode: `flat-surface`

---

## 0. The argument this surface has to win

A record prints its corrections. So moderation status here is **not a badge on a
card** — it is a line of ink set in the entry, in the same face and the same 20 sp
as the transcript above it, with the second ink appearing only as a **rule in the
leading margin**. Nothing about a withheld entry is signalled by colour, by an
icon, or by a pill. The word carries it; the rule marks the margin so a sighted
low-vision reader can find the corrected entries by scanning the edge of the
column, exactly the way a proof-corrected page is scanned.

That is the whole direction, and it either reads on this screen or it does not.

**Style under density.** DIRECTION.md's line — *"At forty rows this is a page of
the record"* — is carried here, with a correction printed rather than hidden:
02-radio's entries run ~5 per screen at default scaling and ~2.5 at the OS 200%
setting. **A profile entry is taller than a radio entry**, because the status is
printed in it and a removed entry prints its reason as well. Measured against the
block below: **~3 entries per screen at default, ~1.5 at 200%.** Forty rows is
thirteen screens, not eight. It still holds, and it holds **by scrolling, not by
density** — the record's own device, the **sticky date rule**, is what keeps the
reader located while they scroll. No table, no collapse, no truncation.

---

## 1. The four bands

| Band | Height | Treatment |
|---|---|---|
| **Status** | `WindowInsets.statusBars`, 24 dp nominal | Painted `paper`. Content never drawn under it. No translucency, no blur — budget hardware, and translucency costs contrast |
| **Title / nav — the running head** | **56 dp fixed, reserved, not floating** | `Your record`, 15 sp / 500, `ink-quiet`, sentence case, left at 24 dp. 1 dp `rule` hairline under it, full bleed. Does not scroll away, does not shrink, does not collapse |
| **Content** | **740 dp** viewport on this frame | `LazyColumn`, 24 dp screen padding, sticky date rules |
| **Bottom** | `WindowInsets.navigationBars` — 24 dp gesture / 48 dp 3-button | **Reserved, empty, and that is the design.** There is no bottom nav bar in this direction: the five-item bar became the contents line under the running head (DIRECTION §7). `Sign out` is the **last item in the scroll**, not a fixed bar |

Bottom padding on the list = navigation inset **+ 24 dp**, so `Sign out` can clear
the gesture bar when scrolled to the foot.

---

## 2. Layout move, with numbers

390 dp frame · 24 dp screen padding both sides · **text column = 342 dp**.

**The leading margin is real and it is the point.** The status rule sits at
`x = 8 dp`, is **4 dp wide**, and runs the full height of the entry's text block.
Between rule and text: 12 dp. Text column starts at 24 dp and is never indented to
make room — a Posted entry and a Taken-down entry set on **exactly the same left
edge**, so the margin rule is the only horizontal difference and the column stays
straight.

```
 0    8   12   24                                        366  390
 |    |███|<-->|  transcript / time / status / actions    |     |
      4dp  12dp            342 dp column
      rule  gap
```

### Vertical block — one entry

| Part | Value |
|---|---|
| Entry hairline (1 dp, `rule`, full bleed 0→390) | 1 dp |
| gap | 16 dp |
| Transcript, 20 sp / 400 / 1.45 → 29 sp line box | 2 lines = 58 |
| gap | 8 dp |
| Time line, 17 sp / 400 / 1.40 `tnum` → 24 | 24 |
| gap | 8 dp |
| **Status line, 20 sp / 400 / 1.45, `ink`** | 1–4 lines (29 / 58 / 87 / 116) |
| gap | 12 dp |
| Action line(s), 20 sp / 600 / 1.45, each a 49 dp row | 49 each |
| gap to next hairline | 24 dp |

Worked total, Posted entry with one action: 1 + 16 + 58 + 8 + 24 + 8 + 29 + 12 +
49 + 24 = **229 dp**. 740 / 229 = **3.2 entries per screen**. A Taken-down entry
with its reason (4 status lines) and two actions = **365 dp**.

**Sticky date rule** — 48 dp band + 1 dp hairline under.
`Sunday 17 August`, 20 sp / 600, `ink`, sentence case, 12 dp above / 8 dp below
the line box. Ground is **opaque `paper`** — a sticky header that lets entries
show through it is unreadable at 200%. `heading()`, not a control, no chevron,
no count on it.

**Actions stack, one per line, always.** `Ask for another look` then `Delete`,
never side by side. At 20 sp / 600 the two labels fit one row at default scaling
and reflow at 130%; a layout that reflows at 130% is a layout the audience will
never see in its designed state. Each action row spans the **full 342 dp column**
as its touch target — a wide target is easier to hit without looking than a
tight-wrapped one.

**48 dp is met past the visible bounds.** 20 sp × 1.45 = 29 dp line box + 10 dp
padding top and bottom = 49 dp at font scale 1.0. At Android's 0.85 scale that
falls to 44.7 dp, so every action row also carries
`defaultMinSize(minHeight = 48.dp)`. Padding is `dp`; type is `sp`; **the two are
never summed to prove a target** — the min-height does that.

---

## 3. Type table

Atkinson Hyperlegible Next, variable, roman only, bundled `res/font/`. Roles used
on this surface:

| Role | Size | Weight light / dark | Line-height | Where |
|---|---|---|---|---|
| Speaker line, 24 sp | — | — | — | **Not used here.** Every entry is yours; printing `From you` forty times is noise |
| Body / transcript | 20 sp | 400 / 350 | 1.45 | Transcript; **status line**; dialog body |
| Action line | 20 sp | 600 / 550 | 1.45 | `Ask for another look`, `Delete`, `Sign out`, contents line, **sticky date rule** |
| Time / secondary | 17 sp | 400 / 350 | 1.40 `tnum` | Time line, inventory line |
| Running head | 15 sp | 500 / 450 | 1.35 | `Your record` |
| Display 34 sp | — | — | — | Not used here |

Dialog title `Delete this memo?` sets at **24 sp / 600 / 1.30** — the speaker-line
role, borrowed because a dialog title is a heading and this is the only heading
slot on the surface that is not the running head or a date rule.

Hard rules held: ragged-right everywhere, **no hyphenation**, no italics, **no
underline** (actions carry 600 weight and a hairline *above* the action block),
no all-caps and no small caps — including on the date rules, which a real record
would set in caps.

**Measure, stated not resolved.** 342 dp at 20 sp ≈ **30–38 characters**, below
the 45–75 working range. Large print and a comfortable measure are in direct
conflict on a 390 dp phone. Mitigations only: short lines, ragged-right, no
hyphenation.

---

## 4. Colour, paired and measured

Read back against the palette table; every ratio below is transcribed from it, not
re-derived.

### Light — "the page"

| Element | Foreground | Ground | Ratio | Bar |
|---|---|---|---|---|
| Transcript | `ink` `#191210` | `paper` `#FDF3EF` | **16.91:1** | 4.5 ✓ |
| **Status line** | `ink` `#191210` | `paper` `#FDF3EF` | **16.91:1** | 4.5 ✓ |
| Time line, inventory line | `ink-quiet` `#5A504D` | `paper` | **7.14:1** | 4.5 ✓ |
| Running head | `ink-quiet` `#5A504D` | `paper` | **7.14:1** | 4.5 ✓ |
| Action lines, date rules | `ink` `#191210` | `paper` | **16.91:1** | 4.5 ✓ |
| **Margin status rule, 4 dp** | `mark` `#A12721` | `paper` | **6.77:1** | 3.0 ✓ graphical |
| Entry hairline, 1 dp | `rule` `#ACA29F` | `paper` | 2.28:1 | **decorative only** |
| Conflict notice block | `ink` `#191210` | `paper-sunk` `#F2E7E3` | **15.23:1** | 4.5 ✓ |
| Dialog surface | `ink` `#191210` | `paper-sunk` `#F2E7E3` | **15.23:1** | 4.5 ✓ |
| **Focused line, reversed** | `paper` `#FDF3EF` | `ink` `#191210` | **16.91:1** | 4.5 ✓ |

`mark` never sets a character of text on this surface. `mark` on `ink` is
**2.50:1** and fails both bars, which is why the 4 dp status rule stays in the
leading margin **outside** the reversed focus block and never inside it. Focus and
status therefore coexist on one entry without either destroying the other.

### Dark — "the reversed edition"

Same structure, every weight **−50** on the `wght` axis.

| Element | Foreground | Ground | Ratio |
|---|---|---|---|
| Transcript, status line, actions, date rules | `light` `#F0E9E7` | `board` `#110C0A` | **16.26:1** |
| Time line, running head | `light-quiet` `#9F9693` | `board` | **6.72:1** |
| Margin status rule, 4 dp | `mark-d` `#C86459` | `board` | **5.03:1** (3.0 ✓) |
| Margin rule where it crosses the notice | `mark-d` `#C86459` | `board-raised` `#1D1714` | **4.60:1** (3.0 ✓) |
| Conflict notice, dialog surface | `light` `#F0E9E7` | `board-raised` `#1D1714` | **14.86:1** |
| Entry hairline | `rule-d` `#524B49` | `board` | 2.29:1 decorative |
| Focused line, reversed | `board` `#110C0A` | `light` `#F0E9E7` | **16.26:1** |

**`board-raised` must never be lightened** — `mark-d` on it is 4.60:1 with 0.10 of
headroom.

Dialog scrim: `ink` / `board` at 55% alpha. Graphical dim, no text on it.

---

## 5. The status vocabulary, rendered

Never colour alone. Every state is **a word and a shape**; two states have no
shape at all, which is correct — an entry that is fine is not marked.

| Status | Printed word (shipped verbatim) | Margin rule | Colour |
|---|---|---|---|
| Visible | `Posted. Anyone can hear this.` | none | — |
| Under review | `Being checked. Someone reported this memo and a moderator is looking at it. It is hidden until they decide.` | 4 dp, **dashed** — 8 dp on, 6 dp off, square caps | `mark` |
| Removed | `Taken down. ` + reason + appeal state | 4 dp, **solid** | `mark` |
| Appeal upheld | `Your appeal succeeded and this memo is back.` | none | — |

Appeal sub-states, appended to the Removed line from shipped `appealStatus`:
`You can ask for this to be looked at again.` /
`You have asked for this to be looked at again. Waiting on a decision.` /
`Your appeal was considered and refused.`

The `Ask for another look` action is **present only when `canAppeal(memo)`** —
removed *and* appeal available. It is not greyed out when unavailable; it is
absent. A disabled control is a thing TalkBack still lands on and reads.

---

## 6. Content direction — the comp as it renders

Running head band, then, down the column:

```
┌ status bar, 24 dp, paper ────────────────────────────┐
├ Your record                    15 sp / 500 ink-quiet ┤  56 dp
├──────────────────────────── 1 dp rule ───────────────┤
   Contents: Your record             20 sp / 600 ink      ← first focusable, 49 dp
   You have posted 3 memos.          17 sp ink-quiet      ← polite live region
├ Sunday 17 August ──────────────── 20 sp / 600 ink ───┤  48 dp sticky, opaque paper
├──────────────────────────── 1 dp rule ───────────────┤
                                                          ── ENTRY 1, Posted
     Testing this out. Hello from my kitchen.             20 sp ink
     8:14 pm · 3 seconds                                  17 sp ink-quiet tnum
     Posted. Anyone can hear this.                        20 sp ink   ← no margin rule
     Delete                                               20 sp / 600, 49 dp
├──────────────────────────── 1 dp rule ───────────────┤
█                                                         ── ENTRY 2, Under review
█    Asking whether anyone else has trouble with the      ← DASHED 4 dp mark rule
█    new bus stop announcements.                             in the leading margin
█    2:07 pm · 9 seconds
█    Being checked. Someone reported this memo and a
█    moderator is looking at it. It is hidden until
█    they decide.
█    Delete
├ Saturday 16 August ───────────── 20 sp / 600 ink ────┤  48 dp sticky
├──────────────────────────── 1 dp rule ───────────────┤
█                                                         ── ENTRY 3, Taken down
█    This memo was taken down, so the appeal screen       ← SOLID 4 dp mark rule
█    has something to open.
█    1:20 pm · 5 seconds
█    Taken down. A listener reported this memo for
█    harassment, and a moderator agreed it broke the
█    community rules. If you think that was wrong,
█    you can ask for another look. You can ask for
█    this to be looked at again.
█    Ask for another look                                 49 dp
█    Delete                                               49 dp
├──────────────────────────── 1 dp rule ───────────────┤
   Sign out                             20 sp / 600 ink   49 dp, 24 dp above
└ nav inset reserved, 24 dp, empty ────────────────────┘
```

**Copy provenance, stated so nothing is taken on trust.**
Shipped verbatim: `Posted. Anyone can hear this.` · the whole `Being checked…`
string · `Taken down.` + reason + `You can ask for this to be looked at again.` ·
`Ask for another look` · `Delete` · `Sign out` · `You have posted 3 memos.` ·
Entry 3's transcript and reason (seed `own2`). Entry 1's transcript (seed `own1`).
**New on this surface, needs sign-off:** the contents-line label
`Contents: Your record`, and the conflict string in §8. Entry 2's transcript is a
**comp-only placeholder** — the product ships no under-review sample — written to
a plausible length from the product's own domain, not to be shipped.

**No counts anywhere except inventory.** No plays, no likes received, no
listeners. `You have posted 3 memos.` is arithmetic about your own recordings,
not engagement machinery, and it is the shipped string.

---

## 7. Traversal, semantics, focus

`collectionInfo` **IS set on this list** (`rowCount = 40`), so TalkBack speaks
*"item 3 of 40"*. Deliberately off on the feed and the deck; on here, because
knowing how many of your own recordings exist is inventory.

Each entry's **text block is one merged node** — shipped `contentDescription`
pattern: `"Your memo. " + transcript + " " + ownMemoStatus(memo)`. Left unmerged
it is four swipes per entry and 160 swipes for the page. The action lines sit
**outside** the merge so they stay separately focusable.

Traversal order:

1. `Your record` — running head, `heading()`. **Focus target on route change.**
2. `Contents: Your record` — first focusable after the running head, every screen
3. `You have posted 3 memos.` — `polite`
4. `Sunday 17 August` — `heading()`
5. Entry 1 merged node → *"Your memo. Testing this out… Posted. Anyone can hear this. Item 1 of 40."*
6. `Delete`
7. Entry 2 merged node → *"…Being checked…"*
8. `Delete`
9. `Saturday 16 August` — `heading()`
10. Entry 3 merged node → *"…Taken down…"*
11. `Ask for another look`
12. `Delete`
13. `Sign out`

**Focus after removal.** A deleted entry hands focus to the **next entry**; to the
**previous** if it was the last; to the **running head** if the record is now
empty. Never to the screen top, never to the scroll container.

The sticky date rule is a heading, not a control: it takes a traversal stop and
gives no action. A blind reader scrolling with headings-navigation moves
day-by-day through their own record, which is the single fastest route through
forty rows and costs nothing to build.

---

## 8. The two states that destroy work

### Delete confirmation — shipped copy, untouched

| Slot | Words |
|---|---|
| Title | `Delete this memo?` |
| Body | `The recording will be destroyed. It cannot be brought back, and anyone who has not heard it never will.` |
| Confirm | `Delete` |
| Dismiss | `Keep it` |

Set on `paper-sunk` `#F2E7E3` / `ink` — 15.23:1. Title 24 sp / 600 with
`heading()`; body 20 sp / 400; the two buttons **stacked, one per line**, 49 dp
each, `Delete` first, `Keep it` last. 24 dp dialog padding.

**Initial focus lands on the title, not the first control** — the question is
spoken before the options are reachable, so nobody acts on `Delete` before hearing
what it does. Traversal: title → body → `Delete` → `Keep it`. Back dismisses
(= `Keep it`). Focus returns to the `Delete` line that opened it after `Keep it`,
and to the **next entry** after `Delete`.

### Conflict — the one that is not handled anywhere today

Spec it, do not defer it. The user is on the appeal of an entry that a moderator
has just removed outright.

> `This memo was removed while you were here. Your appeal was not sent.`

**Assertive** live region — the second and last assertive region in the app, after
recording. Printed as a notice block: `paper-sunk` ground, full 342 dp column,
16 dp inset padding, 20 sp `ink` (15.23:1), replacing the `Ask for another look`
line in place. The entry's **solid** margin rule continues through the notice
uninterrupted — it is still the same entry, still removed, and the correction is
printed inside the entry rather than thrown as a toast.

**Focus:** if focus was on `Ask for another look` when it disappeared, it moves to
that entry's merged node — the entry that changed — and the assertive region
speaks. Not to the screen top. Not lost.

Both states render in this comp; the human picks whether the notice replaces the
appeal action in place (specified above) or the entry re-renders whole.

---

## 9. Motion, and reduced motion

The direction has exactly one motion: the entry rule drawing left-to-right,
180 ms, as the record advances. On this surface that is the hairline of a newly
loaded page of entries.

Under `ANIMATOR_DURATION_SCALE == 0` the reduced-motion state is **the same frame
with the rule already drawn at full length** — a composed still, not
`animation: none`. Sticky date rules do not animate in either mode; sticking is
position, not motion. Deleted rows are gone on the next frame with no fade and no
item-placement animation. Focus movement is identical in both modes.

---

## 10. What this hands the implementer

Nothing aesthetic left open. Settled: 390 dp frame, 24 dp padding, 342 dp column,
4 dp margin rule at x = 8, 56 dp running head, 48 dp sticky date rules, the entry
block's dp stack, 49 dp action rows with `defaultMinSize(48.dp)`, the role-indexed
`sp` scale, every colour pair with its measured ratio, `collectionInfo = true`,
the merged-node boundary, the traversal list, the three focus-after-removal cases,
and the two live-region states.

Open, and needing a human: two new strings (§6), and the conflict re-render choice
(§8).
