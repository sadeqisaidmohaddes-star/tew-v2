package org.teww.tew.feature.carddeck.spike

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureLatencyTest {

    private fun earcon(ms: Long) = LatencySample(FeedbackRoute.EARCON, ms)
    private fun speech(ms: Long) = LatencySample(FeedbackRoute.SPEECH, ms)

    @Test
    fun `budget matches the rule in IMPLEMENTATION`() {
        assertEquals(100L, GestureLatency.BUDGET_MS)
    }

    @Test
    fun `budget is exclusive at the boundary`() {
        assertTrue(GestureLatency.withinBudget(99))
        assertFalse(GestureLatency.withinBudget(100))
        assertFalse(GestureLatency.withinBudget(101))
    }

    @Test
    fun `unmeasured route summarises to null, not to a pass`() {
        val samples = listOf(earcon(10))

        assertNull(summarise(samples, FeedbackRoute.SPEECH))
        assertEquals("not measured", describeStats(null))
    }

    @Test
    fun `stats are computed per route`() {
        val samples = listOf(
            earcon(10), earcon(30), earcon(20),
            speech(300),
        )

        val earconStats = summarise(samples, FeedbackRoute.EARCON)!!
        assertEquals(3, earconStats.count)
        assertEquals(10L, earconStats.minMs)
        assertEquals(20L, earconStats.medianMs)
        assertEquals(30L, earconStats.maxMs)

        val speechStats = summarise(samples, FeedbackRoute.SPEECH)!!
        assertEquals(1, speechStats.count)
        assertEquals(300L, speechStats.maxMs)
    }

    @Test
    fun `median of an even sample count averages the middle pair`() {
        val samples = listOf(earcon(10), earcon(20), earcon(30), earcon(40))

        assertEquals(25L, summarise(samples, FeedbackRoute.EARCON)!!.medianMs)
    }

    @Test
    fun `the worst sample decides the verdict, not the median`() {
        // A user who gets silence on one swipe in ten has a broken interface,
        // so a good median must not be allowed to hide a bad outlier.
        val samples = listOf(
            earcon(5), earcon(6), earcon(7), earcon(8), earcon(250),
        )

        val stats = summarise(samples, FeedbackRoute.EARCON)!!
        assertTrue(stats.medianMs < GestureLatency.BUDGET_MS)
        assertFalse(stats.meetsBudget)
    }

    @Test
    fun `describe names the route and the outcome`() {
        val overBudget = summarise(listOf(speech(420)), FeedbackRoute.SPEECH)

        val text = describeStats(overBudget)
        assertTrue(text.contains("Speech"))
        assertTrue(text.contains("OVER"))
        assertTrue(text.contains("100ms"))
    }
}
