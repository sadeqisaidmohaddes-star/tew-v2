package org.teww.tew.core.voice

import org.teww.tew.core.playback.FeedCommand

/**
 * Turns what someone said into a [FeedCommand].
 *
 * Pure and separate from the recogniser so the vocabulary can be unit-tested
 * without a device or a network. That matters more than usual here: this is the
 * route a user reaches for when they cannot find a control, so a phrase that
 * silently fails to match is a dead end rather than an inconvenience.
 *
 * Matching is deliberately loose — substring, case-insensitive, several
 * synonyms per command. Speech recognition returns approximations, people
 * phrase things differently, and non-negotiable #6's spirit ("no precise
 * input") applies to speaking as much as to gestures. The cost of a loose
 * match is occasionally doing the wrong reversible thing; the cost of a strict
 * one is a route that does not work.
 */
fun parseVoiceCommand(spoken: String?): FeedCommand? {
    val text = spoken?.lowercase()?.trim() ?: return null
    if (text.isEmpty()) return null

    // Order matters: "play again" must be read as REPLAY, not PLAY_PAUSE.
    return when {
        text.containsAny("again", "repeat", "replay", "once more") -> FeedCommand.REPLAY
        text.containsAny("like", "love", "heart", "yes") -> FeedCommand.LIKE
        text.containsAny("skip", "next", "no thanks", "pass", "move on") -> FeedCommand.SKIP
        text.containsAny("reply", "respond", "answer", "comment") -> FeedCommand.REPLY
        text.containsAny("report", "flag") -> FeedCommand.REPORT
        text.containsAny("pause", "stop", "play", "resume") -> FeedCommand.PLAY_PAUSE
        else -> null
    }
}

private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { this.contains(it) }

/** What the user is told the voice route understands. Spoken during onboarding. */
fun voiceVocabularyHelp(): String =
    "You can say: like, skip, again, reply, report, or pause."

/** What is said when nothing matched — never silence. */
fun voiceNotUnderstood(spoken: String?): String = if (spoken.isNullOrBlank()) {
    "I did not catch that. ${voiceVocabularyHelp()}"
} else {
    "I heard \"$spoken\", which I do not know. ${voiceVocabularyHelp()}"
}
