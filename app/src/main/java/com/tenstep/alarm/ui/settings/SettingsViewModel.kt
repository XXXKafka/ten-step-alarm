package com.tenstep.alarm.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.ThemeMode
import com.tenstep.alarm.util.LocaleHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TenStepApplication).container
    private val settings = container.settingsStore
    private val context = application

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val themeColorSource: StateFlow<String> = settings.themeColorSource
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "dynamic")

    val themePresetIndex: StateFlow<Int> = settings.themePresetIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val themeCustomColor: StateFlow<Long> = settings.themeCustomColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFF006A60L)

    val language: StateFlow<String> = settings.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocaleHelper.LANG_SYSTEM)

    val clockShowSeconds: StateFlow<Boolean> = settings.clockShowSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val clockShowDate: StateFlow<Boolean> = settings.clockShowDate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val clock24Hour: StateFlow<Boolean> = settings.clock24Hour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val clockStyle: StateFlow<String> = settings.clockStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "auto")

    val clockCustomColor: StateFlow<Long> = settings.clockCustomColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0xFF006A60L)

    val clockFontScale: StateFlow<Float> = settings.clockFontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    val clockFullscreen: StateFlow<Boolean> = settings.clockFullscreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pomodoroFocusRingtone: StateFlow<String> = settings.pomodoroFocusRingtone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val pomodoroBreakRingtone: StateFlow<String> = settings.pomodoroBreakRingtone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val focusMinutes: StateFlow<Int> = settings.focusMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 25)

    val breakMinutes: StateFlow<Int> = settings.breakMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val snoozeMinutes: StateFlow<Int> = settings.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    val debugSimulateSteps: StateFlow<Boolean> = settings.debugSimulateSteps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val defaultRingtone: StateFlow<String> = settings.defaultRingtone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setThemeColorSource(source: String) {
        viewModelScope.launch { settings.setThemeColorSource(source) }
    }

    fun setThemePresetIndex(index: Int) {
        viewModelScope.launch { settings.setThemePresetIndex(index) }
    }

    fun setThemeCustomColor(argb: Long) {
        viewModelScope.launch { settings.setThemeCustomColor(argb) }
    }

    /** Persists the language and recreates the current activity (done by the UI). */
    fun setLanguage(language: String) {
        LocaleHelper.setLanguage(context, language)
        viewModelScope.launch { settings.setLanguage(language) }
    }

    fun setClockShowSeconds(enabled: Boolean) {
        viewModelScope.launch { settings.setClockShowSeconds(enabled) }
    }

    fun setClockShowDate(enabled: Boolean) {
        viewModelScope.launch { settings.setClockShowDate(enabled) }
    }

    fun setClock24Hour(enabled: Boolean) {
        viewModelScope.launch { settings.setClock24Hour(enabled) }
    }

    fun setClockStyle(style: String) {
        viewModelScope.launch { settings.setClockStyle(style) }
    }

    fun setClockCustomColor(argb: Long) {
        viewModelScope.launch { settings.setClockCustomColor(argb) }
    }

    fun setClockFontScale(scale: Float) {
        viewModelScope.launch { settings.setClockFontScale(scale) }
    }

    fun setClockFullscreen(enabled: Boolean) {
        viewModelScope.launch { settings.setClockFullscreen(enabled) }
    }

    fun setPomodoroFocusRingtone(uri: String) {
        viewModelScope.launch { settings.setPomodoroFocusRingtone(uri) }
    }

    fun setPomodoroBreakRingtone(uri: String) {
        viewModelScope.launch { settings.setPomodoroBreakRingtone(uri) }
    }

    fun setFocusMinutes(minutes: Int) {
        viewModelScope.launch { settings.setFocusMinutes(minutes) }
    }

    fun setBreakMinutes(minutes: Int) {
        viewModelScope.launch { settings.setBreakMinutes(minutes) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { settings.setSnoozeMinutes(minutes) }
    }

    fun setDebugSimulateSteps(enabled: Boolean) {
        viewModelScope.launch { settings.setDebugSimulateSteps(enabled) }
    }

    fun setDefaultRingtone(uri: String) {
        viewModelScope.launch { settings.setDefaultRingtone(uri) }
    }

    fun canScheduleExactAlarms(): Boolean = container.scheduler.canScheduleExact()

    fun openExactAlarmSettings() {
        runCatching {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}