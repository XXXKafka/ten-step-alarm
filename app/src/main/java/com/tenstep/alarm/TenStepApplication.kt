package com.tenstep.alarm

import android.app.Application
import android.content.Context
import com.tenstep.alarm.alarm.AlarmMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)

        // Smart alarm guard: keep the foreground service running only while the
        // user enabled it AND at least one alarm is enabled.
        appScope.launch {
            combine(
                container.settingsStore.alarmMonitorEnabled,
                container.alarmRepository.observeEnabledCount()
            ) { enabled, count -> enabled && count > 0 }
                .distinctUntilChanged()
                .collect { shouldRun ->
                    if (shouldRun) AlarmMonitorService.start(this@TenStepApplication)
                    else AlarmMonitorService.stop(this@TenStepApplication)
                }
        }
    }
}

/** Simple manual dependency container (no DI framework). */
class AppContainer(context: Context) {
    val database: AlarmDatabase = AlarmDatabase.build(context)
    val settingsStore: SettingsStore = SettingsStore(context)
    val scheduler: AlarmScheduler = AlarmScheduler(context)
    val alarmRepository: AlarmRepository = AlarmRepository(database.alarmDao(), scheduler)
}