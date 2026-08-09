package org.teww.tew.feature.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.teww.tew.core.playback.PlaybackState

/**
 * The radio timeline: memos play in order, hands-free, like a station.
 *
 * The screen is deliberately plain. Nothing here needs looking at — the memo
 * plays on its own and advances on its own, so the visible controls exist for
 * people who want them rather than as the primary way through.
 *
 * **Route coverage (non-negotiable #5).** Every action is supposed to have
 * three routes: media controls, voice, and the screen-reader menu. Two are
 * present here — on-screen controls, and custom accessibility actions that
 * TalkBack surfaces in its own actions menu, reachable without hunting for a
 * button. **Hardware/notification media controls and voice control are not
 * built yet** — see this module's README. That is a known gap, not a claim of
 * compliance.
 */
@Composable
fun RadioScreen(
    viewModel: RadioViewModel,
    onComment: (String) -> Unit,
    onReport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    val memoId = state.current?.id

    // The same four actions the buttons below perform, exposed through
    // TalkBack's actions menu so they can be reached from anywhere on the
    // screen rather than only by finding the right button.
    val actions = buildList {
        add(CustomAccessibilityAction("Like this memo") { viewModel.likeCurrent(); true })
        add(CustomAccessibilityAction("Skip to the next memo") { viewModel.skipCurrent(); true })
        add(CustomAccessibilityAction("Play or pause") { viewModel.togglePlayPause(); true })
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
            text = "Radio",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = state.announcement,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        val memo = state.current
        if (memo != null) {
            Text(
                text = "From ${memo.authorUsername}",
                style = MaterialTheme.typography.titleMedium,
            )
            memo.transcript?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = playbackLine(playback),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (state.endOfStream) {
            Text(
                text = RadioViewModel.END_OF_STREAM,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::togglePlayPause) {
                Text(if (playback.isPlaying) "Pause" else "Play")
            }
            Button(onClick = viewModel::likeCurrent) {
                Text(if (memo?.likedByMe == true) "Unlike" else "Like")
            }
            Button(onClick = viewModel::skipCurrent) { Text("Skip") }
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
 * What the progress line says. Pure so it can be tested.
 *
 * Buffering is called out explicitly rather than shown as a stalled position:
 * a listener who hears nothing needs to know whether the app is working or
 * broken, and IMPLEMENTATION.md forbids silent waits past half a second.
 */
internal fun playbackLine(state: PlaybackState): String = when {
    state.error != null -> state.error!!
    state.isBuffering -> "Loading the audio…"
    state.isIdle -> "Nothing playing."
    !state.isPlaying -> "Paused."
    state.durationMs <= 0 -> "Playing."
    else -> {
        val seconds = (state.durationMs - state.positionMs).coerceAtLeast(0) / 1000
        "Playing. $seconds seconds left."
    }
}
