package org.teww.tew.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.playback.FeedCommand
import org.teww.tew.core.playback.FeedCommandBus

class VoiceCommandsTest {

    @Test
    fun `the plain words work`() {
        assertEquals(FeedCommand.LIKE, parseVoiceCommand("like"))
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("skip"))
        assertEquals(FeedCommand.REPLAY, parseVoiceCommand("again"))
        assertEquals(FeedCommand.REPLY, parseVoiceCommand("reply"))
        assertEquals(FeedCommand.REPORT, parseVoiceCommand("report"))
        assertEquals(FeedCommand.PLAY_PAUSE, parseVoiceCommand("pause"))
    }

    @Test
    fun `recognition returns sentences, not single words`() {
        // A recogniser hands back what it heard, and people speak in phrases.
        // Matching only exact single words would make this route look broken.
        assertEquals(FeedCommand.LIKE, parseVoiceCommand("I like this one"))
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("please skip this"))
        assertEquals(FeedCommand.REPORT, parseVoiceCommand("report this memo"))
    }

    @Test
    fun `case and whitespace do not matter`() {
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("  SKIP  "))
        assertEquals(FeedCommand.LIKE, parseVoiceCommand("Like"))
    }

    @Test
    fun `play again is replay, not play-pause`() {
        // Ordering bug this test exists to catch: "play again" contains "play",
        // so a naive match sends it to PLAY_PAUSE and pauses the memo the user
        // just asked to hear again.
        assertEquals(FeedCommand.REPLAY, parseVoiceCommand("play again"))
        assertEquals(FeedCommand.REPLAY, parseVoiceCommand("say that again"))
        assertEquals(FeedCommand.REPLAY, parseVoiceCommand("repeat that"))
    }

    @Test
    fun `synonyms people actually use are accepted`() {
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("next"))
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("no thanks"))
        assertEquals(FeedCommand.SKIP, parseVoiceCommand("move on"))
        assertEquals(FeedCommand.LIKE, parseVoiceCommand("yes"))
        assertEquals(FeedCommand.REPLY, parseVoiceCommand("I want to respond"))
    }

    @Test
    fun `nothing heard is not a command`() {
        assertNull(parseVoiceCommand(null))
        assertNull(parseVoiceCommand(""))
        assertNull(parseVoiceCommand("   "))
    }

    @Test
    fun `unknown speech is not silently mapped to something destructive`() {
        // Guessing here would mean a stray remark skips or reports a memo.
        assertNull(parseVoiceCommand("what is the weather"))
        assertNull(parseVoiceCommand("hello grandma"))
    }

    @Test
    fun `a failure to understand is always spoken`() {
        assertTrue(voiceNotUnderstood(null).contains("did not catch"))
        assertTrue(voiceNotUnderstood("banana").contains("banana"))
        // Every failure message tells the user what they could say instead —
        // a dead end with no way forward is the worst outcome for this route.
        assertTrue(voiceNotUnderstood(null).contains("You can say"))
        assertTrue(voiceNotUnderstood("banana").contains("You can say"))
    }

    @Test
    fun `every command in the enum has at least one phrase that reaches it`() {
        // Guards against adding a FeedCommand and forgetting the voice route,
        // which would silently make #5's three-route rule false for it.
        val reachable = FeedCommand.entries.filter { command ->
            listOf(
                "like", "skip", "again", "reply", "report", "pause",
            ).any { parseVoiceCommand(it) == command }
        }

        assertEquals(FeedCommand.entries.toSet(), reachable.toSet())
    }
}

class FeedCommandBusTest {

    @Test
    fun `commands reach the registered handler`() {
        val bus = FeedCommandBus()
        val received = mutableListOf<FeedCommand>()
        bus.setHandler { received += it }

        assertTrue(bus.dispatch(FeedCommand.LIKE))

        assertEquals(listOf(FeedCommand.LIKE), received)
    }

    @Test
    fun `dispatching with nobody listening reports the drop`() {
        // Returning false lets the caller say something rather than have a
        // headset button do nothing with no explanation.
        assertFalse(FeedCommandBus().dispatch(FeedCommand.SKIP))
    }

    @Test
    fun `a new handler replaces the old one`() {
        // Only one feed screen is visible at a time. Delivering to a screen
        // that has been toggled away would like or skip the wrong memo.
        val bus = FeedCommandBus()
        val first = mutableListOf<FeedCommand>()
        val second = mutableListOf<FeedCommand>()

        bus.setHandler { first += it }
        bus.setHandler { second += it }
        bus.dispatch(FeedCommand.SKIP)

        assertTrue(first.isEmpty())
        assertEquals(listOf(FeedCommand.SKIP), second)
    }

    @Test
    fun `clearing the handler stops delivery`() {
        val bus = FeedCommandBus()
        bus.setHandler { }
        bus.setHandler(null)

        assertFalse(bus.dispatch(FeedCommand.LIKE))
    }
}
