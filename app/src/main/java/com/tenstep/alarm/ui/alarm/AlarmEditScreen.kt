package com.tenstep.alarm.ui.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R
import com.tenstep.alarm.data.RepeatDays
import kotlin.math.abs

/**
 * Alarm editor styled like the iOS Clock app: centered title bar (取消/保存),
 * an hour/minute scroll-wheel time picker, and grouped list rows (重复 / 标签 /
 * 铃声 / 音量 / 震动 / 稍后提醒 / 删除) with chevrons and hairline dividers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(onClose: () -> Unit) {
    val viewModel: AlarmEditViewModel = viewModel()
    val context = LocalContext.current

    val isNew by viewModel.isNew.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val hour by viewModel.hour.collectAsStateWithLifecycle()
    val minute by viewModel.minute.collectAsStateWithLifecycle()
    val days by viewModel.days.collectAsStateWithLifecycle()
    val label by viewModel.label.collectAsStateWithLifecycle()
    val ringtoneUri by viewModel.ringtoneUri.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val vibrate by viewModel.vibrate.collectAsStateWithLifecycle()
    val snoozeEnabled by viewModel.snoozeEnabled.collectAsStateWithLifecycle()

    var showRepeatDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }

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

    if (showRepeatDialog) {
        RepeatDialog(
            days = days,
            onToggle = viewModel::toggleDay,
            onDismiss = { showRepeatDialog = false }
        )
    }
    if (showLabelDialog) {
        LabelDialog(
            initial = label,
            onConfirm = viewModel::setLabel,
            onDismiss = { showLabelDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(if (isNew) R.string.alarm_add else R.string.alarm_edit))
                },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onClose) }) {
                        Text(stringResource(R.string.save))
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
            // iOS-style scroll-wheel time picker.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                // Rebuild the wheels once the alarm data is loaded so editing
                // an existing alarm starts from its real time.
                val wheelHour = if (loaded) hour else 7
                val wheelMinute = if (loaded) minute else 0
                TimeWheelPicker(
                    hour = wheelHour,
                    minute = wheelMinute,
                    onHourChange = viewModel::setHour,
                    onMinuteChange = viewModel::setMinute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }

            // Grouped list (iOS style).
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column {
                    AppleListRow(
                        title = stringResource(R.string.edit_repeat),
                        subtitle = repeatSummary(days),
                        onClick = { showRepeatDialog = true }
                    )
                    AppleDivider()
                    AppleListRow(
                        title = stringResource(R.string.edit_label),
                        subtitle = label.ifBlank { null },
                        onClick = { showLabelDialog = true }
                    )
                    AppleDivider()
                    AppleListRow(
                        title = stringResource(R.string.edit_ringtone),
                        subtitle = ringtoneLabel(context, ringtoneUri),
                        onClick = {
                            ringtoneLauncher.launch(ringtonePickerIntent(context, ringtoneUri))
                        }
                    )
                    AppleDivider()
                    AppleListRow(
                        title = stringResource(R.string.edit_volume),
                        subtitle = "$volume%"
                    ) {
                        Slider(
                            value = volume.toFloat(),
                            onValueChange = { viewModel.setVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 9,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    AppleDivider()
                    AppleListRow(
                        title = stringResource(R.string.edit_vibrate),
                        onClick = { viewModel.setVibrate(!vibrate) }
                    ) {
                        Switch(checked = vibrate, onCheckedChange = viewModel::setVibrate)
                    }
                    AppleDivider()
                    AppleListRow(
                        title = stringResource(R.string.edit_snooze),
                        onClick = { viewModel.setSnoozeEnabled(!snoozeEnabled) }
                    ) {
                        Switch(
                            checked = snoozeEnabled,
                            onCheckedChange = viewModel::setSnoozeEnabled
                        )
                    }
                }
            }

            // Delete row (editing only), iOS style: red centered text.
            if (!isNew) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.edit_delete),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.delete(onClose) }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * iOS-style time wheels: hour (0-23) and minute (0-59) scroll vertically and
 * snap to the center row; the centered value is highlighted and selected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeWheelPicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        WheelColumn(
            items = hours,
            initialIndex = hour.coerceIn(0, 23),
            onSelected = onHourChange,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        WheelColumn(
            items = minutes,
            initialIndex = minute.coerceIn(0, 59),
            onSelected = onMinuteChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 52.dp
    val visibleCount = 5
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var centerIndex by remember { mutableStateOf(initialIndex) }

    // Track which item is currently centered and report it live.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layout ->
                val itemsInfo = layout.visibleItemsInfo
                if (itemsInfo.isNotEmpty()) {
                    val centerY = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                    val closest = itemsInfo.minByOrNull {
                        abs((it.offset + it.size / 2f) - centerY)
                    }
                    closest?.let {
                        if (centerIndex != it.index) {
                            centerIndex = it.index
                            onSelected(it.index)
                        }
                    }
                }
            }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleCount),
        contentAlignment = Alignment.Center
    ) {
        // Center selection band (drawn behind the text).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp)
                )
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(items) { index, value ->
                val centered = index == centerIndex
                Text(
                    text = value,
                    fontSize = if (centered) 28.sp else 18.sp,
                    fontWeight = if (centered) FontWeight.Bold else FontWeight.Normal,
                    color = if (centered) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                )
            }
        }
    }
}

@Composable
private fun AppleListRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 10.dp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onClick != null && trailing == null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun AppleDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun repeatSummary(daysOfWeek: Int): String {
    val short = stringArrayResource(R.array.weekday_short)
    val once = stringResource(R.string.repeat_once)
    val daily = stringResource(R.string.repeat_daily)
    val workdays = stringResource(R.string.repeat_workdays)
    return when {
        daysOfWeek == 0 -> once
        daysOfWeek == RepeatDays.ALL -> daily
        daysOfWeek == RepeatDays.WORKDAYS -> workdays
        else -> RepeatDays.ALL_DAYS_LIST
            .filter { daysOfWeek and it != 0 }
            .joinToString(" ") { short[RepeatDays.indexOf(it)] }
    }
}

@Composable
private fun RepeatDialog(
    days: Int,
    onToggle: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val weekdays = stringArrayResource(R.array.weekday_full)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_repeat)) },
        text = {
            Column {
                RepeatDays.ALL_DAYS_LIST.forEach { bit ->
                    val checked = days and bit != 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(bit) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weekdays[RepeatDays.indexOf(bit)],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(checked = checked, onCheckedChange = { onToggle(bit) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun LabelDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_label)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.edit_label_hint)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()); onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

private fun ringtonePickerIntent(
    context: android.content.Context,
    existingUri: String
): Intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
    putExtra(
        RingtoneManager.EXTRA_RINGTONE_TITLE,
        context.getString(R.string.ringtone_picker_title)
    )
    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
    runCatching {
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri))
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