package org.teww.tew.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.teww.tew.core.TewResult
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.record.MemoRecorder
import org.teww.tew.core.record.RecordingState
import org.teww.tew.core.repo.FeedRepository

/** What the composer is doing. [sent] tells the screen it can leave. */
data class RecordUiState(
    val recording: RecordingState = RecordingState.Idle,
    val sending: Boolean = false,
    val sent: Boolean = false,
    val announcement: String = "",
)

/**
 * Backs the memo composer and the reply composer.
 *
 * One view model for both because the only difference is where the audio is
 * posted — [replyToMemoId] null means a new memo, non-null means a reply. Two
 * near-identical view models would be two places for the spoken states to
 * drift apart, and those states are the whole interface here.
 *
 * [nowMs] is injected so the elapsed-time behaviour can be tested without a
 * clock; `:app` passes `SystemClock.elapsedRealtime`.
 */
class RecordViewModel(
    private val recorder: MemoRecorder,
    private val feedRepository: FeedRepository,
    private val playbackSession: PlaybackSession,
    private val replyToMemoId: String?,
    private val nowMs: () -> Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    val isReply: Boolean get() = replyToMemoId != null

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            recorder.state.collect { state ->
                _uiState.value = _uiState.value.copy(
                    recording = state,
                    announcement = recordingStatus(state, isReply),
                )
            }
        }
    }

    fun startRecording() {
        recorder.start(nowMs())
        ticker?.cancel()
        ticker = viewModelScope.launch {
            // Drives the spoken elapsed time. A second is the right grain:
            // faster would interrupt a screen reader mid-word, slower would
            // leave long silences where the user cannot tell it is still on.
            while (recorder.isRecording) {
                delay(TICK_MS)
                recorder.tick(nowMs())
            }
        }
    }

    fun stopRecording() {
        ticker?.cancel()
        ticker = null
        recorder.stop(nowMs())
    }

    /** Play back what was just recorded, so the user can hear it before sending. */
    fun playBack() {
        val finished = _uiState.value.recording as? RecordingState.Finished ?: return
        playbackSession.play(PREVIEW_ID, finished.file.toURI().toString())
    }

    fun recordAgain() {
        playbackSession.stop()
        recorder.discard()
    }

    fun send() {
        val finished = _uiState.value.recording as? RecordingState.Finished ?: return
        playbackSession.stop()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sending = true,
                announcement = "Sending your ${if (isReply) "reply" else "memo"}.",
            )

            val result = if (replyToMemoId != null) {
                feedRepository.postComment(replyToMemoId, finished.file).let {
                    when (it) {
                        is TewResult.Ok -> TewResult.Ok(Unit)
                        is TewResult.Failure -> it
                    }
                }
            } else {
                feedRepository.postMemo(finished.file).let {
                    when (it) {
                        is TewResult.Ok -> TewResult.Ok(Unit)
                        is TewResult.Failure -> it
                    }
                }
            }

            _uiState.value = when (result) {
                is TewResult.Ok -> _uiState.value.copy(
                    sending = false,
                    sent = true,
                    announcement = if (isReply) {
                        "Reply sent."
                    } else {
                        "Memo posted. Other people will hear it in their feed."
                    },
                )

                is TewResult.Failure -> _uiState.value.copy(
                    // Deliberately not cleared: the recording is still on disk
                    // and still sendable. Losing someone's memo because the
                    // network blipped would be unforgivable on a connection
                    // this app is explicitly designed for.
                    sending = false,
                    announcement = "${result.spoken} Your recording is still here — try sending again.",
                )
            }
        }
    }

    fun cancel() {
        ticker?.cancel()
        playbackSession.stop()
        recorder.discard()
    }

    override fun onCleared() {
        ticker?.cancel()
        recorder.release()
        super.onCleared()
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val PREVIEW_ID = "local-preview"
    }
}
