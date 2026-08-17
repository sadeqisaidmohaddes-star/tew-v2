# TRANSLATE — tew-android

The six-row input contract for the inter.face run on the TEW v2 Android client.

Rows 1, 2, 4 and 6 are derived from evidence in this repo. Rows 3 and 5 are
**ASSUMED** — Taha was asked and declined on time grounds, so they are filled
with the best available reading and marked, per `TRANSLATE.md`'s floor rule. An
ASSUMED row is a visible question, not a fact. Both are cheap to overturn in one
sentence, and overturning row 3 invalidates the concepts.

---

## 1. Surface class — `tool-shaped`

Not a default. Both derivation questions agree: used **daily** by the same
person, and a visit is **minutes**, not seconds. There is no marketing page in
this repo.

**What it binds.** `§15` keyboard completeness becomes **[HARD]** — on Android
that is switch access and D-pad traversal, which non-negotiable #5 already
demands as the third route. Every screen owes the nine data states, not three.
`TOOLS.md` is in the reading list.

## 2. Viewer and their decision or task

A blind or low-vision person, on a budget Android phone, opening the app in the
evening. They hear what people said today and decide, memo by memo, whether to
like it, skip it, or answer it with their own voice. They are listening, not
looking; most will have TalkBack running, some will have low vision and be
reading the screen at very large sizes.

**Not the investor.** The person Taha wants to impress does not appear in this
row — they arrive in row 3, which is the three-second row and exactly where an
evaluator belongs.

## 3. The three-second feel — ASSUMED

> An app with almost nothing on it, and every absence obviously deliberate.

**Why this and not what was asked for.** Taha's answer to the forcing question
was "something new and not made before." That is a property, not a memory —
nobody recalls novelty, they recall an image. It also collides with his earlier
instruction to make it look like a conventional app so it passes review
unquestioned. Both cannot be served. This row resolves the collision toward the
brief: `BRIEF.md`'s stance is refusal — the stream ends, no engagement
machinery, no counts, no endless pile — and an interface whose emptiness is
legible as intent is the visual form of that stance. It is also the only version
where the differentiator survives a screenshot.

**If this row is wrong, the concepts are wrong.** It is the cheapest thing in
this run to correct and the most expensive to leave.

## 4. Archetype and shadow — ASSUMED, from the subject's own sentences

**Archetype: patient, plainspoken, finite.** Taken near-verbatim from what the
project has actually written, not from an archetype table:

- "The product targets loneliness, not engagement — the headline success metric
  is the UCLA Loneliness Scale, not time-on-app." (`BRIEF.md`)
- "The stream ends." / "No engagement machinery." (non-negotiables #3, #4)
- "Take as long as you like — there is no flick to get right and nothing to
  time." (onboarding copy)
- "There is no endless pile." (onboarding copy)
- "Audio is the medium, not a fallback." (non-negotiable #1)

The recurring stance across all five is **refusal** — the product is defined by
what it declines to do to the person using it.

**Shadow: worthy.** The charity leaflet, the medical device, the assistive-tech
product that asks to be admired for its intentions rather than used. A nonprofit
built for blind people, refusing engagement mechanics, lands there by default if
nothing pushes against it — and it is precisely the register that loses the
evaluator in row 3.

## 5. Anti-positioning — ASSUMED, three named bans

Derived from the shadow and from what is currently on screen, not from recoil —
the probe interview did not happen.

1. **Assistive-tech institutional.** Clinical blues, beige, hospital-form
   layouts, oversized rounded buttons with ear or eye pictograms, stock imagery
   of a hand on a shoulder. This is the shadow wearing a palette.
2. **Generic social app.** Avatar circles, heart icons, gradient violet, a
   five-icon bottom tab bar, cards with rounded corners and a drop shadow.
   Includes, specifically, **the Material 3 baseline the app is wearing right
   now** — `#6750A4` and Roboto are the category default arrived at by accident.
3. **The voice-app waveform.** The animated squiggle standing in for audio. It
   is the stock photo of this category: it looks like content without being any,
   and it decorates for an audience that cannot see it.

Ban 3 is the one most likely to be violated by a well-meaning designer, because
it is the obvious answer to "what do you put on screen for sound."

## 6. What is already owned — almost nothing, and that is the finding

**Type: none.** No `@font-face`, no Compose `Typography`. Roboto is the platform
substitute, not a choice.

**Color: none.** `app/src/main/res/values/themes.xml` is
`android:Theme.Material.Light.NoActionBar`, and `MainActivity` calls
`setContent { TewApp(container) }` with **no `MaterialTheme` wrapper at all**.
Every `MaterialTheme.colorScheme` and `MaterialTheme.typography` call in all six
modules resolves to Compose's baseline `lightColorScheme()`. The violet on the
device recording is the absence of a decision, not a brand.

**Accent: none exists — an accent will be chosen and justified in Loop 1.**

**Logo: none.**

**Owned copy — load-bearing, and it survives.** The writing is the strongest
asset this product has:

- "Short voice memos, from people who get it." (sign-in)
- "That is the last card. The stream has ended — there is no more to swipe
  through. Come back later, or record a memo of your own."
- "There is no endless pile."

**Design system to conform to: none.** Material is what it is accidentally
wearing, not a system it is bound to, so `TRANSLATE.md`'s conformance escape
hatch does not apply. This is a reposition.
