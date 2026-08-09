package org.teww.tew.core.repo

import org.teww.tew.core.TewResult
import org.teww.tew.core.model.Comment
import org.teww.tew.core.model.FeedPage
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.ReportReason
import java.io.File

/**
 * The feed, and the three things you can do with a memo.
 *
 * BRIEF.md fixes the interaction set at like, comment, skip. There is no
 * follow, no share, no repost and no bookmark, and adding one here is how that
 * decision would get quietly reversed.
 */
interface FeedRepository {

    /** Pass null for the first page; see [FeedPage.isLastPage] for the end. */
    suspend fun feed(cursor: String? = null): TewResult<FeedPage>

    suspend fun setLiked(memoId: String, liked: Boolean): TewResult<Unit>

    suspend fun skip(memoId: String): TewResult<Unit>

    suspend fun comments(memoId: String): TewResult<List<Comment>>

    suspend fun postComment(memoId: String, audio: File): TewResult<Comment>

    suspend fun postMemo(audio: File): TewResult<Memo>
}

/**
 * Reporting a memo, and seeing what happened to your own.
 *
 * Non-negotiable #8: moderation is visible and appealable. `android/README.md`
 * cuts the moderator's review queue from this build but keeps both of these —
 * they are the parts that face the person affected, and they are not optional.
 */
interface ModerationRepository {

    suspend fun report(memoId: String, reason: ReportReason): TewResult<Unit>

    /** The signed-in user's own memos, including removed ones with reasons. */
    suspend fun myMemos(): TewResult<List<Memo>>

    suspend fun appeal(memoId: String, text: String): TewResult<Unit>
}
