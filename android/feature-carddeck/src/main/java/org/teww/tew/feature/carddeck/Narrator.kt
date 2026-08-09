package org.teww.tew.feature.carddeck

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import java.util.Locale

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
 */
class Narrator(context: Context) {

    private val appContext = context.applicationContext
    private val accessibilityManager =
        appContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    /**
     * False while a screen reader is speaking for us. Re-read on each call
     * rather than cached, because the user can switch TalkBack on or off
     * without leaving the app.
     */
    val shouldNarrate: Boolean
        get() = !accessibilityManager.isTouchExplorationEnabled

    fun start() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts?.language = Locale.getDefault()
        }
    }

    /**
     * Say [text], interrupting whatever was being said.
     *
     * Interrupting is right for this screen: the previous sentence described a
     * card the user has already moved past, and making them wait through it
     * would put the feedback well outside the 100ms budget.
     */
    fun say(text: String) {
        if (!shouldNarrate || !ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "carddeck")
    }

    fun stop() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
