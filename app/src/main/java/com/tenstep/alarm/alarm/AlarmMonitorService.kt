package com.tenstep.alarm.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.tenstep.alarm.util.Notifications

/**
 * Persistent low-key foreground service ("alarm guard").
 *
 * On aggressive OEMs (Xiaomi MIUI/HyperOS) swiping the app away from Recents
 * kills the process and blocks it from restarting, so the alarm receiver never
 * runs. Keeping this foreground service alive lets the process stay resident
 * and the exact-alarm broadcast still deliver, so the ringing page can pop up
 * even after the app was "closed" from Recents.
 *
 * (A true force-stop from Settings still blocks all alarms by platform rule.)
 */
class AlarmMonitorService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = Notifications.monitorNotification(this)
        ServiceCompat.startForeground(
            this,
            Notifications.MONITOR_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        return START_STICKY
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_STOP = "com.tenstep.alarm.action.STOP_MONITOR"

        fun start(context: Context) {
            runCatching {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmMonitorService::class.java)
                )
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, AlarmMonitorService::class.java))
            }
        }
    }
}