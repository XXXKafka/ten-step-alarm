package com.tenstep.alarm.alarm

import android.content.Context
import com.tenstep.alarm.TenStepApplication
import kotlinx.coroutines.flow.first

/**
 * Decides whether the alarm guard foreground service should run.
 *
 * The guard only runs when the user enabled it AND at least one alarm is
 * enabled, so it does not waste battery when there is nothing to guard.
 * Used by [com.tenstep.alarm.TenStepApplication] reactively and by
 * [BootReceiver] / [ExactAlarmPermissionReceiver] after boot or permission
 * changes.
 */
object MonitorController {

    suspend fun refresh(context: Context) {
        val app = context.applicationContext as TenStepApplication
        val enabled = app.container.settingsStore.alarmMonitorEnabled.first()
        val enabledAlarms = app.container.alarmRepository.observeEnabledCount().first()
        if (enabled && enabledAlarms > 0) {
            AlarmMonitorService.start(app)
        } else {
            AlarmMonitorService.stop(app)
        }
    }
}