package com.tenstep.alarm.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tenstep.alarm.TenStepApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fired by the system when the user grants (or revokes) the exact-alarm
 * permission. Re-schedules every enabled alarm as soon as scheduling becomes
 * possible, so alarms work without reopening the app.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TenStepApplication
                if (app.container.scheduler.canScheduleExact()) {
                    app.container.alarmRepository.rescheduleAll()
                }
                // Granting the permission may also make the alarm guard useful.
                MonitorController.refresh(app)
            } finally {
                pendingResult.finish()
            }
        }
    }
}