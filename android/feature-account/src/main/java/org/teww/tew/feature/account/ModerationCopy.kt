package org.teww.tew.feature.account

import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.ModerationState
import org.teww.tew.core.model.ReportReason

/**
 * The words a user hears about moderation.
 *
 * Pure functions, in their own file, because this is the copy that decides
 * whether non-negotiable #8 — *moderation is visible and appealable* — is
 * actually met. A removed memo that says nothing, or says only "content
 * violation", is technically visible and practically not. These are unit-tested
 * for the same reason: wording is behaviour here, not decoration.
 */

/** What the author is told about their own memo. */
fun ownMemoStatus(memo: Memo): String = when (memo.moderation.state) {
    ModerationState.VISIBLE -> "Posted. Anyone can hear this."

    ModerationState.UNDER_REVIEW ->
        "Being checked. Someone reported this memo and a moderator is looking at it. " +
            "It is hidden until they decide."

    ModerationState.REMOVED -> {
        val reason = memo.moderation.reason
            ?: "No reason was recorded, which is itself a mistake — please appeal so someone looks again."
        "Taken down. $reason ${appealStatus(memo.moderation.appeal)}"
    }
}

/** Where an appeal has got to. */
fun appealStatus(state: AppealState): String = when (state) {
    AppealState.NOT_APPLICABLE -> ""
    AppealState.AVAILABLE -> "You can ask for this to be looked at again."
    AppealState.SUBMITTED -> "You have asked for this to be looked at again. Waiting on a decision."
    AppealState.UPHELD -> "Your appeal succeeded and this memo is back."
    AppealState.DENIED -> "Your appeal was considered and refused."
}

/** Whether to offer the appeal control at all. */
fun canAppeal(memo: Memo): Boolean =
    memo.moderation.state == ModerationState.REMOVED &&
        memo.moderation.appeal == AppealState.AVAILABLE

/** Spoken label for a report reason. Plain words, not policy language. */
fun reportReasonLabel(reason: ReportReason): String = when (reason) {
    ReportReason.HARASSMENT -> "Harassment or bullying"
    ReportReason.HATE_SPEECH -> "Hate speech"
    ReportReason.SEXUAL_CONTENT -> "Sexual content"
    ReportReason.SPAM -> "Spam or advertising"
    ReportReason.SOMETHING_ELSE -> "Something else"
}
