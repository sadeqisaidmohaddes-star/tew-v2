package org.teww.tew.feature.record

import org.teww.tew.core.record.RecordingState

/**
 * The words the recording screen speaks.
 *
 * Pure, and tested, because this is a screen a blind user operates entirely by
 * ear. If the spoken state is wrong or missing, the user is talking into a
 * phone with no idea whether it is listening — which is the single worst
 * failure this screen can have.
 */

/** Seconds, spoken the way a person would say them. */
fun spokenDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0L && seconds == 1L -> "1 second"
        minutes == 0L -> "$seconds seconds"
        minutes == 1L && seconds == 0L -> "1 minute"
        seconds == 0L -> "$minutes minutes"
        minutes == 1L -> "1 minute $seconds seconds"
        else -> "$minutes minutes $seconds seconds"
    }
}

/**
 * How long is "getting long".
 *
 * BRIEF.md wants short memos. Nothing enforces a cap — cutting someone off
 * mid-sentence is a worse failure than a memo that runs on — so this is a
 * spoken nudge and nothing more.
 */
const val LONG_MEMO_THRESHOLD_MS: Long = 90_000

/** The single spoken line describing what the recorder is doing. */
fun recordingStatus(state: RecordingState, isReply: Boolean): String {
    val what = if (isReply) "reply" else "memo"
    return when (state) {
        is RecordingState.Idle ->
            "Ready. Press Start recording when you want to speak your $what."

        is RecordingState.Recording -> {
            val base = "Recording. ${spokenDuration(state.elapsedMs)} so far."
            if (state.elapsedMs >= LONG_MEMO_THRESHOLD_MS) {
                "$base This is getting long — short memos are easier to listen to."
            } else {
                base
            }
        }

        is RecordingState.Finished ->
            "Recorded ${spokenDuration(state.durationMs)}. " +
                "Listen back, send it, or record it again."

        is RecordingState.Failed -> state.spoken
    }
}

/** Whether the send control should be offered. */
fun canSend(state: RecordingState): Boolean = state is RecordingState.Finished

/** Whether the screen is mid-recording, so the button says Stop. */
fun isRecording(state: RecordingState): Boolean = state is RecordingState.Recording
