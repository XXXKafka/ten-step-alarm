package com.tenstep.alarm.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tenstep.alarm.ui.theme.hslToColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.BuildConfig
import com.tenstep.alarm.R
import com.tenstep.alarm.data.ThemeMode
import com.tenstep.alarm.ui.theme.ThemePresetSeeds
import com.tenstep.alarm.ui.theme.toHslComponents
import com.tenstep.alarm.util.LocaleHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColorSource by viewModel.themeColorSource.collectAsStateWithLifecycle()
    val themePresetIndex by viewModel.themePresetIndex.collectAsStateWithLifecycle()
    val themeCustomColor by viewModel.themeCustomColor.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val clockShowSeconds by viewModel.clockShowSeconds.collectAsStateWithLifecycle()
    val clockShowDate by viewModel.clockShowDate.collectAsStateWithLifecycle()
    val clock24Hour by viewModel.clock24Hour.collectAsStateWithLifecycle()
    val clockStyle by viewModel.clockStyle.collectAsStateWithLifecycle()
    val clockCustomColor by viewModel.clockCustomColor.collectAsStateWithLifecycle()
    val clockFontScale by viewModel.clockFontScale.collectAsStateWithLifecycle()

    val focusMinutes by viewModel.focusMinutes.collectAsStateWithLifecycle()
    val breakMinutes by viewModel.breakMinutes.collectAsStateWithLifecycle()
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsStateWithLifecycle()
    val debugSimulate by viewModel.debugSimulateSteps.collectAsStateWithLifecycle()
    val defaultRingtone by viewModel.defaultRingtone.collectAsStateWithLifecycle()
    val alarmMonitorEnabled by viewModel.alarmMonitorEnabled.collectAsStateWithLifecycle()
    val clockFullscreen by viewModel.clockFullscreen.collectAsStateWithLifecycle()
    val pomodoroFocusRingtone by viewModel.pomodoroFocusRingtone.collectAsStateWithLifecycle()
    val pomodoroBreakRingtone by viewModel.pomodoroBreakRingtone.collectAsStateWithLifecycle()

    var canScheduleExact by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var notificationGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var pendingRingtoneTarget by remember { mutableStateOf(0) }
    var showThemeColorPicker by remember { mutableStateOf(false) }
    var showClockColorPicker by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        canScheduleExact = viewModel.canScheduleExactAlarms()
        notificationGranted = hasNotificationPermission(context)
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted || hasNotificationPermission(context)
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            uri?.toString()?.let { viewModel.setDefaultRingtone(it) }
        }
    }
    val pomodoroRingtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI
            )
            uri?.toString()?.let { picked ->
                if (pendingRingtoneTarget == 0) {
                    viewModel.setPomodoroFocusRingtone(picked)
                } else {
                    viewModel.setPomodoroBreakRingtone(picked)
                }
            }
        }
    }

    if (showThemeColorPicker) {
        ColorPickerDialog(
            title = stringResource(R.string.choose_color),
            initialColor = Color(themeCustomColor.toInt()),
            onDismiss = { showThemeColorPicker = false },
            onConfirm = { color ->
                viewModel.setThemeCustomColor(color.toArgb().toLong())
                viewModel.setThemeColorSource("custom")
                showThemeColorPicker = false
            }
        )
    }
    if (showClockColorPicker) {
        ColorPickerDialog(
            title = stringResource(R.string.choose_color),
            initialColor = Color(clockCustomColor.toInt()),
            onDismiss = { showClockColorPicker = false },
            onConfirm = { color ->
                viewModel.setClockCustomColor(color.toArgb().toLong())
                viewModel.setClockStyle("custom")
                showClockColorPicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App language
        SettingsCard(title = stringResource(R.string.language)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageChip(
                    label = stringResource(R.string.lang_system),
                    selected = language == LocaleHelper.LANG_SYSTEM,
                    onClick = { viewModel.setLanguage(LocaleHelper.LANG_SYSTEM); (context as? Activity)?.recreate() }
                )
                LanguageChip(
                    label = stringResource(R.string.lang_zh),
                    selected = language == LocaleHelper.LANG_ZH,
                    onClick = { viewModel.setLanguage(LocaleHelper.LANG_ZH); (context as? Activity)?.recreate() }
                )
                LanguageChip(
                    label = stringResource(R.string.lang_en),
                    selected = language == LocaleHelper.LANG_EN,
                    onClick = { viewModel.setLanguage(LocaleHelper.LANG_EN); (context as? Activity)?.recreate() }
                )
            }
        }

        // Theme mode
        SettingsCard(title = stringResource(R.string.theme_mode)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                    OptionChip(
                        selected = themeMode == mode,
                        label = stringResource(label),
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                }
            }
        }

        // Theme color
        SettingsCard(title = stringResource(R.string.theme_color)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip(
                    selected = themeColorSource == "dynamic",
                    label = stringResource(R.string.theme_color_dynamic),
                    onClick = { viewModel.setThemeColorSource("dynamic") }
                )
                OptionChip(
                    selected = themeColorSource == "preset",
                    label = stringResource(R.string.theme_color_preset),
                    onClick = { viewModel.setThemeColorSource("preset") }
                )
                OptionChip(
                    selected = themeColorSource == "custom",
                    label = stringResource(R.string.theme_color_custom),
                    onClick = { viewModel.setThemeColorSource("custom") }
                )
            }
            if (themeColorSource == "preset") {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemePresetSeeds.forEachIndexed { index, color ->
                        val selected = themePresetIndex == index
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setThemePresetIndex(index)
                                    viewModel.setThemeColorSource("preset")
                                }
                        )
                    }
                }
            }
            if (themeColorSource == "custom") {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorSwatch(color = Color(themeCustomColor.toInt()))
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showThemeColorPicker = true }) {
                        Text(stringResource(R.string.choose_color))
                    }
                }
            }
        }

        // Time format (shared by the alarm editor wheels and the flip clock)
        SettingsCard(title = stringResource(R.string.time_format_title)) {
            SwitchRow(
                label = stringResource(R.string.clock_24hour),
                checked = clock24Hour,
                onCheckedChange = viewModel::setClock24Hour
            )
        }

        // Alarm guard (background keep-alive)
        SettingsCard(title = "") {
            SwitchRow(
                label = stringResource(R.string.alarm_monitor_setting),
                checked = alarmMonitorEnabled,
                onCheckedChange = viewModel::setAlarmMonitorEnabled
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.alarm_monitor_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Pomodoro durations
        SettingsCard(title = "") {
            Text(
                text = stringResource(R.string.focus_minutes) + ": $focusMinutes",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = focusMinutes.toFloat(),
                onValueChange = { viewModel.setFocusMinutes(it.toInt()) },
                valueRange = 5f..90f,
                steps = 16
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.break_minutes) + ": $breakMinutes",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = breakMinutes.toFloat(),
                onValueChange = { viewModel.setBreakMinutes(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28
            )
            Spacer(Modifier.height(12.dp))
            RingtoneRow(
                label = stringResource(R.string.pomodoro_focus_ringtone),
                uri = pomodoroFocusRingtone,
                onPick = {
                    pendingRingtoneTarget = 0
                    pomodoroRingtoneLauncher.launch(
                        buildRingtonePickerIntent(context, pomodoroFocusRingtone)
                    )
                }
            )
            RingtoneRow(
                label = stringResource(R.string.pomodoro_break_ringtone),
                uri = pomodoroBreakRingtone,
                onPick = {
                    pendingRingtoneTarget = 1
                    pomodoroRingtoneLauncher.launch(
                        buildRingtonePickerIntent(context, pomodoroBreakRingtone)
                    )
                }
            )
        }

        // Snooze length
        SettingsCard(title = "") {
            Text(
                text = stringResource(R.string.snooze_minutes) + ": $snoozeMinutes",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = snoozeMinutes.toFloat(),
                onValueChange = { viewModel.setSnoozeMinutes(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28
            )
        }

        // Flip clock customization
        SettingsCard(title = stringResource(R.string.clock_settings)) {
            SwitchRow(
                label = stringResource(R.string.clock_show_seconds),
                checked = clockShowSeconds,
                onCheckedChange = viewModel::setClockShowSeconds
            )
            SwitchRow(
                label = stringResource(R.string.clock_show_date),
                checked = clockShowDate,
                onCheckedChange = viewModel::setClockShowDate
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.clock_style),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionChip("auto", clockStyle, stringResource(R.string.clock_style_auto), viewModel::setClockStyle)
                OptionChip("dark", clockStyle, stringResource(R.string.clock_style_dark), viewModel::setClockStyle)
                OptionChip("light", clockStyle, stringResource(R.string.clock_style_light), viewModel::setClockStyle)
                OptionChip("custom", clockStyle, stringResource(R.string.clock_style_custom), viewModel::setClockStyle)
            }
            if (clockStyle == "custom") {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorSwatch(color = Color(clockCustomColor.toInt()))
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = { showClockColorPicker = true }) {
                        Text(stringResource(R.string.choose_color))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.clock_font_size) +
                    ": ${(clockFontScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = clockFontScale,
                onValueChange = viewModel::setClockFontScale,
                valueRange = 0.7f..1.5f,
                steps = 7
            )
            Spacer(Modifier.height(4.dp))
            SwitchRow(
                label = stringResource(R.string.clock_fullscreen),
                checked = clockFullscreen,
                onCheckedChange = viewModel::setClockFullscreen
            )
        }

        // Permissions & default ringtone
        SettingsCard(title = "") {
            PermissionRow(
                title = stringResource(R.string.exact_alarm_permission),
                status = if (canScheduleExact) {
                    stringResource(R.string.exact_alarm_granted)
                } else {
                    stringResource(R.string.exact_alarm_required)
                },
                actionVisible = !canScheduleExact,
                actionLabel = stringResource(R.string.request_permission),
                onAction = viewModel::openExactAlarmSettings
            )

            PermissionRow(
                title = stringResource(R.string.notification_permission),
                status = if (notificationGranted) {
                    stringResource(R.string.notification_granted)
                } else {
                    stringResource(R.string.notification_required)
                },
                actionVisible = !notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                actionLabel = stringResource(R.string.request_notification),
                onAction = {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ringtone_default),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = ringtoneLabel(context, defaultRingtone),
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
                                Uri.parse(defaultRingtone)
                            )
                        }
                    }
                    ringtoneLauncher.launch(intent)
                }) {
                    Text(stringResource(R.string.choose_ringtone))
                }
            }
        }

        // Xiaomi / MIUI background permission guide
        if (viewModel.isXiaomi()) {
            SettingsCard(title = stringResource(R.string.background_permissions_title)) {
                BackgroundButtonRow(
                    label = stringResource(R.string.xiaomi_autostart),
                    onOpen = viewModel::openXiaomiAutostart
                )
                Spacer(Modifier.height(8.dp))
                BackgroundButtonRow(
                    label = stringResource(R.string.xiaomi_background_popup),
                    onOpen = viewModel::openXiaomiBackgroundPopup
                )
                Spacer(Modifier.height(8.dp))
                BackgroundButtonRow(
                    label = stringResource(R.string.battery_optimization),
                    onOpen = viewModel::openBatteryOptimizationSettings
                )
            }
        }

        // Debug section (debug builds only)
        if (BuildConfig.DEBUG) {
            SettingsCard(title = "") {
                SwitchRow(
                    label = stringResource(R.string.debug_simulate),
                    checked = debugSimulate,
                    onCheckedChange = viewModel::setDebugSimulateSteps
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            if (title.isNotBlank()) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OptionChip(selected = selected, label = label, onClick = onClick)
}

@Composable
private fun OptionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
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

@Composable
private fun OptionChip(
    value: String,
    current: String,
    label: String,
    onSelect: (String) -> Unit
) {
    OptionChip(selected = current == value, label = label, onClick = { onSelect(value) })
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val initialHsl = remember(initialColor) { initialColor.toHslComponents() }
    var hue by remember { mutableFloatStateOf(initialHsl[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsl[1]) }
    var lightness by remember { mutableFloatStateOf(initialHsl[2]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(hslToColor(hue, saturation, lightness))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.color_hue) + ": ${hue.toInt()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    steps = 359
                )
                Text(
                    text = stringResource(R.string.color_saturation) + ": ${(saturation * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f
                )
                Text(
                    text = stringResource(R.string.color_lightness) + ": ${(lightness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = lightness,
                    onValueChange = { lightness = it },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hslToColor(hue, saturation, lightness)) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun PermissionRow(
    title: String,
    status: String,
    actionVisible: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (actionVisible) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun ringtoneLabel(context: android.content.Context, uri: String): String {
    if (uri.isBlank()) return stringResource(R.string.ringtone_default)
    val ringtone = runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(uri))
    }.getOrNull()
    return ringtone?.getTitle(context) ?: stringResource(R.string.ringtone_default)
}

@Composable
private fun RingtoneRow(
    label: String,
    uri: String,
    onPick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = ringtoneLabel(context, uri),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onPick) {
            Text(stringResource(R.string.choose_ringtone))
        }
    }
}

private fun buildRingtonePickerIntent(
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
private fun BackgroundButtonRow(
    label: String,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(onClick = onOpen) {
            Text(stringResource(R.string.open_settings))
        }
    }
}
