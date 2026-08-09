package org.teww.tew.feature.carddeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
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

    LaunchedEffect(step) { narrator.say(current.spoken) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "How the card deck works",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = "Step ${step + 1} of ${steps.size}",
            style = MaterialTheme.typography.labelLarge,
        )

        Text(
            text = current.spoken,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        Button(
            onClick = {
                if (step < steps.lastIndex) step++ else onFinished()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (step < steps.lastIndex) "Next" else "Start listening")
        }

        if (step > 0) {
            Button(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

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
