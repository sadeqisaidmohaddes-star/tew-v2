package org.teww.tew.feature.carddeck

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs

/**
 * The card-deck feed: one memo, a decision, the next one.
 *
 * ## What this deliberately does NOT assume
 *
 * The TalkBack gesture-passthrough spike in this module **has not been run**,
 * so whether a screen reader lets raw swipes reach the app is still unknown.
 * The approval on that spike came with a condition: no card-deck code built on
 * the raw-touch assumption until the answer is in.
 *
 * So this screen is built the other way round. **Custom accessibility actions
 * are the primary route** — TalkBack dispatches them from its own actions menu,
 * which works regardless of how the passthrough question resolves. On-screen
 * buttons are the second route. Swipes are an *enhancement*: they work when no
 * screen reader is running, and if the spike comes back negative nothing here
 * needs rewriting, because nothing depends on them.
 *
 * If the spike comes back positive, swipes become a third route for screen
 * reader users too, and that is an addition rather than a redesign.
 *
 * ## Gestures are forgiving on purpose
 *
 * Non-negotiable #6 forbids timed or precise gestures. There is no double-tap,
 * no long-press and no velocity threshold here — only direction, over a
 * generous distance, with no time limit. A slow, wandering swipe counts.
 */
@Composable
fun CardDeckScreen(
    viewModel: CardDeckViewModel,
    narrator: Narrator,
    onComment: (String) -> Unit,
    onReport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.start()
        narrator.start()
    }

    // Narrate state changes for users who are not running a screen reader.
    // Narrator suppresses itself when TalkBack is on, so this never doubles up.
    LaunchedEffect(state.announcement) { narrator.say(state.announcement) }

    if (!state.onboardingDone) {
        OnboardingScreen(
            narrator = narrator,
            onFinished = viewModel::onboardingFinished,
            modifier = modifier,
        )
        return
    }

    val memoId = state.current?.id

    val actions = buildList {
        add(CustomAccessibilityAction("Like this memo and move on") { viewModel.like(); true })
        add(CustomAccessibilityAction("Skip this memo") { viewModel.skip(); true })
        add(CustomAccessibilityAction("Play or pause") { viewModel.togglePlayPause(); true })
        add(CustomAccessibilityAction("Play this memo again") { viewModel.playCurrent(); true })
        memoId?.let {
            add(CustomAccessibilityAction("Reply with a voice memo") { onComment(it); true })
            add(CustomAccessibilityAction("Report this memo") { onReport(it); true })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { customActions = actions },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Card deck",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = state.announcement,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        val memo = state.current
        if (memo != null && !state.endOfStream) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .forgivingSwipes(
                        onLeft = viewModel::skip,
                        onRight = viewModel::like,
                        onUp = { memoId?.let(onComment) },
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "From ${memo.authorUsername}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    memo.transcript?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = if (playback.isBuffering) "Loading the audio…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }

        if (state.endOfStream) {
            Text(
                text = CardDeckViewModel.END_OF_STREAM,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::like) { Text("Like") }
            Button(onClick = viewModel::skip) { Text("Skip") }
            Button(onClick = viewModel::playCurrent) { Text("Again") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { memoId?.let(onComment) },
                enabled = memoId != null,
                modifier = Modifier.fillMaxWidth(0.5f),
            ) {
                Text("Reply")
            }
            OutlinedButton(
                onClick = { memoId?.let(onReport) },
                enabled = memoId != null,
            ) {
                Text("Report")
            }
        }
    }
}

/**
 * Direction-only swipes with no time limit and no velocity threshold.
 *
 * The distance is generous ([SWIPE_THRESHOLD_DP]) and accumulated over the
 * whole drag, so a slow or wandering gesture still counts — non-negotiable #6
 * rules out anything that has to be done quickly or precisely. Deliberately
 * *not* marked as the accessible route: see the class KDoc.
 */
private fun Modifier.forgivingSwipes(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onUp: () -> Unit,
): Modifier = this
    .pointerInput(onLeft, onRight) {
        val threshold = SWIPE_THRESHOLD_DP.dp.toPx()
        var travelled = 0f
        var fired = false
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f; fired = false },
        ) { _, delta ->
            travelled += delta
            if (!fired && abs(travelled) > threshold) {
                fired = true
                if (travelled > 0) onRight() else onLeft()
            }
        }
    }
    .pointerInput(onUp) {
        val threshold = SWIPE_THRESHOLD_DP.dp.toPx()
        var travelled = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = { travelled = 0f; fired = false },
        ) { _, delta ->
            travelled += delta
            if (!fired && travelled < -threshold) {
                fired = true
                onUp()
            }
        }
    }

private const val SWIPE_THRESHOLD_DP = 48
