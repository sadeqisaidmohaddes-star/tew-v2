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
    fun `recording state always states the elapsed time`() {
        // The single most important thing this screen says. Without a running
        // number, a user cannot tell a live recorder from a frozen one.
        val status = recordingStatus(RecordingState.Recording(7_000), isReply = false)

        assertTrue(status.contains("Recording"))
        assertTrue(status.contains("7 seconds"))
    }

    @Test
    fun `a long memo is nudged, not cut off`() {
        val long = recordingStatus(RecordingState.Recording(120_000), isReply = false)
        val short = recordingStatus(RecordingState.Recording(5_000), isReply = false)

        assertTrue(long.contains("getting long"))
        assertFalse(short.contains("getting long"))
        // Still recording — the nudge must never imply it stopped.
        assertTrue(long.contains("Recording"))
    }

    @Test
    fun `idle explains what to do next`() {
        val status = recordingStatus(RecordingState.Idle, isReply = false)

        assertTrue(status.contains("Start recording"))
    }

    @Test
    fun `reply and memo are named differently`() {
        assertTrue(recordingStatus(RecordingState.Idle, isReply = true).contains("reply"))
        assertTrue(recordingStatus(RecordingState.Idle, isReply = false).contains("memo"))
    }

    @Test
    fun `finished offers all three ways forward`() {
        val status = recordingStatus(
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
        val status = recordingStatus(
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
            assertTrue("$it was silent", recordingStatus(it, false).isNotBlank())
        }
    }
}
