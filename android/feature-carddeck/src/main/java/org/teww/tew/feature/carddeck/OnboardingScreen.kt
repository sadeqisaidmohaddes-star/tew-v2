package org.teww.tew.feature.carddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Teaches the card deck's vocabulary before the first card.
 *
 * `android/README.md` puts "the onboarding flow that drills it" in this
 * module's scope. The drilling matters: a gesture vocabulary nobody was taught
 * is indistinguishable from a broken screen, and the usability comparison this
 * whole build exists to run would then be measuring who guessed rather than
 * which model works.
 *
 * Each step is advanced by a button rather than by performing the gesture. That
 * is deliberate, and it is a change from how a sighted onboarding would work:
 * until the passthrough spike has been run we do not know that a swipe reaches
 * the app at all with TalkBack on, and an onboarding that cannot be completed
 * without one would lock a screen-reader user out of the app entirely on the
 * very first screen.
 */
@Composable
fun OnboardingScreen(
    narrator: Narrator,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    val steps = onboardingSteps()
    val current = steps[step]
    val last = step == steps.lastIndex

    LaunchedEffect(step) { narrator.say(current.spoken) }

    fun advance() = if (last) onFinished() else step++
    fun retreat() { if (step > 0) step-- }

    // Both moves reach the screen reader's actions menu, and this is the one
    // screen where that is not optional: leaf 3 promises "every one is also in
    // your screen reader's actions menu", and the leaf making the claim has to
    // be the first thing that honours it.
    val actions = buildList {
        add(CustomAccessibilityAction(if (last) "Start listening" else "Next leaf") { advance(); true })
        if (step > 0) add(CustomAccessibilityAction("Previous leaf") { retreat(); true })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .semantics { customActions = actions },
    ) {
        RunningHead(text = "How this works · ${step + 1} of ${steps.size}")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_MARGIN),
        ) {
            Spacer(Modifier.height(SPACE300))

            // One idea per leaf, and nothing else on the leaf. Focusable in its
            // own right: on this surface the idea *is* the screen, and a switch
            // scan that skips it skips everything.
            Text(
                text = current.spoken,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )

            // The held space is the composition, not a leftover — and it is the
            // first thing to give way as type grows. Once it is down to its
            // minimum the column scrolls, and the actions below never move.
            Spacer(Modifier.heightIn(min = SPACE300).weight(1f, fill = false))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SCREEN_MARGIN),
        ) {
            Rule()
            Spacer(Modifier.height(SPACE200))
            ActionLine(text = if (last) "Start listening" else "Next", onClick = { advance() })
            if (step > 0) {
                Spacer(Modifier.height(SPACE100))
                ActionLine(text = "Back", onClick = { retreat() })
            }
            Spacer(Modifier.height(SPACE300))
        }
    }
}

/**
 * The record's running head: which section this is, and where in it you are.
 *
 * A preface is undated, so the leaf counter takes the date's slot. It is set
 * into the head rather than printed above the idea so a screen reader speaks
 * the position as part of the section name, instead of as a stray fragment.
 *
 * Reserved, never floating, and it grows rather than clipping — a 15 sp head at
 * 200% resolves to more than a fixed 56 dp band can hold once padding exists.
 */
@Composable
private fun RunningHead(text: String) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RUNNING_HEAD_MIN)
                .padding(horizontal = SCREEN_MARGIN),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
        }
        Rule()
    }
}

/**
 * A printed hairline. Decorative only — never a boundary, a focus ring, or an
 * underline. It measures 2.28:1 against the page, which is right for a rule and
 * disqualifies it from carrying any of those other jobs.
 *
 * Whether it bleeds or is inset is decided by where it is placed: the head rule
 * sits outside the screen margin and runs full width, the action rule sits
 * inside it and stops at the column.
 */
@Composable
private fun Rule() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

/**
 * An action, set as a line of the record rather than as a button.
 *
 * No fill and no border: a filled slab would read as permanently focused, since
 * reversal is this design's focus device. Weight carries it instead, never an
 * underline — RNIB is explicit that underlining hurts, and the hairline above
 * the block is a rule, not an underline.
 *
 * **The 48 dp target is asserted, never inferred from the type.** A 20 sp line
 * at 1.45 is ≈29 dp, and +10 dp above and below reaches 49 dp only at font
 * scale 1.0 — at Android's 0.85 it drops to 44.7 dp and fails. So the minimum
 * is stated in dp and the target spans the full column, past the glyphs.
 */
@Composable
private fun ActionLine(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TARGET_MIN)
            .clickable(onClick = onClick)
            .padding(vertical = SPACE125),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

private val SCREEN_MARGIN = 24.dp
private val RUNNING_HEAD_MIN = 56.dp
private val TARGET_MIN = 48.dp
private val SPACE100 = 8.dp
private val SPACE125 = 10.dp
private val SPACE200 = 16.dp
private val SPACE300 = 24.dp

data class OnboardingStep(val spoken: String)

/**
 * The script. Pure and testable, because what it promises has to match what
 * the screen actually does — onboarding that teaches a gesture the app does
 * not honour is worse than no onboarding.
 */
fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        "This is the card deck. You hear one memo at a time, and you decide what " +
            "to do with it before the next one arrives.",
    ),
    OnboardingStep(
        "There are three things you can do with a memo: like it, skip it, or reply " +
            "with a voice memo of your own.",
    ),
    OnboardingStep(
        "Every one of those has a button on screen, and every one is also in your " +
            "screen reader's actions menu. You never have to find a particular spot " +
            "on the glass.",
    ),
    OnboardingStep(
        "If you are not using a screen reader, you can also swipe: right to like, " +
            "left to skip, up to reply. Take as long as you like — there is no " +
            "flick to get right and nothing to time.",
    ),
    OnboardingStep(
        "The deck ends. When you reach the last card, we will tell you, and that " +
            "is the end of it for now. There is no endless pile.",
    ),
)
