package com.tenstep.alarm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tenstep.alarm.ui.navigation.AppRoot
import com.tenstep.alarm.ui.theme.AppSettingsTheme
import com.tenstep.alarm.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded(this)
        requestActivityRecognitionIfNeeded(this)
        requestIgnoreBatteryOptimizations(this)

        val container = (application as TenStepApplication).container
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

    /** Ask for the battery optimization exemption (helps exact alarms & the
     *  ringing service survive Doze / aggressive OEM battery savers). */
    private fun requestIgnoreBatteryOptimizations(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(activity.packageName)) {
                runCatching {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 100
        private const val REQUEST_ACTIVITY_RECOGNITION = 101
    }
}