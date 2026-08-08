package org.teww.tew.feature.carddeck.spike

// Pure classification logic for the TalkBack/gesture-passthrough spike.
//
// Deliberately free of Android types so it can be unit-tested on the JVM.
// The spike's empirical input has to come from a real device with TalkBack
// on (see SPIKE.md), but the rule that turns a recorded event stream into a
// verdict is testable here, and is the part most likely to be misread later.

/** How an input event reached the app window. */
enum class InputKind {
    /** ACTION_DOWN / MOVE / UP / CANCEL — a real touch stream. */
    RAW_TOUCH,

    /** ACTION_HOVER_* — synthesized by touch exploration, not a real touch. */
    HOVER,
}

/** One event observed at the app window, in arrival order. */
data class ProbeEvent(
    val kind: InputKind,
    val action: String,
    val uptimeMillis: Long,
)

/**
 * What the recorded stream says about whether the card-deck gesture surface
 * can actually receive gestures while TalkBack is running.
 */
enum class PassthroughVerdict {
    /** DOWN through UP arrived. Raw gestures reach the app — mechanism holds. */
    RAW_TOUCH_COMPLETED,

    /**
     * DOWN arrived, then CANCEL, with no UP. The app saw the start of the
     * gesture and then lost the stream — the signature of the accessibility
     * input filter taking the gesture over mid-flight.
     */
    RAW_TOUCH_CANCELLED,

    /** Only exploration hovers arrived. TalkBack owns the touch stream. */
    ONLY_HOVER,

    /** Nothing reached the app window at all. */
    NO_INPUT,
}

/** Action name constants, matching MotionEvent's actionMasked names. */
object ProbeActions {
    const val DOWN = "DOWN"
    const val MOVE = "MOVE"
    const val UP = "UP"
    const val CANCEL = "CANCEL"
    const val HOVER_ENTER = "HOVER_ENTER"
    const val HOVER_MOVE = "HOVER_MOVE"
    const val HOVER_EXIT = "HOVER_EXIT"
}

/**
 * Reduce an observed event stream to a verdict.
 *
 * Order matters: a stream that reached UP is a pass even if hovers also
 * appeared (TalkBack emits hovers alongside a passed-through gesture), and a
 * cancelled stream is reported distinctly from one that never arrived —
 * "cancelled" means the takeover happened, "no input" can also just mean
 * nobody touched the screen, which is not the same finding.
 */
fun verdictFor(events: List<ProbeEvent>): PassthroughVerdict {
    val touches = events.filter { it.kind == InputKind.RAW_TOUCH }
    val sawDown = touches.any { it.action == ProbeActions.DOWN }
    val sawUp = touches.any { it.action == ProbeActions.UP }
    val sawCancel = touches.any { it.action == ProbeActions.CANCEL }

    return when {
        sawDown && sawUp -> PassthroughVerdict.RAW_TOUCH_COMPLETED
        sawDown && sawCancel -> PassthroughVerdict.RAW_TOUCH_CANCELLED
        touches.isNotEmpty() -> PassthroughVerdict.RAW_TOUCH_CANCELLED
        events.any { it.kind == InputKind.HOVER } -> PassthroughVerdict.ONLY_HOVER
        else -> PassthroughVerdict.NO_INPUT
    }
}

/**
 * Plain-language reading of a verdict, shown on screen and spoken aloud.
 *
 * The spike is meant to be run by someone who is themselves blind, so the
 * result has to be legible without reading logcat.
 */
fun verdictSummary(verdict: PassthroughVerdict, touchExplorationOn: Boolean): String = when {
    !touchExplorationOn ->
        "Touch exploration is OFF. This run proves nothing about TalkBack — " +
            "turn TalkBack on and repeat."

    verdict == PassthroughVerdict.RAW_TOUCH_COMPLETED ->
        "Raw gestures REACHED the app with TalkBack on. The card-deck " +
            "gesture surface is viable as designed."

    verdict == PassthroughVerdict.RAW_TOUCH_CANCELLED ->
        "Gesture was TAKEN OVER mid-flight: the app saw the start, then the " +
            "stream was cancelled. The card-deck surface cannot rely on raw touch."

    verdict == PassthroughVerdict.ONLY_HOVER ->
        "Only exploration hovers reached the app. TalkBack owns the touch " +
            "stream. The card-deck surface cannot rely on raw touch."

    else ->
        "No input reached the app. Either nothing was touched, or the " +
            "gesture surface never received the stream."
}
