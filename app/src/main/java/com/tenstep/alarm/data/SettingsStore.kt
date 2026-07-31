package com.tenstep.alarm.data

import android.content.Context
import android.media.RingtoneManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User preferences persisted with Jetpack DataStore. */
class SettingsStore(private val context: Context) {

    companion object {
        /** Default accent color used by the custom color settings (teal). */
        const val DEFAULT_ACCENT_ARGB: Long = 0xFF006A60L
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_COLOR_SOURCE = stringPreferencesKey("theme_color_source") // dynamic|preset|custom
        val THEME_PRESET_INDEX = intPreferencesKey("theme_preset_index")
        val THEME_CUSTOM_COLOR = longPreferencesKey("theme_custom_color")

        val LANGUAGE = stringPreferencesKey("language") // system|zh|en

        val CLOCK_SHOW_SECONDS = booleanPreferencesKey("clock_show_seconds")
        val CLOCK_SHOW_DATE = booleanPreferencesKey("clock_show_date")
        val CLOCK_24_HOUR = booleanPreferencesKey("clock_24_hour")
        val CLOCK_STYLE = stringPreferencesKey("clock_style") // auto|dark|light|custom
        val CLOCK_CUSTOM_COLOR = longPreferencesKey("clock_custom_color")
        val CLOCK_FONT_SCALE = floatPreferencesKey("clock_font_scale")
        val CLOCK_FULLSCREEN = booleanPreferencesKey("clock_fullscreen")

        val POMODORO_FOCUS_RINGTONE = stringPreferencesKey("pomodoro_focus_ringtone")
        val POMODORO_BREAK_RINGTONE = stringPreferencesKey("pomodoro_break_ringtone")

        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val BREAK_MINUTES = intPreferencesKey("break_minutes")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val DEFAULT_RINGTONE = stringPreferencesKey("default_ringtone")
        val DEBUG_SIMULATE_STEPS = booleanPreferencesKey("debug_simulate_steps")
        val ALARM_MONITOR_ENABLED = booleanPreferencesKey("alarm_monitor_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    val themeColorSource: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.THEME_COLOR_SOURCE] ?: "dynamic" }

    val themePresetIndex: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[Keys.THEME_PRESET_INDEX] ?: 0 }

    val themeCustomColor: Flow<Long> =
        context.dataStore.data.map { prefs -> prefs[Keys.THEME_CUSTOM_COLOR] ?: DEFAULT_ACCENT_ARGB }

    val language: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.LANGUAGE] ?: "system" }

    val clockShowSeconds: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_SHOW_SECONDS] ?: false }

    val clockShowDate: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_SHOW_DATE] ?: true }

    val clock24Hour: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_24_HOUR] ?: true }

    val clockStyle: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_STYLE] ?: "auto" }

    val clockCustomColor: Flow<Long> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_CUSTOM_COLOR] ?: DEFAULT_ACCENT_ARGB }

    val clockFontScale: Flow<Float> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_FONT_SCALE] ?: 1f }

    val clockFullscreen: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.CLOCK_FULLSCREEN] ?: false }

    val pomodoroFocusRingtone: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.POMODORO_FOCUS_RINGTONE] ?: defaultAlarmRingtone() }

    val pomodoroBreakRingtone: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.POMODORO_BREAK_RINGTONE] ?: defaultAlarmRingtone() }

    val focusMinutes: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[Keys.FOCUS_MINUTES] ?: 25 }

    val breakMinutes: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[Keys.BREAK_MINUTES] ?: 5 }

    val snoozeMinutes: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[Keys.SNOOZE_MINUTES] ?: 5 }

    val defaultRingtone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_RINGTONE] ?: defaultAlarmRingtone()
    }

    val debugSimulateSteps: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.DEBUG_SIMULATE_STEPS] ?: false }

    /** Keeps the alarm guard foreground service running (see AlarmMonitorService). */
    val alarmMonitorEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.ALARM_MONITOR_ENABLED] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemeColorSource(source: String) {
        context.dataStore.edit { it[Keys.THEME_COLOR_SOURCE] = source }
    }

    suspend fun setThemePresetIndex(index: Int) {
        context.dataStore.edit { it[Keys.THEME_PRESET_INDEX] = index }
    }

    suspend fun setThemeCustomColor(argb: Long) {
        context.dataStore.edit { it[Keys.THEME_CUSTOM_COLOR] = argb }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setClockShowSeconds(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_SHOW_SECONDS] = enabled }
    }

    suspend fun setClockShowDate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_SHOW_DATE] = enabled }
    }

    suspend fun setClock24Hour(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_24_HOUR] = enabled }
    }

    suspend fun setClockStyle(style: String) {
        context.dataStore.edit { it[Keys.CLOCK_STYLE] = style }
    }

    suspend fun setClockCustomColor(argb: Long) {
        context.dataStore.edit { it[Keys.CLOCK_CUSTOM_COLOR] = argb }
    }

    suspend fun setClockFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.CLOCK_FONT_SCALE] = scale.coerceIn(0.7f, 1.5f) }
    }

    suspend fun setClockFullscreen(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOCK_FULLSCREEN] = enabled }
    }

    suspend fun setPomodoroFocusRingtone(uri: String) {
        context.dataStore.edit { it[Keys.POMODORO_FOCUS_RINGTONE] = uri }
    }

    suspend fun setPomodoroBreakRingtone(uri: String) {
        context.dataStore.edit { it[Keys.POMODORO_BREAK_RINGTONE] = uri }
    }

    suspend fun setFocusMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_MINUTES] = minutes }
    }

    suspend fun setBreakMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.BREAK_MINUTES] = minutes }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes }
    }

    suspend fun setDefaultRingtone(uri: String) {
        context.dataStore.edit { it[Keys.DEFAULT_RINGTONE] = uri }
    }

    suspend fun setDebugSimulateSteps(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEBUG_SIMULATE_STEPS] = enabled }
    }

    suspend fun setAlarmMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_MONITOR_ENABLED] = enabled }
    }

    private fun defaultAlarmRingtone(): String =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
}
