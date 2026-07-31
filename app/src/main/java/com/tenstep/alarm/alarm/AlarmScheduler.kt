package com.tenstep.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tenstep.alarm.data.AlarmEntity
import java.time.ZonedDateTime

/**
 * Thin wrapper around AlarmManager.
 *
 * Primary alarms use [AlarmManager.setAlarmClock] (exact, shows the alarm icon
 * in the status bar and gets a temporary process allowlist when it fires).
 * Snooze alarms use setExactAndAllowWhileIdle.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: AlarmEntity) {
        if (!alarm.enabled) return
        if (!canScheduleExact()) return
        val trigger = NextTriggerCalculator.nextTrigger(
            hour = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = alarm.daysOfWeek,
            oneShot = alarm.isOneTime(),
            from = ZonedDateTime.now()
        ) ?: return
        val pi = pendingIntent(alarm.id, snooze = false)
        val info = AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), pi)
        alarmManager.setAlarmClock(info, pi)
    }

    /**
     * Schedules a snooze that fires [minutes] from now. Uses setAlarmClock so
     * the broadcast also gets the temporary background activity-start
     * allowlist, making the ringing page pop up even from the background.
     */
    fun scheduleSnooze(alarmId: Long, minutes: Int) {
        if (!canScheduleExact()) return
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pi = pendingIntent(alarmId, snooze = true)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntent(alarmId, snooze = false))
        alarmManager.cancel(pendingIntent(alarmId, snooze = true))
    }

    fun canScheduleExact(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(alarmId: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (snooze) AlarmReceiver.ACTION_SNOOZE else AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze)
        }
        val requestCode = (alarmId and 0xFFFFF).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}