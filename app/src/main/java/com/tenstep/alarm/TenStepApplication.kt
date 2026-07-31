package com.tenstep.alarm

import android.app.Application
import android.content.Context
import com.tenstep.alarm.alarm.AlarmScheduler
import com.tenstep.alarm.data.AlarmDatabase
import com.tenstep.alarm.data.AlarmRepository
import com.tenstep.alarm.data.SettingsStore
import com.tenstep.alarm.util.LocaleHelper
import com.tenstep.alarm.util.Notifications

/**
 * Application entry point. Owns the simple service locator ([AppContainer]) so
 * Activity/Service/Receiver code can share one Room database, one DataStore and
 * one AlarmManager-backed scheduler.
 */
class TenStepApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.apply(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}

/** Simple manual dependency container (no DI framework). */
class AppContainer(context: Context) {
    val database: AlarmDatabase = AlarmDatabase.build(context)
    val settingsStore: SettingsStore = SettingsStore(context)
    val scheduler: AlarmScheduler = AlarmScheduler(context)
    val alarmRepository: AlarmRepository = AlarmRepository(database.alarmDao(), scheduler)
}