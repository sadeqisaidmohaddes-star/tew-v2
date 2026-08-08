package org.teww.tew.feature.carddeck.spike

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Content-view wrapper that records what kind of input actually arrives.
 *
 * This exists because the two streams the spike needs to tell apart are
 * dispatched through different paths: touch through [dispatchTouchEvent] and
 * exploration hovers through [dispatchHoverEvent]. Activity only exposes a
 * hook for the first, so observing both means sitting in the view tree.
 *
 * Nothing is consumed here — every event is passed straight through — so the
 * probe cannot itself change the behaviour it is measuring.
 */
internal class ProbeFrameLayout(context: Context) : FrameLayout(context) {

    var onProbe: ((InputKind, String) -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onProbe?.invoke(InputKind.RAW_TOUCH, touchActionName(ev.actionMasked))
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        onProbe?.invoke(InputKind.HOVER, hoverActionName(event.actionMasked))
        return super.dispatchHoverEvent(event)
    }

    private fun touchActionName(action: Int): String = when (action) {
        MotionEvent.ACTION_DOWN -> ProbeActions.DOWN
        MotionEvent.ACTION_MOVE -> ProbeActions.MOVE
        MotionEvent.ACTION_UP -> ProbeActions.UP
        MotionEvent.ACTION_CANCEL -> ProbeActions.CANCEL
        else -> "TOUCH_$action"
    }

    private fun hoverActionName(action: Int): String = when (action) {
        MotionEvent.ACTION_HOVER_ENTER -> ProbeActions.HOVER_ENTER
        MotionEvent.ACTION_HOVER_MOVE -> ProbeActions.HOVER_MOVE
        MotionEvent.ACTION_HOVER_EXIT -> ProbeActions.HOVER_EXIT
        else -> "HOVER_$action"
    }
}
