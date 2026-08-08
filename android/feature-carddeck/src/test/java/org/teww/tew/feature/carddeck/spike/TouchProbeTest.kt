package org.teww.tew.feature.carddeck.spike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The empirical half of this spike needs a real device with TalkBack on. The
 * half that can be pinned down here is the rule that turns a recorded event
 * stream into a verdict — which is also the half most likely to be misread
 * later, because "the app got nothing" and "the app got the gesture and then
 * had it taken away" look similar in a log and mean different things.
 */
class TouchProbeTest {

    private fun touch(action: String, at: Long = 0) =
        ProbeEvent(InputKind.RAW_TOUCH, action, at)

    private fun hover(action: String, at: Long = 0) =
        ProbeEvent(InputKind.HOVER, action, at)

    @Test
    fun `complete touch stream means passthrough works`() {
        val events = listOf(
            touch(ProbeActions.DOWN, 10),
            touch(ProbeActions.MOVE, 20),
            touch(ProbeActions.MOVE, 30),
            touch(ProbeActions.UP, 40),
        )

        assertEquals(PassthroughVerdict.RAW_TOUCH_COMPLETED, verdictFor(events))
    }

    @Test
    fun `down then cancel is a takeover, not a silence`() {
        val events = listOf(
            touch(ProbeActions.DOWN, 10),
            touch(ProbeActions.MOVE, 20),
            touch(ProbeActions.CANCEL, 25),
        )

        assertEquals(PassthroughVerdict.RAW_TOUCH_CANCELLED, verdictFor(events))
    }

    @Test
    fun `hover only means touch exploration owns the stream`() {
        val events = listOf(
            hover(ProbeActions.HOVER_ENTER, 10),
            hover(ProbeActions.HOVER_MOVE, 20),
            hover(ProbeActions.HOVER_EXIT, 30),
        )

        assertEquals(PassthroughVerdict.ONLY_HOVER, verdictFor(events))
    }

    @Test
    fun `empty stream is reported as no input, never as a pass`() {
        assertEquals(PassthroughVerdict.NO_INPUT, verdictFor(emptyList()))
    }

    @Test
    fun `hovers alongside a completed touch still count as a pass`() {
        // TalkBack emits exploration hovers around a gesture it passes
        // through, so their presence must not mask a successful stream.
        val events = listOf(
            hover(ProbeActions.HOVER_ENTER, 5),
            touch(ProbeActions.DOWN, 10),
            touch(ProbeActions.UP, 40),
            hover(ProbeActions.HOVER_EXIT, 45),
        )

        assertEquals(PassthroughVerdict.RAW_TOUCH_COMPLETED, verdictFor(events))
    }

    @Test
    fun `partial touch without up or cancel is not treated as a pass`() {
        val events = listOf(
            touch(ProbeActions.DOWN, 10),
            touch(ProbeActions.MOVE, 20),
        )

        assertEquals(PassthroughVerdict.RAW_TOUCH_CANCELLED, verdictFor(events))
    }

    @Test
    fun `summary refuses to draw a conclusion when talkback is off`() {
        val summary = verdictSummary(
            verdict = PassthroughVerdict.RAW_TOUCH_COMPLETED,
            touchExplorationOn = false,
        )

        // A clean pass with TalkBack off is exactly the false positive this
        // spike exists to avoid reporting.
        assertTrue(summary.contains("proves nothing"))
    }

    @Test
    fun `summary reports a pass when talkback is on`() {
        val summary = verdictSummary(
            verdict = PassthroughVerdict.RAW_TOUCH_COMPLETED,
            touchExplorationOn = true,
        )

        assertTrue(summary.contains("REACHED"))
    }
}
