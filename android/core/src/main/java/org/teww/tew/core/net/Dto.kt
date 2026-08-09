package org.teww.tew.core.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.Comment
import org.teww.tew.core.model.FeedPage
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.model.ModerationState

// Wire types, kept separate from the domain models in `model/`. The split
// exists so a backend field rename doesn't ripple into two feature modules,
// and so the domain types can omit things the API happens to send.
//
// The contract these describe is documented in core/API_CONTRACT.md. The
// backend is still design-stage, so this is the Android side's proposal and
// needs agreeing before either side is built against it as settled.

@Serializable
data class MemoDto(
    val id: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("posted_at") val postedAtEpochSeconds: Long,
    val transcript: String? = null,
    @SerialName("liked_by_me") val likedByMe: Boolean = false,
    val moderation: ModerationDto = ModerationDto(),
)

@Serializable
data class ModerationDto(
    val state: String = "visible",
    val reason: String? = null,
    val appeal: String = "not_applicable",
)

@Serializable
data class FeedPageDto(
    val memos: List<MemoDto> = emptyList(),
    /** Null or absent means the feed has ended — see FeedPage.isLastPage. */
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class CommentDto(
    val id: String,
    @SerialName("memo_id") val memoId: String,
    @SerialName("author_username") val authorUsername: String,
    @SerialName("audio_url") val audioUrl: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("posted_at") val postedAtEpochSeconds: Long,
    val transcript: String? = null,
)

@Serializable
data class CommentListDto(val comments: List<CommentDto> = emptyList())

@Serializable
data class MemoListDto(val memos: List<MemoDto> = emptyList())

@Serializable
data class ReportRequest(val reason: String)

@Serializable
data class AppealRequest(val text: String)

@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
)

// ---- Wire → domain ----------------------------------------------------

fun MemoDto.toDomain(): Memo = Memo(
    id = id,
    authorUsername = authorUsername,
    audioUrl = audioUrl,
    durationMs = durationMs,
    postedAtEpochSeconds = postedAtEpochSeconds,
    transcript = transcript,
    likedByMe = likedByMe,
    moderation = moderation.toDomain(),
)

fun ModerationDto.toDomain(): Moderation = Moderation(
    state = when (state.lowercase()) {
        "removed" -> ModerationState.REMOVED
        "under_review" -> ModerationState.UNDER_REVIEW
        else -> ModerationState.VISIBLE
    },
    reason = reason,
    appeal = when (appeal.lowercase()) {
        "available" -> AppealState.AVAILABLE
        "submitted" -> AppealState.SUBMITTED
        "upheld" -> AppealState.UPHELD
        "denied" -> AppealState.DENIED
        else -> AppealState.NOT_APPLICABLE
    },
)

fun FeedPageDto.toDomain(): FeedPage = FeedPage(
    memos = memos.map { it.toDomain() },
    nextCursor = nextCursor,
)

fun CommentDto.toDomain(): Comment = Comment(
    id = id,
    memoId = memoId,
    authorUsername = authorUsername,
    audioUrl = audioUrl,
    durationMs = durationMs,
    postedAtEpochSeconds = postedAtEpochSeconds,
    transcript = transcript,
)
