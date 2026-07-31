package com.tenstep.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a primary alarm or a snooze is due.
 * Starts the ringing foreground service and (best effort) opens the
 * full-screen ringing activity; the full-screen-intent notification is the
 * fallback that works from the background on all API levels.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        if (alarmId < 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TenStepApplication
                val repository = app.container.alarmRepository
                val alarm = repository.getAlarm(alarmId)
                if (alarm != null) {
                    startRinging(context, alarm, snooze)
                    if (!snooze) {
                        if (alarm.isOneTime() && alarm.enabled) {
                            // One-shot alarms are automatically disabled after firing.
                            repository.setEnabled(alarm, enabled = false)
                        } else if (!alarm.isOneTime()) {
                            // Repeating alarms: schedule the next occurrence.
                            repository.reschedule(alarm)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startRinging(context: Context, alarm: AlarmEntity, snooze: Boolean) {
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_SNOOZE, snooze)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)

        // Best-effort direct launch (works while the exact-alarm allowlist is
        // active). The full-screen-intent notification covers the rest.
        try {
            val activity = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(activity)
        } catch (_: Exception) {
            // Background activity start blocked; the notification will show it.
        }
    }

    companion object {
        const val ACTION_ALARM = "com.tenstep.alarm.action.ALARM"
        const val ACTION_SNOOZE = "com.tenstep.alarm.action.SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_SNOOZE = "extra_snooze"
    }
}