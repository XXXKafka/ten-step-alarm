package com.tenstep.alarm.timer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tenstep.alarm.R
import com.tenstep.alarm.util.Notifications

/** Posts the timer end notification (with sound) even when the app is closed. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER_FINISHED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            Notifications.TIMER_NOTIFICATION_ID,
            Notifications.timerNotification(
                context,
                context.getString(R.string.timer_done_text)
            )
        )
    }

    companion object {
        const val ACTION_TIMER_FINISHED = "com.tenstep.alarm.action.TIMER_FINISHED"
        const val REQUEST_CODE = 3001
    }
}