# record: Surface 6 of 6 — 06-sign-in

**Mode:** coded comp. No image was generated — this environment has no image MCP
server loaded, so image mode was unavailable. Everything below is stated as numbers
that a render would have approximated.

**Composition anchor:** `top-left-lead`
**Background mode:** `flat-surface`

**Frame:** one phone, **390 × 844 dp**, portrait, Android-native, minSdk 26. All
lengths `dp`, all type `sp`, and **no height anywhere is computed by adding an `sp`
value to a `dp` value** (decision 20 — `4sp + 20sp ≠ 24sp` under Android 14's
non-linear curve). Where a target must clear a floor it is `max(48.dp, lineBox)`,
resolved at layout, never by arithmetic in this spec.

---

## 1. The structural claim — the head band is reserved and empty

**Every other surface in `record` carries a fixed 56 dp running head with the date
and the section name, hairline under it. This one carries none, and it is the only
one.** There is no record yet to head. The band is **held at its full 56 dp and
painted in `paper` with nothing in it** — not collapsed, not repurposed, not
floated over.

> **Builder note, load-bearing:** a running head added to this screen deletes the
> idea. There is no date, no section name, no step counter, no hairline in this
> band. If a shared `RecordScaffold` supplies the head, this screen passes it
> `null` and keeps the 56 dp spacer.

The emptiness is also the composition. The block sits **at the top of the content
band**, immediately under the empty head, and roughly **441 dp of unbroken cream
falls beneath it** — 62% of the content band, unfilled. That inversion is what
separates this surface from **01-onboarding**, its closest sibling: onboarding puts
one large idea in the middle of an empty field, under a running head that carries a
step position, with a two-word action row. This one has no head at all, one action,
and its empty field is *below* the block rather than around it — the page the record
has not started on. It is also the exact opposite of **04-record**, whose big
control sits low.

---

## 2. Layout, with numbers

Column: **x = 24 dp, width = 342 dp** (390 − 2 × 24 screen padding). One column.
Nothing side-by-side anywhere. Every line **left-aligned, ragged-right, no
hyphenation, no justification**. The wordmark is **not centred** — RNIB permits a
heading to centre, and this comp declines, so the whole block shares one left edge
and the ragged right is the only soft edge on the screen.

| y (dp, from top) | Height | Element |
|---|---|---|
| 0 | **24** | **Band 1 — status.** Reserved, `paper`, no content, no scrim |
| 24 | **56** | **Band 2 — running head. RESERVED AND EMPTY.** `paper`, no text, no hairline |
| 80 | **716** | **Band 3 — content.** Block below; rest is bare `paper` |
| 796 | **48** | **Band 4 — bottom.** Reserved for gesture bar / 3-button nav. No bottom nav on this surface, nothing painted |

Inside the content band, top-anchored:

| From band top | Height | Element |
|---|---|---|
| 0 | 24 | gap (`space300`) |
| 24 | ~43 | `Third Eye World` — Display 34 sp / 600 / 1.25, one line at 100% |
| 67 | 16 | gap (`space200`) |
| 83 | ~58 | positioning line — Body 20 sp / 400 / 1.45, **wraps to 2 lines** at 342 dp |
| 141 | 32 | gap |
| 173 | **1** | hairline rule, full 342 dp column |
| 174 | 16 | gap (`space200`) |
| 190 | **49** | `Sign in with Google` — action line, target = `max(48.dp, lineBox + 20.dp)` |
| 239 | 12 | gap (`space150`, the admitted nested unit) |
| 251 | ~24 | status line — 17 sp / 400 / 1.40 |
| ~275 | **441** | **empty `paper`. Not to be filled.** |

The hairline sits **above** the action and belongs to it — 32 dp of air above it,
16 dp below. It is a rule in the record's sense, **not an underline**: no glyph on
this screen is underlined, ever (RNIB, hard rule).

**200% text scale:** block grows to ~571 dp, still inside the 716 dp band, nothing
truncates, nothing shrinks, nothing moves sideways. The wordmark breaks to two
lines, the positioning line to three, the action target to ~78 dp. The content band
is a `verticalScroll` container anyway so that beyond ~250% it scrolls rather than
clips. The four bands never scroll.

---

## 3. Type table

Atkinson Hyperlegible Next, variable, **roman only** — the italic VF is not in the
APK. Weights in the dark column are the same roles at −50 on the `wght` axis.

| Element | Role | Size | Weight L / D | Line-height | Notes |
|---|---|---|---|---|---|
| `Third Eye World` | Display | **34 sp** | 600 / 550 | 1.25 | `heading()` — see §6 |
| `Short voice memos, from people who get it.` | Body | **20 sp** | 400 / 350 | 1.45 | the 20 sp floor; ragged-right in a left-aligned column |
| `Sign in with Google` | Action line | **20 sp** | 600 / 550 | 1.45 | weight carries it; **never underline** |
| status line | Time / secondary | **17 sp** | 400 / 350 | 1.40 | `tnum` on, though no figures occur in the three strings |
| — | Running head 15 sp | — | — | — | **absent by design on this surface only** |

No all-caps, no small caps, no italics, no letter-spacing tricks. Sentence case
throughout.

---

## 4. Paired colours, measured

Light — "the page":

| Pair | Hexes | Ratio | Where |
|---|---|---|---|
| `ink` on `paper` | `#191210` on `#FDF3EF` | **16.91:1** | wordmark, positioning line, action line |
| `ink-quiet` on `paper` | `#5A504D` on `#FDF3EF` | **7.14:1** | status line (17 sp, so the 4.5:1 bar applies and is cleared with room) |
| `rule` on `paper` | `#ACA29F` on `#FDF3EF` | 2.28:1 | the 1 dp hairline — **decorative only**, carries no meaning and is not the boundary of any control |
| focus, reversed | `#FDF3EF` text on `#191210` ground | **16.91:1** | the focused line |
| `mark` on `paper` | `#A12721` on `#FDF3EF` | **6.77:1** | the 4 dp focus rule — **graphical only, never text** |

Dark — "the reversed edition", a real published object, not an inversion filter:

| Pair | Hexes | Ratio | Where |
|---|---|---|---|
| `light` on `board` | `#F0E9E7` on `#110C0A` | **16.26:1** | wordmark, positioning line, action line |
| `light-quiet` on `board` | `#9F9693` on `#110C0A` | **6.72:1** | status line |
| `rule-d` on `board` | `#524B49` on `#110C0A` | 2.29:1 | hairline, decorative only |
| focus, reversed | `#110C0A` text on `#F0E9E7` ground | **16.26:1** | the focused line |
| `mark-d` on `board` | `#C86459` on `#110C0A` | **5.03:1** | the 4 dp focus rule |

`paper-sunk` / `board-raised` are **not used on this surface** — there is no printed
notice here. Stated so the absence is not read as an omission.

**Focus, exactly:** the focused line **reverses** — the ink ground fills the full
342 dp column at the line's height, its text set in `paper`. The **4 dp `mark` rule
sits in the leading margin at x = 12–16 dp, 8 dp clear of the reversed block,
outside it**, running the block's height. It is never inside the reversed block,
because `mark` on `ink` measures **2.50:1** and fails both bars. The hairline cannot
substitute for any of this at 2.28:1, which is why focus is a reversal rather than
a ring.

---

## 5. Content direction

Four strings, all owned, none invented, nothing added:

- `Third Eye World`
- `Short voice memos, from people who get it.`
- `Sign in with Google`
- status line, exactly one visible at a time:
  `You are not signed in yet.` / `Signing you in. This can take a few seconds.` /
  `Signed in as ruthm.`

**There is no logo and no mark of any kind.** The product owns no visual asset, and
designing around that absence is the answer — the wordmark is set type at Display,
not an image. No tagline, no subhead, no feature list, no illustration, no
device art, no waveform, no superlative, no placeholder prose.

The positioning line breaks as `Short voice memos, from people` / `who get it.` at
100% scale in a 342 dp measure. Ragged-right, unhyphenated.

---

## 6. Traversal order and semantics

There is no running head, so the concept's route-change contract — *focus moves to
the new screen's running head* — has no target here. **This is the one surface where
the fallback fires: initial focus lands on `Third Eye World`, the screen's single
top-level heading.**

1. **`Third Eye World`** — `Modifier.semantics { heading() }`. **This is the
   accessibility fix.** `SignInScreen.kt` currently ships it as a `headlineMedium`
   with no `heading()` — the only headline in the app missing it. This comp
   specifies it as a heading; that is not optional dressing, it is the screen's only
   landmark.
2. **positioning line** — plain text node, no role.
3. hairline — **not focusable, no semantics, no `contentDescription`**.
4. **`Sign in with Google`** — `Role.Button`, the only actionable element on the
   screen. Target `max(48.dp, lineBox + 20.dp)` tall by the full 342 dp column wide;
   the tappable bounds extend past the visible glyph box in `dp`, and they only grow
   with text scale. Stays enabled while signing in — re-tapping re-launches the same
   federated intent, which is harmless, and disabling it would drop it out of some
   traversal paths.
5. **status line** — plain text, `liveRegion = Polite` (advisory class: it never
   interrupts, and nothing here is imperative).

**Authentication — WCAG 3.3.8 conforms by absence.** Federated one-tap. There is no
password field, no OTP, no puzzle, no memorisation, no transcription, **no text
input of any kind on the path**, so no cognitive function test exists to except.
Paste-blocking and password-manager-blocking cannot occur because there is no field
to block them in. **3.3.9 (AAA) also conforms** — no object-recognition and no
personal-content step is in play either. Reopens only if a password is ever added.
2.5.8 is met by the 48 dp floor above.

Voice: the shipped command set is unchanged. There is no contents line on this
surface, because there are no destinations yet.

---

## 7. Reduced motion

**This surface is byte-identical under `ANIMATOR_DURATION_SCALE == 0.** The one
motion the direction adds — the entry rule drawing left-to-right, 180 ms — belongs
to entry rows, and there are no entry rows here. The hairline is drawn at full
length always. The signing-in state is a **printed line, not a spinner**: no
indeterminate progress animation, no pulse, no skeleton shimmer. Nothing on this
screen moves in any setting.

No toast, anywhere. The status line is a printed line that stays until the next
action displaces it — never timer-dismissed.

---

## 8. The nine data states

| State | This surface |
|---|---|
| **Error** | **Designed.** Status line becomes `Sign-in did not complete.` in `ink-quiet`, printed in place, `polite`. The action line stays where it is, same words, same position — the user retries the thing they already found. No dialog, no toast |
| **Offline** | **Designed.** Status line carries the shipped `TewResult.Failure.spoken` verbatim: *"You seem to be offline. Check your connection and try again."* It wraps to 2–3 lines and the block grows downward into the empty field. Nothing below it moves, because nothing is below it |
| **Empty** | N/A — nothing is listed on this surface |
| **Loading** | N/A as a *data* state: this screen fetches nothing. `Signing you in. This can take a few seconds.` is the single control's transaction status, not a screen load, and it is already specified as one of the three status strings |
| **Partial** | N/A — there is no multi-source content to partly arrive |
| **Permission denied** | N/A — no runtime permission is requested on this path; the microphone permission belongs to 04-record |
| **Stale** | N/A — nothing is fetched, so nothing can age. This is also why there is no running head to carry a fetch time |
| **Conflict** | N/A — nothing here can be removed underneath the user |
| **Bulk** | N/A — one action, one tap |

---

## 9. What I could not satisfy

- **Google's Sign-In branding guidelines want the `G` mark on the button.** This
  direction states *"the app has zero icons and this direction adds none — every
  control in it is a word"* (decision 10), and there is no icon language in the
  type-only system to draw one from. The comp ships the words alone. This is a real,
  unresolved collision between a third-party brand requirement and an owned
  non-negotiable, and it needs a decision above my level — either an exception for
  the one asset Google requires, or a documented deviation from their guidelines.
- **No image was rendered.** Coded comp only; no image tool was available in this
  environment.

## 10. Numbers read back against the palette table

Checked line by line against §6a of `DIRECTION.md`: `paper` `#FDF3EF` 16.91:1 with
`ink` `#191210` ✓ · `ink-quiet` `#5A504D` 7.14:1 ✓ · `rule` `#ACA29F` 2.28:1
decorative ✓ · `mark` `#A12721` 6.77:1 graphical-only, and 2.50:1 on `ink` which is
why it stays in the margin ✓ · `board` `#110C0A` / `light` `#F0E9E7` 16.26:1 ✓ ·
`light-quiet` `#9F9693` 6.72:1 ✓ · `rule-d` `#524B49` 2.29:1 ✓ · `mark-d` `#C86459`
5.03:1 ✓. No hex on this surface is absent from the table, and no ratio was
estimated.
