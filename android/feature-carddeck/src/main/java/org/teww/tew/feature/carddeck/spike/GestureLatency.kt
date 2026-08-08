package org.teww.tew.feature.carddeck.spike

// Latency bookkeeping for the spike. Pure Kotlin so it unit-tests on the JVM.
//
// IMPLEMENTATION.md fixes one hard performance rule: gesture/tap to audible
// feedback must be under 100ms on a 3-year-old budget phone. This file holds
// the budget and the summary maths; the measuring itself is in the Activity.

/**
 * Which audible route was measured.
 *
 * These are separated because they are expected to behave very differently,
 * and conflating them would hide the finding: a short earcon can plausibly
 * land inside the budget, while speech synthesis usually cannot.
 */
enum class FeedbackRoute {
    /** Short non-speech tone — the fast acknowledgement channel. */
    EARCON,

    /** TextToSpeech utterance — the informative, slower channel. */
    SPEECH,
}

/** One measured gesture-to-audio interval. */
data class LatencySample(
    val route: FeedbackRoute,
    val millis: Long,
)

object GestureLatency {
    /** IMPLEMENTATION.md's hard rule, in milliseconds. */
    const val BUDGET_MS: Long = 100

    fun withinBudget(millis: Long): Boolean = millis < BUDGET_MS
}

/** Min / median / max / worst-case view of a set of samples for one route. */
data class LatencyStats(
    val route: FeedbackRoute,
    val count: Int,
    val minMs: Long,
    val medianMs: Long,
    val maxMs: Long,
) {
    /**
     * The budget is a per-gesture guarantee, not an average — a user who gets
     * silence on one swipe in ten has a broken interface, so the worst case is
     * what decides the verdict.
     */
    val meetsBudget: Boolean get() = GestureLatency.withinBudget(maxMs)
}

/**
 * Summarise samples for one route. Returns null when there is nothing measured
 * for that route — an empty set is "unmeasured", never "passing".
 */
fun summarise(samples: List<LatencySample>, route: FeedbackRoute): LatencyStats? {
    val forRoute = samples.filter { it.route == route }.map { it.millis }.sorted()
    if (forRoute.isEmpty()) return null

    val median = if (forRoute.size % 2 == 1) {
        forRoute[forRoute.size / 2]
    } else {
        (forRoute[forRoute.size / 2 - 1] + forRoute[forRoute.size / 2]) / 2
    }

    return LatencyStats(
        route = route,
        count = forRoute.size,
        minMs = forRoute.first(),
        medianMs = median,
        maxMs = forRoute.last(),
    )
}

/** One line per route, in plain language, for on-screen and spoken output. */
fun describeStats(stats: LatencyStats?): String {
    if (stats == null) return "not measured"
    val routeName = when (stats.route) {
        FeedbackRoute.EARCON -> "Earcon"
        FeedbackRoute.SPEECH -> "Speech"
    }
    val verdict = if (stats.meetsBudget) "within" else "OVER"
    return "$routeName: ${stats.count} samples, min ${stats.minMs}ms, " +
        "median ${stats.medianMs}ms, worst ${stats.maxMs}ms — " +
        "$verdict the ${GestureLatency.BUDGET_MS}ms budget."
}
