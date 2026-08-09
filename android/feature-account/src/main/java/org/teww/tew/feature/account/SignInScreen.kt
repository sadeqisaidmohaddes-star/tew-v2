package org.teww.tew.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.teww.tew.core.auth.AuthState

/**
 * Sign-in.
 *
 * There is exactly one thing to do on this screen. That is the design: the
 * first screen a new user meets should not require navigating a form, and
 * `BRIEF.md`'s audience is people for whom a mistyped password field is a real
 * cost rather than a small annoyance.
 *
 * The status line is a live region so its changes are announced without the
 * user having to go hunting for them — signing in is asynchronous, and silence
 * during it is indistinguishable from a frozen app when you cannot see a
 * spinner (non-negotiable #1).
 */
@Composable
fun SignInScreen(
    viewModel: AccountViewModel,
    modifier: Modifier = Modifier,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val status = signInStatusText(authState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Third Eye World",
            style = MaterialTheme.typography.headlineMedium,
        )

        Text(
            text = "Short voice memos, from people who get it.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(
            onClick = viewModel::signIn,
            enabled = authState !is AuthState.SigningIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in with Google")
        }

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * Kept separate from the composable so it can be unit-tested on the JVM — the
 * wording of a spoken status is behaviour, not decoration.
 */
internal fun signInStatusText(state: AuthState): String = when (state) {
    is AuthState.SignedOut -> "You are not signed in yet."
    is AuthState.SigningIn -> "Signing you in. This can take a few seconds."
    is AuthState.SignedIn -> "Signed in as ${state.user.username}."
}
