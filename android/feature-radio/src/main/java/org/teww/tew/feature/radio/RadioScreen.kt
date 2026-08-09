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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import org.teww.tew.core.playback.FeedCommand
import org.teww.tew.core.playback.FeedCommandBus
import org.teww.tew.core.voice.VoiceCommandListener
import org.teww.tew.core.voice.VoiceState
import org.teww.tew.core.voice.voiceNotUnderstood
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
 * **Route coverage (non-negotiable #5).** All three routes are present:
 *
 * 1. **Media controls** — headset and lock-screen transport buttons, via the
 *    `MediaSession` in `:core`. Next skips, previous replays, play/pause does
 *    what it says.
 * 2. **Voice** — press-to-talk, never always-listening. See
 *    [org.teww.tew.core.voice.VoiceCommandListener] for why.
 * 3. **Screen-reader menu** — custom accessibility actions TalkBack surfaces
 *    in its own actions list, reachable without hunting for a button.
 *
 * On-screen controls are a fourth, for people who want them. Every route lands
 * in [RadioViewModel.onCommand], so none of them can drift into supporting a
 * different set of actions from the others.
 */
@Composable
fun RadioScreen(
    viewModel: RadioViewModel,
    onComment: (String) -> Unit,
    onReport: (String) -> Unit,
    modifier: Modifier = Modifier,
    commandBus: FeedCommandBus? = null,
    voice: VoiceCommandListener? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playbackState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    // Media buttons and voice both land in the view model's single command
    // entry point. Registered here and released on dispose so a command never
    // reaches a feed screen the moderator has toggled away from.
    DisposableEffect(commandBus, voice) {
        viewModel.onNavigationCommand = { command, memoId ->
            when (command) {
                FeedCommand.REPLY -> onComment(memoId)
                FeedCommand.REPORT -> onReport(memoId)
                else -> Unit
            }
        }
        commandBus?.setHandler(viewModel::onCommand)
        voice?.setCommandHandler(viewModel::onCommand)
        onDispose {
            commandBus?.setHandler(null)
            voice?.setCommandHandler(null)
            viewModel.onNavigationCommand = null
        }
    }

    val voiceState = voice?.state?.collectAsState()?.value ?: VoiceState.Idle

    val memoId = state.current?.id

    // The same four actions the buttons below perform, exposed through
    // TalkBack's actions menu so they can be reached from anywhere on the
    // screen rather than only by finding the right button.
    val actions = buildList {
        add(CustomAccessibilityAction("Like this memo") { viewModel.likeCurrent(); true })
        add(CustomAccessibilityAction("Skip to the next memo") { viewModel.skipCurrent(); true })
        add(CustomAccessibilityAction("Play or pause") { viewModel.togglePlayPause(); true })
        add(CustomAccessibilityAction("Play this memo again") { viewModel.replayCurrent(); true })
        voice?.let { add(CustomAccessibilityAction("Speak a command") { it.startListening(); true }) }
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

        if (voice != null) {
            Text(
                text = voiceStatusLine(voiceState),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Button(
                onClick = { voice.startListening() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Speak a command")
            }
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
 * What the voice route is doing, in words. Pure so it can be tested.
 *
 * Every state says something. A voice route that goes quiet when it fails is
 * a dead end — the user has no way to tell "not listening" from "listening and
 * ignoring me".
 */
internal fun voiceStatusLine(state: VoiceState): String = when (state) {
    is VoiceState.Idle -> "Voice is off. Press Speak a command to use it."
    is VoiceState.Listening -> "Listening."
    is VoiceState.Heard -> if (state.command != null) {
        "Heard \"${state.text}\"."
    } else {
        voiceNotUnderstood(state.text)
    }
    is VoiceState.Unavailable -> state.spoken
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
