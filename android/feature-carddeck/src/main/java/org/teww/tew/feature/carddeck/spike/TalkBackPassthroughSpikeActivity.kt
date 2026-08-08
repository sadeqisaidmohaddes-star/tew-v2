package org.teww.tew.feature.carddeck.spike

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The TalkBack / gesture-passthrough spike.
 *
 * `android/README.md` names this as the one open technical risk the whole
 * card-deck direction rests on: the card deck needs a gesture surface that
 * TalkBack does not intercept before the app sees it, and Android has no
 * equivalent of iOS's direct-interaction trait. The pattern the README
 * proposes to test is "mark the surface not-important-for-accessibility and
 * handle raw touch dispatch yourself".
 *
 * This screen tests exactly that, and is deliberately a throwaway: it is not
 * the card deck, it imports nothing from `:core`, and it should be deleted
 * once the question is settled.
 *
 * ## What it does
 *
 * - The middle band is the gesture surface, with its semantics cleared
 *   ([Modifier.clearAndSetSemantics]) — the Compose equivalent of
 *   `importantForAccessibility="noHideDescendants"`. Horizontal swipes on it
 *   are supposed to reach the app.
 * - Every touch and hover event arriving at the content view is recorded by
 *   [ProbeFrameLayout], so the run produces evidence rather than an
 *   impression.
 * - A recognised swipe fires an earcon and a spoken word, each timed from the
 *   gesture — see [AudibleFeedback] for what those timings do and do not
 *   include.
 * - The header, the buttons and the log are ordinary accessible content. They
 *   are the control group: if TalkBack still focuses and announces them
 *   normally while the surface is passing gestures through, the mechanism is
 *   selective rather than a blanket accessibility opt-out.
 *
 * ## Running it
 *
 * Not launchable from the home screen by design — it is a spike, not a
 * feature. Install the debug APK and start it directly:
 *
 * ```
 * adb shell am start -n org.teww.tew/org.teww.tew.feature.carddeck.spike.TalkBackPassthroughSpikeActivity
 * ```
 *
 * The full protocol, including what to record, is in
 * `feature-carddeck/SPIKE.md`.
 */
class TalkBackPassthroughSpikeActivity : ComponentActivity() {

    private val events = mutableStateListOf<ProbeEvent>()
    private val samples = mutableStateListOf<LatencySample>()

    private var touchExplorationOn by mutableStateOf(false)
    private var accessibilityOn by mutableStateOf(false)

    private lateinit var feedback: AudibleFeedback

    private val accessibilityManager: AccessibilityManager
        get() = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        feedback = AudibleFeedback(this) { sample -> samples.add(sample) }
        feedback.start()

        val probe = ProbeFrameLayout(this).apply {
            onProbe = { kind, action ->
                // Bounded so a long exploration session cannot grow without
                // limit; the verdict only depends on which kinds appeared.
                if (events.size >= MAX_EVENTS) events.removeAt(0)
                events.add(ProbeEvent(kind, action, android.os.SystemClock.uptimeMillis()))
            }
        }

        probe.addView(
            ComposeView(this).apply {
                setContent {
                    MaterialTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            SpikeScreen(
                                events = events,
                                samples = samples,
                                accessibilityOn = accessibilityOn,
                                touchExplorationOn = touchExplorationOn,
                                onSwipe = ::onSurfaceSwipe,
                                onSpeakResults = ::speakResults,
                                onReset = ::reset,
                            )
                        }
                    }
                }
            },
        )

        setContentView(probe)
    }

    override fun onResume() {
        super.onResume()
        // Read on resume so the tester can toggle TalkBack and come back
        // without reinstalling or restarting the spike.
        accessibilityOn = accessibilityManager.isEnabled
        touchExplorationOn = accessibilityManager.isTouchExplorationEnabled
    }

    override fun onDestroy() {
        feedback.stop()
        super.onDestroy()
    }

    /**
     * A horizontal swipe was recognised on the cleared-semantics surface.
     * [gestureUptimeMillis] is the event time of the pointer sample that
     * crossed the threshold, so the latency figures start at recognition.
     */
    private fun onSurfaceSwipe(direction: String, gestureUptimeMillis: Long) {
        feedback.earcon(gestureUptimeMillis)
        feedback.speak(direction, gestureUptimeMillis)
    }

    private fun speakResults() {
        val verdict = verdictFor(events)
        feedback.announce(verdictSummary(verdict, touchExplorationOn))
        feedback.announce(describeStats(summarise(samples, FeedbackRoute.EARCON)))
        feedback.announce(describeStats(summarise(samples, FeedbackRoute.SPEECH)))
    }

    private fun reset() {
        events.clear()
        samples.clear()
        feedback.announce("Cleared.")
    }

    private companion object {
        const val MAX_EVENTS = 400
    }
}

@Composable
private fun SpikeScreen(
    events: List<ProbeEvent>,
    samples: List<LatencySample>,
    accessibilityOn: Boolean,
    touchExplorationOn: Boolean,
    onSwipe: (String, Long) -> Unit,
    onSpeakResults: () -> Unit,
    onReset: () -> Unit,
) {
    val verdict = verdictFor(events)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "TalkBack gesture passthrough spike",
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = "Screen reader: ${onOff(accessibilityOn)}. " +
                "Touch exploration: ${onOff(touchExplorationOn)}.",
        )

        // The gesture surface under test. Semantics cleared so TalkBack has no
        // node here — the mechanism android/README.md asks us to prove out.
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clearAndSetSemantics { }
                .pointerInput(Unit) {
                    val thresholdPx = SWIPE_THRESHOLD_DP.dp.toPx()
                    var travelled = 0f
                    var fired = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            travelled = 0f
                            fired = false
                        },
                    ) { change, dragAmount ->
                        travelled += dragAmount
                        if (!fired && abs(travelled) > thresholdPx) {
                            fired = true
                            onSwipe(
                                if (travelled > 0) "right" else "left",
                                change.uptimeMillis,
                            )
                        }
                    }
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Swipe here, left or right")
                Text("(this area is hidden from the screen reader)")
            }
        }

        Text(text = verdictSummary(verdict, touchExplorationOn))

        Text(text = describeStats(summarise(samples, FeedbackRoute.EARCON)))
        Text(text = describeStats(summarise(samples, FeedbackRoute.SPEECH)))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSpeakResults) { Text("Speak results") }
            Button(onClick = onReset) { Text("Clear") }
        }

        Text(
            text = "Events received (newest last), ${events.size} total:",
            style = MaterialTheme.typography.titleSmall,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            events.takeLast(VISIBLE_EVENTS).forEach { event ->
                Text(text = "${event.kind.name}  ${event.action}")
            }
        }
    }
}

private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

private const val SWIPE_THRESHOLD_DP = 24
private const val VISIBLE_EVENTS = 40
