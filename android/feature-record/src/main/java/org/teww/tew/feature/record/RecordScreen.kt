package org.teww.tew.feature.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Record a memo, or a voice reply.
 *
 * ## The design constraint that shapes everything here
 *
 * This screen is operated entirely by ear. The user cannot see a waveform, a
 * red dot or a timer, so **every state has to be spoken** — and the one state
 * that matters most is "am I being recorded right now". A user talking into a
 * phone that is not listening, with no way to tell, is this screen's worst
 * failure, and it is silent when it happens.
 *
 * So: the status line is a live region, it updates once a second while
 * recording, and it names the elapsed time rather than just saying "recording".
 * A ticking number is how you tell a running recorder from a frozen one
 * without looking.
 *
 * ## Start and stop are separate presses
 *
 * Not press-and-hold. Non-negotiable #6 forbids timed or precise gestures, and
 * hold-to-record is exactly that — it also fails badly for anyone whose grip
 * is unsteady, which overlaps heavily with this app's audience.
 *
 * ## Nothing is lost on a failed send
 *
 * If posting fails the recording stays on disk and the send control stays
 * live. Losing someone's memo to a network blip would be unforgivable on the
 * throttled connection this app is explicitly built for.
 */
@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(state.sent) { if (state.sent) onDone() }

    val recording = isRecording(state.recording)

    val actions = buildList {
        if (hasPermission) {
            add(
                CustomAccessibilityAction(
                    if (recording) "Stop recording" else "Start recording",
                ) {
                    if (recording) viewModel.stopRecording() else viewModel.startRecording()
                    true
                },
            )
        }
        if (canSend(state.recording)) {
            add(CustomAccessibilityAction("Listen back") { viewModel.playBack(); true })
            add(CustomAccessibilityAction("Send") { viewModel.send(); true })
            add(CustomAccessibilityAction("Record again") { viewModel.recordAgain(); true })
        }
        add(CustomAccessibilityAction("Cancel and go back") { viewModel.cancel(); onDone(); true })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { customActions = actions },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (viewModel.isReply) "Record a reply" else "Record a memo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = state.announcement,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        )

        if (!hasPermission) {
            Text(
                text = "TEW needs permission to use the microphone before you can record.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BIG_CONTROL_DP.dp),
            ) {
                Text("Allow microphone")
            }
        } else {
            // One large control. It is the only thing most people will press
            // on this screen, and a big target matters for anyone aiming by
            // memory rather than by sight.
            Button(
                onClick = {
                    if (recording) viewModel.stopRecording() else viewModel.startRecording()
                },
                enabled = !state.sending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BIG_CONTROL_DP.dp),
            ) {
                Text(if (recording) "Stop recording" else "Start recording")
            }
        }

        if (canSend(state.recording)) {
            OutlinedButton(
                onClick = viewModel::playBack,
                enabled = !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Listen back")
            }
            Button(
                onClick = viewModel::send,
                enabled = !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isReply) "Send reply" else "Post memo")
            }
            OutlinedButton(
                onClick = viewModel::recordAgain,
                enabled = !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Record again")
            }
        }

        OutlinedButton(
            onClick = { viewModel.cancel(); onDone() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }
    }
}

/** Comfortably above the 48dp minimum — this is the button that matters. */
private const val BIG_CONTROL_DP = 96
