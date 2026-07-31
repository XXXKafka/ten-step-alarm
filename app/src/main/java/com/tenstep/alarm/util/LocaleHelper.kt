package com.tenstep.alarm.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language override.
 *
 * The chosen language is persisted synchronously in a tiny SharedPreferences
 * mirror (needed by [apply], which runs during attachBaseContext before the
 * async DataStore is available). The DataStore copy in SettingsStore stays the
 * source of truth for the UI; both are written together by
 * SettingsViewModel.setLanguage().
 */
object LocaleHelper {

    const val LANG_SYSTEM = "system"
    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    private const val PREFS_NAME = "locale_override"
    private const val KEY_LANGUAGE = "language"

    fun currentLanguage(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    /** Wraps [context] with the selected locale (unchanged when "system"). */
    fun apply(context: Context): Context = when (currentLanguage(context)) {
        LANG_ZH -> context.withLocale(Locale.SIMPLIFIED_CHINESE)
        LANG_EN -> context.withLocale(Locale.ENGLISH)
        else -> context
    }

    private fun Context.withLocale(locale: Locale): Context {
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return createConfigurationContext(config)
    }
}