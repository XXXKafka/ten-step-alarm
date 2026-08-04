package com.tenstep.alarm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tenstep.alarm.ui.navigation.AppRoot
import androidx.lifecycle.lifecycleScope
import com.tenstep.alarm.data.SettingsStore
import com.tenstep.alarm.ui.theme.AppSettingsTheme
import com.tenstep.alarm.util.LocaleHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded(this)
        requestActivityRecognitionIfNeeded(this)
        requestAudioReadPermissionIfNeeded(this)

        val container = (application as TenStepApplication).container

        // If the exact-alarm permission is available, re-assert every enabled
        // alarm (covers the case where it was granted while the app was closed).
        lifecycleScope.launch {
            if (container.scheduler.canScheduleExact()) {
                container.alarmRepository.rescheduleAll()
            }
        }

        setContent {
            AppSettingsTheme(settingsStore = container.settingsStore) {
                AppRoot()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS
                )
            }
        }
    }

    /**
     * Xiaomi/MIUI stores the default alarm sound behind a media URI that the
     * app can only open with the audio read permission; without it, alarms
     * using the default ringtone would ring silently on those devices.
     */
    private fun requestAudioReadPermissionIfNeeded(activity: Activity) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(activity, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                REQUEST_AUDIO_READ
            )
        }
    }

    private fun requestActivityRecognitionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    REQUEST_ACTIVITY_RECOGNITION
                )
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
        private const val REQUEST_ACTIVITY_RECOGNITION = 101
        private const val REQUEST_AUDIO_READ = 102
    }
}