package org.teww.tew.feature.carddeck

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.teww.tew.core.accessibility.screenReaderActive

/**
 * The card deck's on-device narrator.
 *
 * `android/README.md` puts a narrator in this module and specifies Android's
 * native [TextToSpeech] rather than a server round trip — a network hop before
 * the user hears anything would blow the 100ms feedback budget on its own.
 *
 * ## Why it goes quiet when a screen reader is running
 *
 * This is the part worth understanding before changing it. TalkBack is already
 * speech. If the card deck also narrates, two voices talk over each other and
 * neither is intelligible — and the app's voice cannot be silenced by the
 * user's normal screen-reader controls, so it is worse than useless.
 *
 * So [shouldNarrate] is false whenever a screen reader is active, and all
 * narration in this module goes through it. The spoken content is not lost:
 * the same words are on the composables as semantics and live regions, which
 * is TalkBack's job to read. The narrator exists for people who want the deck
 * spoken *without* running a screen reader — the low-vision half of BLV, and
 * sighted testers moderating a session.
 *
 * Non-negotiable #2 is *voice-first, never voice-only*, and this is the other
 * side of it: never voice-twice either.
 *
 * ## Why [say] takes a callback
 *
 * The 2026-08-16 device session found the narrator and the memo's own audio
 * speaking at once, because the deck announced a card and started its audio in
 * the same breath. Nothing arbitrated between them: this class requests no
 * audio focus, so a playing memo never hears about it.
 *
 * The fix is order, not volume — the memo starts when the announcement has
 * *finished*, which only the engine can tell us. `onSpoken` is that signal.
 * Deliberately it is **not** called when a screen reader is running: there is
 * no equivalent signal for TalkBack, so under a screen reader nothing starts
 * on its own at all and the person presses play. See `STATE.md`.
 */
class Narrator(context: Context) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null

    /** The engine's init callback has come back, whether or not it succeeded. */
    @Volatile
    private var initialised = false

    /** ...and it succeeded, so there is actually a voice available. */
    @Volatile
    private var ready = false

    /** `onSpoken` callbacks for utterances in flight, keyed by utterance id. */
    private val waiting = ConcurrentHashMap<String, () -> Unit>()

    /** A [say] that arrived before the engine finished starting up. */
    private var queued: (() -> Unit)? = null

    private var nextUtteranceId = 0L

    /**
     * False while a screen reader is speaking for us. Re-read on each call
     * rather than cached, because the user can switch TalkBack on or off
     * without leaving the app.
     */
    val shouldNarrate: Boolean
        get() = !screenReaderActive(appContext)

    fun start() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            main.post {
                initialised = true
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.language = Locale.getDefault()
                    tts?.setOnUtteranceProgressListener(progress)
                }
                val pending = queued
                queued = null
                pending?.invoke()
            }
        }
    }

    /**
     * Say [text], interrupting whatever was being said, and call [onSpoken]
     * once it has finished.
     *
     * Interrupting is right for this screen: the previous sentence described a
     * card the user has already moved past, and making them wait through it
     * would put the feedback well outside the 100ms budget. An interrupted
     * utterance's [onSpoken] is dropped rather than called — the announcement
     * that replaced it owns what happens next.
     *
     * [onSpoken] runs on the main thread. It is called even when the engine
     * fails or is missing, because a caller waiting on it to start audio would
     * otherwise wait forever; the one case it is *not* called is a screen
     * reader being active, which is the point of the whole arrangement.
     */
    fun say(text: String, onSpoken: (() -> Unit)? = null) {
        if (!shouldNarrate) return
        if (text.isBlank()) { onSpoken?.invoke(); return }
        if (tts == null) { onSpoken?.invoke(); return }
        if (!initialised) { queued = { say(text, onSpoken) }; return }
        if (!ready) { onSpoken?.invoke(); return }

        val id = "carddeck-${nextUtteranceId++}"
        if (onSpoken != null) waiting[id] = onSpoken
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result != TextToSpeech.SUCCESS) {
            // The engine refused the utterance outright — no callback is
            // coming for it, so release the caller rather than strand it.
            waiting.remove(id)?.invoke()
        }
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialised = false
        ready = false
        queued = null
        waiting.clear()
    }

    @Suppress("DEPRECATION")
    private val progress = object : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = release(utteranceId)

        /** Abstract in the framework, so it has to be here even though it is deprecated. */
        override fun onError(utteranceId: String?) = release(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = release(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            // Flushed by a newer announcement. The newer one is now responsible
            // for what plays next, so this callback is dropped, not fired —
            // firing it would start the audio of a card the user has left.
            utteranceId?.let { waiting.remove(it) }
        }

        private fun release(utteranceId: String?) {
            val done = utteranceId?.let { waiting.remove(it) } ?: return
            main.post(done)
        }
    }
}
