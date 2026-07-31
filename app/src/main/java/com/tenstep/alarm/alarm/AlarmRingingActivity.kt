package com.tenstep.alarm.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.ui.ringing.RingingScreen
import com.tenstep.alarm.ui.ringing.RingingViewModel
import com.tenstep.alarm.ui.theme.AppSettingsTheme
import com.tenstep.alarm.util.LocaleHelper

/** Full-screen, lock-screen-aware UI shown while an alarm is ringing. */
class AlarmRingingActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show above the lock screen and keep the screen on.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Immersive mode for the ringing UI.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (!RingingSession.active.value) {
            // Nothing is ringing (e.g. stale full-screen intent): close.
            finish()
            return
        }

        val container = (application as TenStepApplication).container
        setContent {
            AppSettingsTheme(settingsStore = container.settingsStore) {
                val vm: RingingViewModel = viewModel()
                RingingScreen(viewModel = vm, onClose = { finish() })
            }
        }
    }

    override fun onBackPressed() {
        // Keep ringing; the alarm must be dismissed via the buttons.
    }
}