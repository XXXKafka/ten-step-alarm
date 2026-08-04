package com.tenstep.alarm.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R

@Composable
fun TimerScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.tab_timer)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.tab_stopwatch)) }
            )
        }
        when (tab) {
            0 -> TimerView()
            else -> StopwatchView()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerView() {
    val viewModel: TimerViewModel = viewModel()
    val minutes by viewModel.minutes.collectAsStateWithLifecycle()
    val totalMs by viewModel.totalMs.collectAsStateWithLifecycle()
    val remainingMs by viewModel.remainingMs.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val completionTick by viewModel.completionTick.collectAsStateWithLifecycle()

    val fraction = if (totalMs > 0) {
        (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
    } else 0f
    val presets = listOf(1, 3, 5, 10, 25)
    // "Custom" opens a number dialog for a precise duration; the dialog input
    // starts empty so typing always replaces (no prefill-append confusion).
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { p ->
                FilterChip(
                    selected = minutes == p,
                    onClick = { viewModel.setMinutes(p) },
                    label = { Text(stringResource(R.string.timer_minutes_format, p)) }
                )
            }
            FilterChip(
                selected = presets.none { it == minutes },
                onClick = { showCustomDialog = true },
                label = { Text(stringResource(R.string.timer_custom)) }
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.timer_minutes_format, minutes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Slider(
            value = minutes.toFloat(),
            onValueChange = { viewModel.setMinutes(it.toInt()) },
            valueRange = 1f..180f,
            steps = 178,
            enabled = !running
        )
        Spacer(Modifier.height(24.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(240.dp),
                strokeWidth = 12.dp
            )
            Text(
                text = formatTime(remainingMs),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(36.dp))
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
        }
    }

    if (showCustomDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text(stringResource(R.string.timer_custom_minutes)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.timer_custom_minutes)) },
                    suffix = { Text(stringResource(R.string.timer_minutes_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        input.toIntOrNull()?.let(viewModel::setMinutes)
                        showCustomDialog = false
                    },
                    enabled = input.toIntOrNull() != null
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (completionTick > 0) {
        AlertDialog(
            onDismissRequest = viewModel::acknowledgeCompletion,
            title = { Text(stringResource(R.string.timer_notification_title)) },
            text = { Text(stringResource(R.string.timer_done_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::acknowledgeCompletion) {
                    Text(stringResource(R.string.pomodoro_end_confirm))
                }
            }
        )
    }
}

@Composable
private fun StopwatchView() {
    val viewModel: StopwatchViewModel = viewModel()
    val elapsedMs by viewModel.elapsedMs.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = StopwatchEngine.format(elapsedMs),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.pomodoro_reset))
            }
            Spacer(Modifier.width(12.dp))
            if (running) {
                Button(onClick = viewModel::lap) {
                    Text(stringResource(R.string.stopwatch_lap))
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = { if (running) viewModel.pause() else viewModel.start() }) {
                Text(
                    stringResource(
                        if (running) R.string.pomodoro_pause else R.string.pomodoro_start
                    )
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(laps) { index, lap ->
                    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Text(
                            text = stringResource(R.string.stopwatch_lap_number, index + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = StopwatchEngine.format(lap),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}