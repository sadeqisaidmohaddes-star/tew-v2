package org.teww.tew.core.model

/**
 * A voice memo — the only kind of post TEW has.
 *
 * Note what is deliberately absent: there is no like count, play count,
 * comment count or any other tally. Non-negotiable #4 is *no engagement
 * machinery*, and BRIEF.md rules out engagement metrics explicitly. [likedByMe]
 * exists because a user needs to know whether their own like registered; a
 * total does not exist because nobody needs to know how popular a memo is, and
 * a number on a screen is how that stops being true.
 */
data class Memo(
    val id: String,
    val authorUsername: String,
    val audioUrl: String,
    val durationMs: Long,
    val postedAtEpochSeconds: Long,
    /** ASR transcript from the backend, when one has been generated. */
    val transcript: String?,
    val likedByMe: Boolean,
    val moderation: Moderation,
)

/** A voice reply. Comments are memos too — audio is the medium, not a fallback. */
data class Comment(
    val id: String,
    val memoId: String,
    val authorUsername: String,
    val audioUrl: String,
    val durationMs: Long,
    val postedAtEpochSeconds: Long,
    val transcript: String?,
)

/**
 * One page of the feed.
 *
 * [nextCursor] being null is load-bearing: non-negotiable #3 says *the stream
 * ends*. A null cursor is the end of the feed and the UI is expected to say so
 * out loud, not silently stop or loop back to the top.
 */
data class FeedPage(
    val memos: List<Memo>,
    val nextCursor: String?,
) {
    val isLastPage: Boolean get() = nextCursor == null
}

/**
 * Moderation state of a memo, from the perspective of whoever asked for it.
 *
 * Non-negotiable #8: moderation is visible and appealable. That means a
 * removed memo does not simply vanish from its author's own view — it comes
 * back with a reason attached and a route to challenge it.
 */
data class Moderation(
    val state: ModerationState,
    /** Plain-language reason, written to be read aloud. Null unless removed. */
    val reason: String?,
    val appeal: AppealState,
) {
    companion object {
        val visible = Moderation(ModerationState.VISIBLE, null, AppealState.NOT_APPLICABLE)
    }
}

enum class ModerationState {
    VISIBLE,
    UNDER_REVIEW,
    REMOVED,
}

enum class AppealState {
    /** Nothing to appeal — the memo is fine. */
    NOT_APPLICABLE,

    /** Removed, and the author has not appealed yet. */
    AVAILABLE,

    /** Appeal sent, awaiting a decision. */
    SUBMITTED,

    /** Appeal succeeded — the memo is back. */
    UPHELD,

    /** Appeal considered and refused. */
    DENIED,
}

/** Why a memo is being reported. Deliberately short — a long list is a burden. */
enum class ReportReason {
    HARASSMENT,
    HATE_SPEECH,
    SEXUAL_CONTENT,
    SPAM,
    SOMETHING_ELSE,
}
