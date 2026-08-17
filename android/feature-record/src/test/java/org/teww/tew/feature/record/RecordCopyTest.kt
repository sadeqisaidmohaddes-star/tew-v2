package org.teww.tew.feature.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.record.RecordingState
import java.io.File

/**
 * This screen is operated entirely by ear, so the spoken copy *is* the
 * interface. These tests cover it as behaviour, not decoration.
 */
class RecordCopyTest {

    @Test
    fun `durations are spoken the way a person would say them`() {
        assertEquals("0 seconds", spokenDuration(0))
        assertEquals("1 second", spokenDuration(1_000))
        assertEquals("9 seconds", spokenDuration(9_400))
        assertEquals("1 minute", spokenDuration(60_000))
        assertEquals("1 minute 5 seconds", spokenDuration(65_000))
        assertEquals("2 minutes", spokenDuration(120_000))
        assertEquals("2 minutes 30 seconds", spokenDuration(150_000))
    }

    @Test
    fun `negative durations do not produce nonsense`() {
        // Clock skew between start and stop should never make the app say
        // "minus three seconds" to someone who cannot see the screen.
        assertEquals("0 seconds", spokenDuration(-5_000))
    }

    @Test
    fun `recording is stated, and the elapsed time is printed beside it`() {
        // The single most important thing this screen says. Without a running
        // number, a user cannot tell a live recorder from a frozen one — but
        // the number is printed, not announced, so the two live apart.
        val spoken = spokenStatus(RecordingState.Recording(7_000), isReply = false)
        val printed = recordingElapsed(RecordingState.Recording(7_000))

        assertTrue(spoken.contains("Recording"))
        assertTrue(printed.contains("7 seconds"))
    }

    @Test
    fun `the spoken line carries no count, so a live region cannot loop on it`() {
        // The bug this split exists to fix: an assertive live region re-announces
        // on every change and interrupts itself. If the spoken string ever picks
        // the count back up, TalkBack cuts itself off once a second for the
        // whole length of the memo.
        val first = spokenStatus(RecordingState.Recording(7_000), isReply = false)
        val later = spokenStatus(RecordingState.Recording(8_000), isReply = false)

        assertEquals(first, later)
    }

    @Test
    fun `the printed count is absent when nothing is recording`() {
        assertEquals("", recordingElapsed(RecordingState.Idle))
        assertEquals("", recordingElapsed(RecordingState.Finished(File("x"), 1_000)))
    }

    @Test
    fun `a long memo is nudged, not cut off`() {
        val long = spokenStatus(RecordingState.Recording(120_000), isReply = false)
        val short = spokenStatus(RecordingState.Recording(5_000), isReply = false)

        assertTrue(long.contains("getting long"))
        assertFalse(short.contains("getting long"))
        // Still recording — the nudge must never imply it stopped.
        assertTrue(long.contains("Recording"))
    }

    @Test
    fun `idle explains what to do next`() {
        val status = spokenStatus(RecordingState.Idle, isReply = false)

        assertTrue(status.contains("Start recording"))
    }

    @Test
    fun `reply and memo are named differently`() {
        assertTrue(spokenStatus(RecordingState.Idle, isReply = true).contains("reply"))
        assertTrue(spokenStatus(RecordingState.Idle, isReply = false).contains("memo"))
    }

    @Test
    fun `finished offers all three ways forward`() {
        val status = spokenStatus(
            RecordingState.Finished(File("x.m4a"), 12_000),
            isReply = false,
        )

        assertTrue(status.contains("12 seconds"))
        assertTrue(status.contains("Listen back"))
        assertTrue(status.contains("send"))
        assertTrue(status.contains("again"))
    }

    @Test
    fun `a failure speaks its own reason`() {
        val status = spokenStatus(
            RecordingState.Failed("Recording could not start."),
            isReply = false,
        )

        assertEquals("Recording could not start.", status)
    }

    @Test
    fun `send is offered only once something has been recorded`() {
        assertFalse(canSend(RecordingState.Idle))
        assertFalse(canSend(RecordingState.Recording(1_000)))
        assertFalse(canSend(RecordingState.Failed("no")))
        assertTrue(canSend(RecordingState.Finished(File("x"), 1_000)))
    }

    @Test
    fun `every recording state says something`() {
        // Silence on this screen is indistinguishable from the app being dead.
        val states = listOf(
            RecordingState.Idle,
            RecordingState.Recording(0),
            RecordingState.Finished(File("x"), 1),
            RecordingState.Failed("problem"),
        )

        states.forEach {
            assertTrue("$it was silent", spokenStatus(it, false).isNotBlank())
        }
    }
}
