# CURRENT — tew-android

What is on the screen today. Measured from the source and from a real device
recording, not remembered.

**How this was gathered, and what that costs.** `redesign-scout` was not
dispatched — see `SKIPS.md`. The extraction below is from two sources instead:
the Compose source at `dev@caf3cca`, and 23 frames taken at 20-second intervals
from the 2026-08-16 device session (`video_2026-08-14_15-55-00.mp4`, 7m38s, real
hardware, TalkBack on for most of it). The cost of skipping the scout is that
nobody re-rendered the surface to check states the video never reached — the
nine data states are unmeasured, and empty, loading and error appearances are
inferred from code rather than seen.

## The extracted system

There is no system. That is the finding, and it is unusually clean.

| | Measured | Source |
|---|---|---|
| Theme wrapper | **None.** `setContent { TewApp(container) }` | `MainActivity.kt:19` |
| XML theme | `android:Theme.Material.Light.NoActionBar` — the platform theme, not an app theme | `app/src/main/res/values/themes.xml` |
| Color scheme | Compose baseline `lightColorScheme()` by fallback — primary `#6750A4` | absence of a `MaterialTheme` wrapper |
| Typography | Compose baseline — Roboto, default scale | same |
| Shape | Compose baseline — 12dp cards, 20dp full-width buttons | same |
| Spacing | 16dp screen padding, 8–12dp between items, ad hoc per screen | each screen file |
| Iconography | **None.** No icons anywhere in the app | grep: no `Icons.` import in any module |
| Imagery | **None.** No drawables beyond the launcher icon | `app/src/main/res` |
| Dark mode | **Not implemented.** Light only, no `isSystemInDarkTheme` branch | grep across all modules |
| Motion | **None.** No `animate*`, no transitions | grep across all modules |

Every `MaterialTheme.typography.*` and `MaterialTheme.colorScheme.*` call across
the six modules is therefore reading a default nobody chose. There is nothing to
correct — only something to replace.

## What the device frames show

Confirms the code rather than contradicting it. Across all nine screens reached:
white ground, black body text, filled violet buttons at full width or in a row,
one headline per screen, a status line under it, no rules, no cards except on
the deck and the profile, no icon anywhere, no image anywhere.

Two things the frames show that the source alone does not:

- **The moderator banner sits above every screen** — "Moderator controls.
  Currently showing: the radio timeline. Using built-in sample memos." — plus a
  five-item text nav. It is the densest, least considered element in the app and
  it is on every single screenshot an investor would ever see.
- **Text runs edge to edge at default body size.** On the deck, a memo
  transcript is a full-width paragraph of ~14sp Roboto. For the low-vision half
  of the audience this is the product failing at its first job.

## The survival list

What a reposition must carry across. Nothing visual is on it, because nothing
visual exists.

1. **Every accessibility structure.** Live regions, merged semantic nodes with
   one spoken description per card, custom accessibility actions, heading
   semantics, focus order. These are load-bearing and hard-won — the 2026-08-16
   session proved they work on hardware.
2. **The copy, verbatim where quoted in `TRANSLATE.md` row 6.** It is the best
   thing in the product.
3. **No counts, anywhere.** Non-negotiable #4. A design that introduces a number
   next to a like has reversed a product decision through visual language.
4. **Three routes to every action.** Non-negotiable #5. A visual redesign that
   makes an action reachable only by tapping a specific place has broken it.
5. **No timed or precise gestures.** Non-negotiable #6. Rules out swipe-to-reveal
   affordances, drag handles, and anything with a velocity threshold.
6. **The stream visibly ends.** Non-negotiable #3 needs a designed terminal
   state, not an empty scroll.

## Positioning against STYLES.md

Present state is not a style. If it had to be placed, it is unstyled Material —
which sits inside `TRANSLATE.md` row 5's ban list as the category default, so
the current surface is, precisely, the thing this run is banned from producing.
