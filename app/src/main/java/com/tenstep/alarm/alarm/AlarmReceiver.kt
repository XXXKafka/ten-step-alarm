package com.tenstep.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager when a primary alarm or a snooze is due.
 * Starts the ringing foreground service and force-shows the full-screen
 * ringing activity (waking the screen even when it is off/locked); the
 * full-screen-intent notification is the fallback for background launches.
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

        // Force the screen on (even when off/locked) while showing the page.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
            "TenStepAlarm:ShowRinging"
        )
        runCatching { wakeLock.acquire(8000) }

        // Best-effort direct launch (exact-alarm broadcasts get a temporary
        // background activity-start allowlist). The full-screen-intent
        // notification covers the remaining cases.
        try {
            val activity = Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(activity)
        } catch (_: Exception) {
            // Fallback: the full-screen-intent notification will show it.
        }
    }

    companion object {
        const val ACTION_ALARM = "com.tenstep.alarm.action.ALARM"
        const val ACTION_SNOOZE = "com.tenstep.alarm.action.SNOOZE"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_SNOOZE = "extra_snooze"
    }
}