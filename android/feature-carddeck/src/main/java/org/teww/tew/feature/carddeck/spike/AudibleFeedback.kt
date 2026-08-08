package org.teww.tew.feature.carddeck.spike

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.concurrent.ConcurrentHashMap

/**
 * The two audible routes the card deck would use to acknowledge a gesture,
 * each timed from the moment the gesture was recognised.
 *
 * What these numbers are, precisely, matters for how they get read:
 *
 * - EARCON is measured from gesture recognition to the moment [ToneGenerator]
 *   accepts the tone. That is the app handing audio to the framework mixer,
 *   NOT the sound arriving at the user's ear. Output-path latency beyond that
 *   point (mixer, HAL, DAC, Bluetooth if used) is invisible from inside the
 *   process and can be tens of milliseconds on its own, more over Bluetooth.
 * - SPEECH is measured to [UtteranceProgressListener.onStart], which the
 *   engine raises as it begins the utterance — again the start of output, not
 *   an acoustic measurement.
 *
 * So both figures are lower bounds on true gesture-to-ear latency. A real
 * conformance check against IMPLEMENTATION.md's 100ms rule needs external
 * measurement (high-frame-rate capture, or an audio loopback rig) on the
 * actual budget device. These in-process numbers are useful for catching a
 * route that is already over budget before the output path is even counted —
 * they cannot certify that a route is under it.
 */
class AudibleFeedback(
    context: Context,
    private val onSample: (LatencySample) -> Unit,
) {
    private val appContext = context.applicationContext

    private var toneGenerator: ToneGenerator? = null
    private var tts: TextToSpeech? = null

    @Volatile
    private var ttsReady = false

    /** utteranceId -> gesture recognition time, so onStart can compute a delta. */
    private val pendingUtterances = ConcurrentHashMap<String, Long>()

    private var utteranceCounter = 0

    fun start() {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
        } catch (e: RuntimeException) {
            // Some devices refuse a second ToneGenerator when one is already
            // held elsewhere. Earcon timing is then unavailable, which the UI
            // reports as "not measured" rather than as a pass.
            null
        }

        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val startedAt = SystemClock.uptimeMillis()
                    val gestureAt = pendingUtterances.remove(utteranceId) ?: return
                    onSample(LatencySample(FeedbackRoute.SPEECH, startedAt - gestureAt))
                }

                override fun onDone(utteranceId: String?) {
                    pendingUtterances.remove(utteranceId)
                }

                @Deprecated("Required by the abstract base class")
                override fun onError(utteranceId: String?) {
                    pendingUtterances.remove(utteranceId)
                }
            })
        }
    }

    /**
     * Fire the fast acknowledgement tone and record how long it took to get
     * there from [gestureUptimeMillis].
     */
    fun earcon(gestureUptimeMillis: Long) {
        val generator = toneGenerator ?: return
        generator.startTone(ToneGenerator.TONE_PROP_BEEP, EARCON_DURATION_MS)
        onSample(
            LatencySample(
                route = FeedbackRoute.EARCON,
                millis = SystemClock.uptimeMillis() - gestureUptimeMillis,
            ),
        )
    }

    /**
     * Speak [text], timing to the engine's utterance start. Silently does
     * nothing until the engine has initialised — an unmeasured gesture is
     * better than a fabricated sample.
     */
    fun speak(text: String, gestureUptimeMillis: Long) {
        val engine = tts?.takeIf { ttsReady } ?: return
        val id = "spike-${utteranceCounter++}"
        pendingUtterances[id] = gestureUptimeMillis
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    /** Speak without timing — for status text the user asked to hear again. */
    fun announce(text: String) {
        val engine = tts?.takeIf { ttsReady } ?: return
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun stop() {
        toneGenerator?.release()
        toneGenerator = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        pendingUtterances.clear()
    }

    private companion object {
        const val TONE_VOLUME = 80
        const val EARCON_DURATION_MS = 60
    }
}
