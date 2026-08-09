package org.teww.tew.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.ReportReason

/**
 * Reporting a memo.
 *
 * One button per reason, no free text, no confirmation dialog. A dialog would
 * add a second decision point to something the user has already decided, and
 * `android/README.md` puts reporting in the shipping scope precisely because
 * non-negotiable #8 does not treat it as optional — so it has to be easy enough
 * to actually use.
 */
@Composable
fun ReportScreen(
    memo: Memo,
    onReport: (ReportReason) -> Unit,
    onCancel: () -> Unit,
    status: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Report this memo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = "By ${memo.authorUsername}. Choose what is wrong with it.",
            style = MaterialTheme.typography.bodyMedium,
        )

        ReportReason.entries.forEach { reason ->
            Button(
                onClick = { onReport(reason) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(reportReasonLabel(reason))
            }
        }

        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Never mind")
        }

        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * Appealing a removal.
 *
 * The reason the memo was removed is repeated on this screen rather than left
 * behind on the profile. Someone appealing needs to answer it, and asking them
 * to remember it while typing is a memory test nobody agreed to.
 */
@Composable
fun AppealScreen(
    memo: Memo,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    status: String,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Ask for another look",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = memo.moderation.reason
                ?: "No reason was recorded for this removal.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = "Say why you think this was wrong. A person will read it.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Your explanation") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )

        Button(
            onClick = { onSubmit(text) },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send appeal")
        }

        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Go back")
        }

        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
