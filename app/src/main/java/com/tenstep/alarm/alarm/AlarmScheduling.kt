package com.tenstep.alarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tenstep.alarm.data.AlarmEntity
import java.time.ZonedDateTime

/**
 * Scheduling contract used by [AlarmRepository]. Abstracted so repository
 * logic can be unit-tested with a fake scheduler.
 */
interface AlarmScheduling {
    fun schedule(alarm: AlarmEntity)
    fun scheduleSnooze(alarmId: Long, minutes: Int)
    fun cancel(alarmId: Long)
    fun canScheduleExact(): Boolean
}

/**
 * Thin wrapper around AlarmManager.
 *
 * Primary alarms use [AlarmManager.setAlarmClock] (exact, shows the alarm icon
 * in the status bar and gets a temporary process allowlist when it fires).
 * Snooze alarms also use setAlarmClock on purpose: the allowlist makes the
 * ringing page pop up reliably even on aggressive OEMs (MIUI/HyperOS).
 */
class AlarmScheduler(private val context: Context) : AlarmScheduling {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: AlarmEntity) {
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

    override fun scheduleSnooze(alarmId: Long, minutes: Int) {
        if (!canScheduleExact()) return
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pi = pendingIntent(alarmId, snooze = true)
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, pi), pi)
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntent(alarmId, snooze = false))
        alarmManager.cancel(pendingIntent(alarmId, snooze = true))
    }

    override fun canScheduleExact(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(alarmId: Long, snooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (snooze) AlarmReceiver.ACTION_SNOOZE else AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_SNOOZE, snooze)
        }
        // Snooze gets the next code so it can never collide with the main alarm
        // (or with another alarm id that shares the lower 19 bits).
        val requestCode = ((alarmId and 0x7FFFF) * 2 + if (snooze) 1 else 0).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}