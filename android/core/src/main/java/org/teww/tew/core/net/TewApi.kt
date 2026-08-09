package org.teww.tew.core.net

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The backend API, as the Android client needs it.
 *
 * `backend/README.md` is still design-stage and names no endpoints, so this is
 * the client's proposal rather than a settled contract — see
 * `core/API_CONTRACT.md`. It is written to match the shape the backend README
 * *does* commit to: one cursor-paginated feed serving both feed models, with
 * no per-UI fork.
 */
interface TewApi {

    @GET("v1/me")
    suspend fun profile(): Response<ProfileDto>

    /**
     * One page of the feed. Omit [cursor] for the first page; the response's
     * `next_cursor` is null at the end of the stream.
     */
    @GET("v1/feed")
    suspend fun feed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
    ): Response<FeedPageDto>

    @POST("v1/memos/{id}/like")
    suspend fun like(@Path("id") memoId: String): Response<Unit>

    @DELETE("v1/memos/{id}/like")
    suspend fun unlike(@Path("id") memoId: String): Response<Unit>

    /**
     * Records that the listener moved past this memo. Not an engagement
     * signal and not used for ranking — BRIEF.md rules out a ranking
     * algorithm. It exists so the same memo is not served again.
     */
    @POST("v1/memos/{id}/skip")
    suspend fun skip(@Path("id") memoId: String): Response<Unit>

    @GET("v1/memos/{id}/comments")
    suspend fun comments(@Path("id") memoId: String): Response<CommentListDto>

    @Multipart
    @POST("v1/memos/{id}/comments")
    suspend fun postComment(
        @Path("id") memoId: String,
        @Part audio: MultipartBody.Part,
    ): Response<CommentDto>

    @Multipart
    @POST("v1/memos")
    suspend fun postMemo(
        @Part audio: MultipartBody.Part,
    ): Response<MemoDto>

    @POST("v1/memos/{id}/report")
    suspend fun report(
        @Path("id") memoId: String,
        @Body body: ReportRequest,
    ): Response<Unit>

    /** The signed-in user's own memos, including any that were removed. */
    @GET("v1/me/memos")
    suspend fun myMemos(): Response<MemoListDto>

    @POST("v1/memos/{id}/appeal")
    suspend fun appeal(
        @Path("id") memoId: String,
        @Body body: AppealRequest,
    ): Response<Unit>

    companion object {
        /**
         * Small on purpose. The target device is a three-year-old budget phone
         * on throttled 3G and the cold-start budget is three seconds to first
         * playable memo — a large first page spends that budget on memos
         * nobody has reached yet.
         */
        const val DEFAULT_PAGE_SIZE = 10
    }
}
