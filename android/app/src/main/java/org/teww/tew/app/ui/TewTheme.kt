package org.teww.tew.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.teww.tew.app.R

/**
 * The record, set as a large-print edition.
 *
 * From `runs/tew-android/DIRECTION.md`, Loop 1 of the inter.face run. The
 * concept is an official report — one column, a speaker named, entries timed,
 * corrections printed rather than hidden — set at the type size a low-vision
 * reader actually needs. The two parents fight: a record is normally dense and
 * small, a large-print edition has no structural apparatus. Forcing one through
 * the other leaves no margin, so the marginal apparatus moves into the reading
 * line — and since a screen reader already reads linearly, the visual design
 * and the spoken design become the same object.
 *
 * ## Read this before changing a number
 *
 * Every value here was derived against a source and most of them were corrected
 * by measurement at least once. `DIRECTION.md` carries the reasoning in full and
 * wins wherever it and this file disagree. The short version:
 *
 * - **Nothing is smaller than 15 sp, and body is 20 sp.** RNIB puts large print
 *   at 16–18 point; 20 sp clears it. Material's 12 sp floor is not the floor
 *   here, because Material was not designed for this audience.
 * - **Every text pair is at least 4.5:1 and body is at least 7:1**, regardless of
 *   size. Android publishes an 18 sp boundary below which 4.5:1 applies and 3:1
 *   above it. This design declines the exemption: an exception that lowers
 *   contrast as type grows is backwards for people who need the type large.
 * - **The accent is graphical only and never carries text.** Its one job is to be
 *   the single mark in a record. A colour with two jobs has none.
 *
 * ## What this file deliberately does not do
 *
 * It rethemes; it does not relayout. `DIRECTION.md` §7 specifies front matter, a
 * running head, entry rules, reversal-as-focus and a designed closing surface —
 * none of which are here, because they are per-screen work. What this file gives
 * those screens is the palette, the scale and the family to be rebuilt against.
 */
@Composable
fun TewTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) ReversedEdition else Page,
        typography = if (dark) typographyFor(dark = true) else typographyFor(dark = false),
        shapes = PrintShapes,
        content = content,
    )
}

// ---------------------------------------------------------------- the palette

/*
 * The printer's second ink — the one colour on a page that is otherwise black on
 * cream, reached for only where the record has been corrected, withheld, or
 * closed. Its rarity is what makes it read as deliberate.
 *
 * Computed OKLCH → sRGB, all in gamut. The neutral ramp holds hue 40 at chroma
 * 0.008–0.014; the accent sits at hue 28, four degrees away, which is why it
 * reads as a second ink from the same press rather than as a highlight.
 */

private val Paper = Color(0xFFFDF3EF)
private val PaperSunk = Color(0xFFF2E7E3)
private val Ink = Color(0xFF191210)
private val InkQuiet = Color(0xFF5A504D)
private val Rule = Color(0xFFACA29F)
private val Mark = Color(0xFFA12721)

private val Board = Color(0xFF110C0A)
private val BoardRaised = Color(0xFF1D1714)
private val Light = Color(0xFFF0E9E7)
private val LightQuiet = Color(0xFF9F9693)
private val RuleDark = Color(0xFF524B49)
private val MarkDark = Color(0xFFC86459)

/**
 * Light — "the page". `Ink` on `Paper` measures 16.91:1.
 *
 * `primary` is the ink itself, so a filled control is a black slab on cream
 * rather than a coloured one. Note the tension `DIRECTION.md` names: reversal is
 * this concept's focus device, so a permanently reversed control reads as
 * permanently focused. The spec's answer is action rows with no fill at all,
 * carrying weight and a hairline instead — that is layout work, and until it
 * lands the filled control stays, because a violet slab is worse than a black
 * one on every axis that matters here.
 */
private val Page = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = InkQuiet,
    onSecondary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSunk,
    onSurfaceVariant = InkQuiet,
    outline = Rule,
    outlineVariant = Rule,
    // The accent's one sanctioned role. It measures 6.77:1 on paper and would
    // legally carry text; it is still barred from doing so.
    error = Mark,
    onError = Paper,
)

/**
 * Dark — "the reversed large-print edition". A real published object, not an
 * inverted page: white on near-black bleeds optically, so every weight drops 50
 * units in [typographyFor] and the rules lighten.
 *
 * `MarkDark` on `BoardRaised` measures 4.60:1 — barely over the bar — so
 * `BoardRaised` must never be lightened.
 */
private val ReversedEdition = darkColorScheme(
    primary = Light,
    onPrimary = Board,
    secondary = LightQuiet,
    onSecondary = Board,
    background = Board,
    onBackground = Light,
    surface = Board,
    onSurface = Light,
    surfaceVariant = BoardRaised,
    onSurfaceVariant = LightQuiet,
    outline = RuleDark,
    outlineVariant = RuleDark,
    error = MarkDark,
    onError = Board,
)

// ------------------------------------------------------------------ the type

/**
 * Atkinson Hyperlegible Next, variable, roman only, `wght` 200–800, SIL OFL 1.1.
 * Licence at `licenses/AtkinsonHyperlegibleNext-OFL.txt`.
 *
 * **Why this face and not the platform's.** It disambiguates the exact character
 * pairs low-vision readers confuse — I/l/1, 0/O, b/d — by *drawing them
 * differently* rather than by weight, which survives Android's non-linear
 * scaling curve and survives central field loss. Roboto, the incumbent by
 * absence, does none of that at any size.
 *
 * **Bundled, not a downloadable font.** 112 KB, covering the whole axis. On the
 * throttled-3G target `IMPLEMENTATION.md` names, a downloadable font paints in
 * Roboto and then reflows — and for a low-vision reader a reflow is a re-read.
 * The italic variable font is 121 KB and is **not shipped at all**: RNIB says
 * avoid italics, so there is nothing for it to do.
 */
@OptIn(ExperimentalTextApi::class)
private fun atkinson(weight: Int): FontFamily = FontFamily(
    Font(
        resId = R.font.atkinson_hyperlegible_next,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
    ),
)

/**
 * The scale is role-indexed rather than ratio-derived — each size answers to a
 * job on a screen, not to a multiplier.
 *
 * | Role | Size | Weight light / dark | Line height |
 * | --- | --- | --- | --- |
 * | Display | 34 sp | 600 / 550 | 1.25 |
 * | Speaker | 24 sp | 600 / 550 | 1.30 |
 * | Body | 20 sp | 400 / 350 | 1.45 |
 * | Action | 20 sp | 600 / 550 | 1.45 |
 * | Time | 17 sp | 400 / 350 | 1.40 |
 * | Running head | 15 sp | 500 / 450 | 1.35 |
 *
 * Mapped onto the Material slots the app already calls, so no screen has to
 * change to pick this up.
 *
 * Three rules from RNIB's Clear Print guidance are enforced here rather than
 * left to each screen: text is **ragged-right, never justified**; **nothing is
 * underlined** — actions carry weight instead; and **nothing is set in capitals**,
 * because the shape of the word goes missing. The running head is the one place
 * a record would conventionally use caps, and the convention loses to the
 * audience.
 */
private fun typographyFor(dark: Boolean): Typography {
    // White on near-black bleeds, so the reversed edition is set lighter. This
    // is the difference between two art directions and one design run twice.
    val regular = if (dark) 350 else 400
    val medium = if (dark) 450 else 500
    val strong = if (dark) 550 else 600

    fun style(
        sizeSp: Int,
        weight: Int,
        lineHeightRatio: Float,
        tabularFigures: Boolean = false,
    ) = TextStyle(
        fontFamily = atkinson(weight),
        fontWeight = FontWeight(weight),
        fontSize = sizeSp.sp,
        // sp is never summed with dp anywhere in this design, and line height is
        // no exception: it scales with the text, so it is expressed in sp.
        lineHeight = (sizeSp * lineHeightRatio).sp,
        textAlign = TextAlign.Start,
        fontFeatureSettings = if (tabularFigures) "tnum" else null,
    )

    val display = style(34, strong, 1.25f)
    val speaker = style(24, strong, 1.30f)
    val body = style(20, regular, 1.45f)
    val action = style(20, strong, 1.45f)
    // `tnum` is a real substitution in this face, not an alias: the default
    // figures are proportional and tnum swaps in a uniform-advance set. It stops
    // a live elapsed count from shuddering as the digits change.
    val time = style(17, regular, 1.40f, tabularFigures = true)
    val runningHead = style(15, medium, 1.35f)

    return Typography(
        displayLarge = display,
        displayMedium = display,
        displaySmall = display,
        headlineLarge = display,
        headlineMedium = speaker,
        headlineSmall = speaker,
        titleLarge = speaker,
        titleMedium = speaker,
        titleSmall = body,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = time,
        // Button and other control labels. Weight, never underline.
        labelLarge = action,
        labelMedium = action,
        labelSmall = runningHead,
    )
}

// ---------------------------------------------------------------- the shapes

/**
 * Print has no rounded corners and no elevation. Flat surfaces also make
 * "Reduce Transparency" a no-op by construction — there is no scrim, no blur and
 * no shadow anywhere carrying meaning that would have to survive it being
 * switched off.
 *
 * Not zero: 2 dp is the ink-spread of a printed rule rather than a radius you
 * would call a corner. It keeps a filled control from reading as a hole punched
 * in the page.
 */
private val PrintShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
)
