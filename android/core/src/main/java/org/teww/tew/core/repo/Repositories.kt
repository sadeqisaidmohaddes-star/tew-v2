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
 * Reporting a memo, and everything to do with your own.
 *
 * Non-negotiable #8: moderation is visible and appealable. `android/README.md`
 * cuts the moderator's review queue from this build but keeps both of these —
 * they are the parts that face the person affected, and they are not optional.
 *
 * [deleteMemo] is here rather than on [FeedRepository] next to `postMemo`
 * because this is the "your own memos" repository in practice — [myMemos] is
 * the list the delete acts on, and the profile screen already has this one.
 */
interface ModerationRepository {

    suspend fun report(memoId: String, reason: ReportReason): TewResult<Unit>

    /** The signed-in user's own memos, including removed ones with reasons. */
    suspend fun myMemos(): TewResult<List<Memo>>

    suspend fun appeal(memoId: String, text: String): TewResult<Unit>

    /**
     * Delete your own memo. The recording is destroyed, not hidden.
     *
     * `STATE.md`: "Voice retention: until the person deletes it." This is the
     * half of that promise the app is responsible for — the backend has always
     * honoured it, and until this landed nothing in the app could ask.
     */
    suspend fun deleteMemo(memoId: String): TewResult<Unit>
}
