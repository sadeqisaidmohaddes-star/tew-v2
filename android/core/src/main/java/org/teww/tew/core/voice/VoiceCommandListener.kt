package org.teww.tew.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.teww.tew.core.playback.FeedCommand

/** What the voice route is doing, so a screen can say it out loud. */
sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Heard(val text: String, val command: FeedCommand?) : VoiceState
    data class Unavailable(val spoken: String) : VoiceState
}

/**
 * The voice route for feed actions.
 *
 * ## Push-to-talk, never always-listening
 *
 * This only records when [startListening] is called, and stops at the end of
 * an utterance. That is a product decision, not a technical limitation.
 *
 * Non-negotiable #7 says voice is biometric data. An always-listening
 * microphone in an app whose users are disproportionately at home, alone, and
 * relying on audio would be continuously capturing a private space — exactly
 * the thing that principle exists to prevent. A wake word would be the same
 * problem with a filter in front of it.
 *
 * The cost is that the user has to start it. That is paid back by making the
 * route reachable three ways itself: an on-screen button, a screen-reader
 * action, and a media-button long-press if a headset is attached.
 *
 * ## When speech recognition is not available
 *
 * On the target device — a three-year-old budget phone, often without Google
 * services in the markets TEWW aims at — [SpeechRecognizer.isRecognitionAvailable]
 * can be false. That is reported as [VoiceState.Unavailable] with a spoken
 * sentence rather than failing silently, because a user who has been told this
 * route exists deserves to know it does not work on their phone.
 */
class VoiceCommandListener(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var onCommand: ((FeedCommand) -> Unit)? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun setCommandHandler(handler: ((FeedCommand) -> Unit)?) {
        onCommand = handler
    }

    fun startListening() {
        if (!isAvailable) {
            _state.value = VoiceState.Unavailable(
                "Voice control is not available on this phone. " +
                    "The buttons and your screen reader's actions menu still work.",
            )
            return
        }

        val speechRecognizer = recognizer ?: SpeechRecognizer
            .createSpeechRecognizer(appContext)
            .also { it.setRecognitionListener(listener); recognizer = it }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            // Prefer on-device where the platform supports it: a memo feed for
            // BLV users in the Global South should not require a round trip to
            // recognise the word "skip", and it keeps the audio off a server.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        _state.value = VoiceState.Listening
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        _state.value = VoiceState.Idle
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
        onCommand = null
        _state.value = VoiceState.Idle
    }

    private val listener = object : RecognitionListener {

        override fun onResults(results: Bundle?) {
            val candidates = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                .orEmpty()

            // Take the first candidate that maps to something we know, not
            // simply the highest-confidence one. The recogniser ranks by
            // acoustic likelihood and has no idea which words this app can
            // act on, so its favourite is often an unusable near-miss.
            val match = candidates.firstNotNullOfOrNull { text ->
                parseVoiceCommand(text)?.let { text to it }
            }

            if (match != null) {
                _state.value = VoiceState.Heard(match.first, match.second)
                onCommand?.invoke(match.second)
            } else {
                _state.value = VoiceState.Heard(candidates.firstOrNull().orEmpty(), null)
            }
        }

        override fun onError(error: Int) {
            _state.value = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                -> VoiceState.Heard("", null)

                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceState.Unavailable(
                    "Voice control needs permission to use the microphone.",
                )

                else -> VoiceState.Unavailable(
                    "Voice control is not working right now. " +
                        "The buttons and your screen reader's actions menu still work.",
                )
            }
        }

        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceState.Listening
        }

        override fun onEndOfSpeech() = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private companion object {
        const val MAX_RESULTS = 5
    }
}
