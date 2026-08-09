package org.teww.tew.core.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.teww.tew.core.TewResult

/**
 * Stand-in [AuthSession] with no Firebase behind it.
 *
 * `STATE.md` lists the Firebase project as deliberately deferred: no account
 * has been chosen yet. Rather than block every screen on that decision, this
 * satisfies the interface so sign-in, sign-out and the signed-in/signed-out
 * branches of the UI can all be built and tested for real.
 *
 * **This is not a security boundary and must never reach a release build.** It
 * signs anyone in, accepts no credential, and its "token" is a fixed string.
 * The replacement is a `FirebaseAuthSession` implementing the same interface;
 * nothing above `:core` should need to change when it lands.
 *
 * The artificial [signInDelayMs] is not padding — it keeps the sign-in screen
 * honest about being asynchronous, so the spoken loading state required by
 * IMPLEMENTATION.md gets built and exercised rather than never firing because
 * the stub returned instantly.
 */
class StubAuthSession(
    private val signInDelayMs: Long = 600,
    private val username: String = "test-user",
) : AuthSession {

    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override val currentUser: AuthUser?
        get() = (_state.value as? AuthState.SignedIn)?.user

    override suspend fun signIn(): TewResult<AuthUser> {
        _state.value = AuthState.SigningIn
        delay(signInDelayMs)
        val user = AuthUser(id = STUB_USER_ID, username = username)
        _state.value = AuthState.SignedIn(user)
        return TewResult.Ok(user)
    }

    override suspend fun signOut() {
        _state.value = AuthState.SignedOut
    }

    override suspend fun bearerToken(): String? =
        if (_state.value is AuthState.SignedIn) STUB_TOKEN else null

    private companion object {
        const val STUB_USER_ID = "stub-user"
        const val STUB_TOKEN = "stub-token-not-a-credential"
    }
}
