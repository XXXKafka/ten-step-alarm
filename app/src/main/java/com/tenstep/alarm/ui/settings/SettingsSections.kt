package com.tenstep.alarm.ui.settings

import android.Manifest
import android.app.Activity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tenstep.alarm.BuildConfig
import com.tenstep.alarm.R
import com.tenstep.alarm.data.ThemeMode
import com.tenstep.alarm.ui.theme.ThemePresetSeeds
import com.tenstep.alarm.util.LocaleHelper

@Composable
fun LanguageSection(viewModel: SettingsViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    SettingsCard(title = stringResource(R.string.language)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageChip(
                label = stringResource(R.string.lang_system),
                selected = language == LocaleHelper.LANG_SYSTEM,
                onClick = {
                    viewModel.setLanguage(LocaleHelper.LANG_SYSTEM)
                    activity?.recreate()
                }
            )
            LanguageChip(
                label = stringResource(R.string.lang_zh),
                selected = language == LocaleHelper.LANG_ZH,
                onClick = {
                    viewModel.setLanguage(LocaleHelper.LANG_ZH)
                    activity?.recreate()
                }
            )
            LanguageChip(
                label = stringResource(R.string.lang_en),
                selected = language == LocaleHelper.LANG_EN,
                onClick = {
                    viewModel.setLanguage(LocaleHelper.LANG_EN)
                    activity?.recreate()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSection(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeColorSource by viewModel.themeColorSource.collectAsStateWithLifecycle()
    val themePresetIndex by viewModel.themePresetIndex.collectAsStateWithLifecycle()
    val themeCustomColor by viewModel.themeCustomColor.collectAsStateWithLifecycle()
    var showThemeColorPicker by remember { mutableStateOf(false) }

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

    SettingsCard(title = stringResource(R.string.theme_mode)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.AMOLED -> R.string.theme_amoled
                }
                OptionChip(
                    selected = themeMode == mode,
                    label = stringResource(label),
                    onClick = { viewModel.setThemeMode(mode) }
                )
            }
        }
    }

    SettingsCard(title = stringResource(R.string.theme_color)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(
                selected = themeColorSource == "mono",
                label = stringResource(R.string.theme_color_mono),
                onClick = { viewModel.setThemeColorSource("mono") }
            )
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
                                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                                } else {
                                    androidx.compose.material3.MaterialTheme.colorScheme.outline
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
}

@Composable
fun TimeFormatSection(viewModel: SettingsViewModel) {
    val clock24Hour by viewModel.clock24Hour.collectAsStateWithLifecycle()
    SettingsCard(title = stringResource(R.string.time_format_title)) {
        SwitchRow(
            label = stringResource(R.string.clock_24hour),
            checked = clock24Hour,
            onCheckedChange = viewModel::setClock24Hour
        )
    }
}

@Composable
fun AlarmGuardSection(viewModel: SettingsViewModel) {
    val alarmMonitorEnabled by viewModel.alarmMonitorEnabled.collectAsStateWithLifecycle()
    SettingsCard(title = "") {
        SwitchRow(
            label = stringResource(R.string.alarm_monitor_setting),
            checked = alarmMonitorEnabled,
            onCheckedChange = viewModel::setAlarmMonitorEnabled
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.alarm_monitor_desc),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PomodoroSection(viewModel: SettingsViewModel) {
    val focusMinutes by viewModel.focusMinutes.collectAsStateWithLifecycle()
    val breakMinutes by viewModel.breakMinutes.collectAsStateWithLifecycle()
    val pomodoroFocusRingtone by viewModel.pomodoroFocusRingtone.collectAsStateWithLifecycle()
    val pomodoroBreakRingtone by viewModel.pomodoroBreakRingtone.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingRingtoneTarget by remember { mutableStateOf(0) }

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

    SettingsCard(title = "") {
        Text(
            text = stringResource(R.string.focus_minutes) + ": $focusMinutes",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
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
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
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
        Spacer(Modifier.height(8.dp))
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
}

@Composable
fun SnoozeSection(viewModel: SettingsViewModel) {
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsStateWithLifecycle()
    SettingsCard(title = "") {
        Text(
            text = stringResource(R.string.snooze_minutes) + ": $snoozeMinutes",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = snoozeMinutes.toFloat(),
            onValueChange = { viewModel.setSnoozeMinutes(it.toInt()) },
            valueRange = 1f..30f,
            steps = 28
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClockSection(viewModel: SettingsViewModel) {
    val clockShowSeconds by viewModel.clockShowSeconds.collectAsStateWithLifecycle()
    val clockShowDate by viewModel.clockShowDate.collectAsStateWithLifecycle()
    val clockStyle by viewModel.clockStyle.collectAsStateWithLifecycle()
    val clockCustomColor by viewModel.clockCustomColor.collectAsStateWithLifecycle()
    val clockFontScale by viewModel.clockFontScale.collectAsStateWithLifecycle()
    val clockFullscreen by viewModel.clockFullscreen.collectAsStateWithLifecycle()
    var showClockColorPicker by remember { mutableStateOf(false) }

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
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
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
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
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
}

@Composable
fun PermissionsSection(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val defaultRingtone by viewModel.defaultRingtone.collectAsStateWithLifecycle()
    var canScheduleExact by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

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
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ringtone_default),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = ringtoneLabel(context, defaultRingtone),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = {
                ringtoneLauncher.launch(
                    buildRingtonePickerIntent(context, defaultRingtone)
                )
            }) {
                Text(stringResource(R.string.choose_ringtone))
            }
        }
    }
}

@Composable
fun XiaomiSection(viewModel: SettingsViewModel) {
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

@Composable
fun DebugSection(viewModel: SettingsViewModel) {
    val debugSimulate by viewModel.debugSimulateSteps.collectAsStateWithLifecycle()
    if (!BuildConfig.DEBUG) return
    SettingsCard(title = "") {
        SwitchRow(
            label = stringResource(R.string.debug_simulate),
            checked = debugSimulate,
            onCheckedChange = viewModel::setDebugSimulateSteps
        )
    }
}