package com.tenstep.alarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tenstep.alarm.TenStepApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recreates every enabled alarm after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TenStepApplication
                app.container.alarmRepository.rescheduleAll()
                // Keep the alarm guard running after a reboot (only when it
                // should run: setting enabled AND at least one enabled alarm).
                MonitorController.refresh(app)
            } finally {
                pendingResult.finish()
            }
        }
    }
}