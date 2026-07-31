package com.tenstep.alarm.ui.alarm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R
import com.tenstep.alarm.data.RepeatDays

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(onClose: () -> Unit) {
    val viewModel: AlarmEditViewModel = viewModel()
    val context = LocalContext.current

    val isNew by viewModel.isNew.collectAsStateWithLifecycle()
    val hour by viewModel.hour.collectAsStateWithLifecycle()
    val minute by viewModel.minute.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val label by viewModel.label.collectAsStateWithLifecycle()
    val ringtoneUri by viewModel.ringtoneUri.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val vibrate by viewModel.vibrate.collectAsStateWithLifecycle()

    val loaded by viewModel.loaded.collectAsStateWithLifecycle()

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            uri?.toString()?.let { viewModel.setRingtone(it) }
        }
    }

    // Recreate the picker once the alarm data is loaded so editing an existing
    // alarm shows its real time instead of the defaults (the branch switch
    // replaces the remember slot and rebuilds the picker state).
    val timeState = if (loaded) {
        rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
    } else {
        rememberTimePickerState(
            initialHour = 7,
            initialMinute = 0,
            is24Hour = true
        )
    }
    LaunchedEffect(timeState.hour) { viewModel.setHour(timeState.hour) }
    LaunchedEffect(timeState.minute) { viewModel.setMinute(timeState.minute) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isNew) R.string.alarm_add else R.string.alarm_edit))
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onClose) }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.edit_time),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TimePicker(state = timeState)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.edit_repeat),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    val weekdays = stringArrayResource(R.array.weekday_short)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatDays.ALL_DAYS_LIST.forEach { bit ->
                            val selected = days and bit != 0
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.toggleDay(bit) },
                                label = {
                                    Text(weekdays[RepeatDays.indexOf(bit)])
                                },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.width(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = viewModel::setLabel,
                        label = { Text(stringResource(R.string.edit_label)) },
                        placeholder = { Text(stringResource(R.string.edit_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.edit_ringtone),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = ringtoneLabel(context, ringtoneUri),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM
                                )
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TITLE,
                                    context.getString(R.string.ringtone_picker_title)
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                runCatching {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        Uri.parse(ringtoneUri)
                                    )
                                }
                            }
                            ringtoneLauncher.launch(intent)
                        }) {
                            Text(stringResource(R.string.choose_ringtone))
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.edit_volume) + " $volume%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = volume.toFloat(),
                            onValueChange = { viewModel.setVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 9
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.edit_vibrate),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = vibrate, onCheckedChange = viewModel::setVibrate)
                    }
                }
            }

            if (!isNew) {
                TextButton(
                    onClick = { viewModel.delete(onClose) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.edit_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ringtoneLabel(context: android.content.Context, uri: String): String {
    if (uri.isBlank()) return stringResource(R.string.ringtone_default)
    val ringtone = runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(uri))
    }.getOrNull()
    return ringtone?.getTitle(context) ?: stringResource(R.string.ringtone_default)
}
