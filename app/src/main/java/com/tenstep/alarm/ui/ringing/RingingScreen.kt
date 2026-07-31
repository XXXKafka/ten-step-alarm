package com.tenstep.alarm.ui.ringing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenstep.alarm.BuildConfig
import com.tenstep.alarm.R
import com.tenstep.alarm.alarm.RingingSession
import com.tenstep.alarm.alarm.StepMode

@Composable
fun RingingScreen(
    viewModel: RingingViewModel,
    onClose: () -> Unit
) {
    val alarm by viewModel.alarm.collectAsStateWithLifecycle()
    val isSnooze by viewModel.isSnooze.collectAsStateWithLifecycle()
    val steps by viewModel.steps.collectAsStateWithLifecycle()
    val stepMode by viewModel.stepMode.collectAsStateWithLifecycle()
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsStateWithLifecycle()
    val debugSimulate by viewModel.debugSimulateSteps.collectAsStateWithLifecycle()

    LaunchedEffect(alarm) {
        if (alarm == null) onClose()
    }

    val current = alarm ?: return
    val target = RingingSession.STEPS_TARGET
    val satisfied = steps >= target
    val remaining = (target - steps).coerceAtLeast(0)
    val progress = (steps.toFloat() / target).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when {
                isSnooze -> stringResource(R.string.ringing_snooze_title)
                current.label.isNotBlank() -> current.label
                else -> stringResource(R.string.ringing_title)
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.time_format, current.hour, current.minute),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(36.dp))

        // Prominent live step counter: ticks by 1 immediately on every step.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = steps.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (satisfied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "/ $target",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                when (stepMode) {
                    StepMode.ACCELEROMETER -> {
                        Text(
                            text = stringResource(R.string.step_mode_accelerometer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    StepMode.UNAVAILABLE -> {
                        Text(
                            text = stringResource(R.string.step_mode_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    StepMode.STEP_COUNTER -> Unit
                }
                Text(
                    text = if (satisfied) {
                        stringResource(R.string.steps_done)
                    } else {
                        stringResource(R.string.step_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (satisfied) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (!satisfied && stepMode != StepMode.UNAVAILABLE) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.steps_remaining, remaining),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    viewModel.snooze()
                    onClose()
                }
            ) {
                Text(stringResource(R.string.snooze_action_min, snoozeMinutes))
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    viewModel.dismiss()
                    onClose()
                },
                enabled = satisfied
            ) {
                Text(stringResource(R.string.dismiss_action))
            }
        }

        if (BuildConfig.DEBUG && debugSimulate) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.simulateSteps(10) }) {
                Text(stringResource(R.string.simulate_steps))
            }
        }
    }
}