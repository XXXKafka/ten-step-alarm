package com.tenstep.alarm.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tenstep.alarm.R
import com.tenstep.alarm.alarm.AlarmRingingActivity
import com.tenstep.alarm.data.AlarmEntity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Notifications {

    const val CHANNEL_ALARM = "alarm_ringing"
    const val CHANNEL_POMODORO = "pomodoro"
    const val ALARM_NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            context.getString(R.string.notification_channel_alarm),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alarm)
            setShowBadge(false)
            enableVibration(true)
            // The alarm notification (and its full-screen intent) works in DND.
            setBypassDnd(true)
        }
        val pomodoroChannel = NotificationChannel(
            CHANNEL_POMODORO,
            context.getString(R.string.notification_channel_pomodoro),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_pomodoro)
        }
        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(pomodoroChannel)
    }

    fun alarmNotification(context: Context, alarm: AlarmEntity, snooze: Boolean): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlarmRingingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val timeText = LocalTime.of(alarm.hour, alarm.minute)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        val title = if (snooze) {
            context.getString(R.string.ringing_snooze_title)
        } else {
            alarm.label.ifBlank { context.getString(R.string.ringing_title) }
        }

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(timeText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()
    }

    fun pomodoroNotification(context: Context, text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_POMODORO)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(context.getString(R.string.pomodoro_notification_title))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }
}