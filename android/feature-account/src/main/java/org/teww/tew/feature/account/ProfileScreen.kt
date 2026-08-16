package org.teww.tew.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.teww.tew.core.model.Memo

/**
 * The whole profile: your username, your memos, and the way out.
 *
 * `android/README.md` scopes this to "your own username + your own posted
 * memos, sign out" — there is no bio, no avatar, no counts. That is not a
 * simplification for now; a profile with numbers on it is engagement machinery
 * (non-negotiable #4) wearing a different hat.
 *
 * Each memo card is collapsed into a single accessibility node with one spoken
 * description. Left as separate nodes, a screen-reader user would swipe through
 * four fragments per memo — author, status, reason, appeal — and have to
 * assemble the meaning themselves.
 */
@Composable
fun ProfileScreen(
    viewModel: AccountViewModel,
    onAppeal: (Memo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadMyMemos() }

    // Deleting destroys the recording — there is no undo to offer afterwards,
    // so the question comes first. Non-negotiable #6 rules out timed or precise
    // gestures, and this is the same principle applied to consequences: nothing
    // irreversible happens on one tap.
    var pendingDelete by remember { mutableStateOf<Memo?>(null) }

    pendingDelete?.let { memo ->
        DeleteConfirmation(
            onConfirm = { viewModel.deleteMemo(memo.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "You are ${viewModel.username}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = state.announcement,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.myMemos, key = { it.id }) { memo ->
                OwnMemoCard(
                    memo = memo,
                    onAppeal = { onAppeal(memo) },
                    onDelete = { pendingDelete = memo },
                )
            }
        }

        OutlinedButton(
            onClick = viewModel::signOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign out")
        }
    }
}

@Composable
private fun OwnMemoCard(
    memo: Memo,
    onAppeal: () -> Unit,
    onDelete: () -> Unit,
) {
    val spoken = buildString {
        append("Your memo. ")
        memo.transcript?.let { append("$it ") }
        append(ownMemoStatus(memo))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics(mergeDescendants = true) { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            memo.transcript?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = ownMemoStatus(memo),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Outside the merged node above so these stay separately focusable,
        // separately actionable controls rather than being swallowed by the
        // card's description.
        Row(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
            if (canAppeal(memo)) {
                TextButton(onClick = onAppeal) {
                    Text("Ask for another look")
                }
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

/**
 * The question asked before a recording is destroyed.
 *
 * Worded so it is unambiguous heard rather than read: "Delete" and "Keep it"
 * are distinguishable in one word each, where "Cancel" next to "Delete" leaves
 * a listener working out which one keeps their memo. The consequence is stated
 * in the body rather than implied by the title, because a screen reader user
 * may act on the button before the whole dialog has been read out.
 */
@Composable
private fun DeleteConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this memo?") },
        text = {
            Text(
                "The recording will be destroyed. It cannot be brought back, " +
                    "and anyone who has not heard it never will.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep it") }
        },
    )
}

/**
 * A memo card with no interactive parts, for use in lists where the whole row
 * is one control. Kept here so both feed modules can present an author's memo
 * consistently without either of them owning profile code.
 */
@Composable
fun MemoSummary(memo: Memo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = memo.authorUsername, style = MaterialTheme.typography.titleMedium)
        memo.transcript?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
