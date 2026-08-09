package org.teww.tew.core.record

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** What the recorder is doing, in a form a screen can speak. */
sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val elapsedMs: Long) : RecordingState
    data class Finished(val file: File, val durationMs: Long) : RecordingState
    data class Failed(val spoken: String) : RecordingState
}

/**
 * Captures a voice memo to a file.
 *
 * Lives in `:core` because it is infrastructure with no UI, and because both
 * the memo composer and the reply composer need it — putting it in either
 * feature module would mean the other imported from it.
 *
 * ## Format
 *
 * AAC in an MP4 container at a speech-appropriate bitrate. `backend/README.md`
 * runs whisper.cpp over these, and the container question is genuinely open —
 * `core/API_CONTRACT.md` lists it for the backend author to settle. Changing it
 * is one constant here.
 *
 * The bitrate is deliberately low. These are voice memos uploaded from a budget
 * phone on throttled 3G, and a minute of audio at music bitrates is a minute of
 * waiting the user spends in silence.
 *
 * ## No maximum duration is enforced here
 *
 * The product wants short memos, but cutting someone off mid-sentence is a
 * worse failure than a long recording. The composer screen warns as the memo
 * gets long; it does not stop the user.
 */
class MemoRecorder(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0

    val isRecording: Boolean get() = _state.value is RecordingState.Recording

    /**
     * Begin recording. [nowMs] is injected rather than read from the clock so
     * duration handling can be tested; callers pass `SystemClock.elapsedRealtime()`.
     */
    fun start(nowMs: Long) {
        if (isRecording) return

        val file = File(appContext.cacheDir, "memo-$nowMs.m4a")
        outputFile = file

        try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(BITRATE)
                setAudioSamplingRate(SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            startedAtMs = nowMs
            _state.value = RecordingState.Recording(0)
        } catch (e: Exception) {
            // Includes the permission case: MediaRecorder throws rather than
            // returning an error, and a silent failure here would leave the
            // user talking to a phone that is not listening.
            cleanUp()
            _state.value = RecordingState.Failed(
                "Recording could not start. Check that TEW is allowed to use the microphone.",
            )
        }
    }

    /** Called by the screen's ticker so the elapsed time can be spoken. */
    fun tick(nowMs: Long) {
        if (isRecording) {
            _state.value = RecordingState.Recording(nowMs - startedAtMs)
        }
    }

    fun stop(nowMs: Long) {
        val active = recorder ?: return
        val file = outputFile
        try {
            active.stop()
            val duration = nowMs - startedAtMs
            _state.value = if (file != null && file.length() > 0) {
                RecordingState.Finished(file, duration)
            } else {
                RecordingState.Failed("Nothing was recorded. Try again.")
            }
        } catch (e: RuntimeException) {
            // MediaRecorder.stop() throws when it was stopped almost
            // immediately after start — the file is unusable rather than short.
            file?.delete()
            _state.value = RecordingState.Failed(
                "That was too short to save. Hold on a moment longer and try again.",
            )
        } finally {
            cleanUp()
        }
    }

    /** Abandon the recording and delete the file. */
    fun discard() {
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Already stopped or never started; nothing to salvage either way.
        }
        cleanUp()
        outputFile?.delete()
        outputFile = null
        _state.value = RecordingState.Idle
    }

    fun release() {
        cleanUp()
        _state.value = RecordingState.Idle
    }

    private fun cleanUp() {
        recorder?.reset()
        recorder?.release()
        recorder = null
    }

    private companion object {
        /** Speech, not music. Keeps uploads short on a throttled connection. */
        const val BITRATE = 32_000
        const val SAMPLE_RATE = 22_050
    }
}
