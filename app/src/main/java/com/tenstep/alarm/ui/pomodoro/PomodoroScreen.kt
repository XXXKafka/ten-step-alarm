package com.tenstep.alarm.ui.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R
import com.tenstep.alarm.pomodoro.PomodoroMode
import com.tenstep.alarm.pomodoro.PomodoroViewModel

@Composable
fun PomodoroScreen() {
    val viewModel: PomodoroViewModel = viewModel()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val remainingMs by viewModel.remainingMs.collectAsStateWithLifecycle()
    val totalMs by viewModel.totalMs.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val sessions by viewModel.sessionsCompleted.collectAsStateWithLifecycle()
    val completionTick by viewModel.completionTick.collectAsStateWithLifecycle()
    val finishedMode by viewModel.lastFinishedMode.collectAsStateWithLifecycle()

    val fraction = if (totalMs > 0) {
        (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(
                if (mode == PomodoroMode.FOCUS) R.string.pomodoro_focus
                else R.string.pomodoro_break
            ),
            style = MaterialTheme.typography.headlineMedium,
            color = if (mode == PomodoroMode.FOCUS) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        )
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(260.dp),
                strokeWidth = 12.dp
            )
            Text(
                text = formatTime(remainingMs),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.pomodoro_reset))
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { if (running) viewModel.pause() else viewModel.start() }) {
                Text(
                    stringResource(
                        if (running) R.string.pomodoro_pause else R.string.pomodoro_start
                    )
                )
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = viewModel::skipToNextPhase) {
                Text(stringResource(R.string.pomodoro_switch_mode))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.pomodoro_sessions, sessions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // End-of-phase prompt page (dismissible).
    if (completionTick > 0 && finishedMode != null) {
        val titleRes = if (finishedMode == PomodoroMode.FOCUS) {
            R.string.pomodoro_done_focus
        } else {
            R.string.pomodoro_done_break
        }
        AlertDialog(
            onDismissRequest = viewModel::acknowledgeCompletion,
            title = { Text(stringResource(R.string.pomodoro_notification_title)) },
            text = { Text(stringResource(titleRes)) },
            confirmButton = {
                TextButton(onClick = viewModel::acknowledgeCompletion) {
                    Text(stringResource(R.string.pomodoro_end_confirm))
                }
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}