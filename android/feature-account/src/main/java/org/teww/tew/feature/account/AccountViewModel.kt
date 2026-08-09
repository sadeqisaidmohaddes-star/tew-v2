package org.teww.tew.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.teww.tew.core.TewResult
import org.teww.tew.core.auth.AuthSession
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.ReportReason
import org.teww.tew.core.repo.ModerationRepository

/**
 * What the profile screen is showing.
 *
 * [announcement] is the one-line spoken status. It is part of the state rather
 * than a side channel so that every state change has an audible consequence by
 * construction, instead of relying on each call site to remember to say
 * something.
 */
data class AccountUiState(
    val loading: Boolean = false,
    val myMemos: List<Memo> = emptyList(),
    val announcement: String = "",
)

/**
 * Backs sign-in, the profile, reporting and appeals.
 *
 * One view model for all four because they share a single dependency pair and
 * splitting them would mean four separate wiring points in `:app` for what is,
 * from the user's side, one "your account" surface.
 */
class AccountViewModel(
    private val authSession: AuthSession,
    private val moderationRepository: ModerationRepository,
) : ViewModel() {

    val authState = authSession.state

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    val username: String get() = authSession.currentUser?.username ?: "you"

    fun signIn() {
        viewModelScope.launch { authSession.signIn() }
    }

    fun signOut() {
        viewModelScope.launch {
            authSession.signOut()
            _uiState.value = AccountUiState(announcement = "Signed out.")
        }
    }

    fun loadMyMemos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                announcement = "Loading your memos.",
            )
            when (val result = moderationRepository.myMemos()) {
                is TewResult.Ok -> _uiState.value = AccountUiState(
                    loading = false,
                    myMemos = result.value,
                    announcement = describeMemoCount(result.value),
                )

                is TewResult.Failure -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    announcement = result.spoken,
                )
            }
        }
    }

    fun report(memoId: String, reason: ReportReason, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val spoken = when (val result = moderationRepository.report(memoId, reason)) {
                is TewResult.Ok ->
                    "Report sent. A moderator will look at it. Thank you for telling us."

                is TewResult.Failure -> result.spoken
            }
            onDone(spoken)
        }
    }

    fun appeal(memoId: String, text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(announcement = "Sending your appeal.")
            val spoken = when (val result = moderationRepository.appeal(memoId, text)) {
                is TewResult.Ok ->
                    "Appeal sent. You will hear back on this memo once someone has looked again."

                is TewResult.Failure -> result.spoken
            }
            _uiState.value = _uiState.value.copy(announcement = spoken)
            loadMyMemos()
        }
    }
}

/**
 * Pure so it can be unit-tested. The empty case matters most: a silent empty
 * list is indistinguishable from a failed load when you cannot see the screen.
 */
internal fun describeMemoCount(memos: List<Memo>): String = when (memos.size) {
    0 -> "You have not posted any memos yet."
    1 -> "You have posted one memo."
    else -> "You have posted ${memos.size} memos."
}
