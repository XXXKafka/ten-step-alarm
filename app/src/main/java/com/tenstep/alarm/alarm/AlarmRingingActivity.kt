package com.tenstep.alarm.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tenstep.alarm.R
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.ui.ringing.RingingScreen
import com.tenstep.alarm.ui.ringing.RingingViewModel
import com.tenstep.alarm.ui.theme.AppSettingsTheme
import com.tenstep.alarm.util.LocaleHelper
import kotlinx.coroutines.delay

/**
 * Full-screen, lock-screen-aware UI shown while an alarm is ringing.
 *
 * The activity can be launched (by the receiver or by the full-screen-intent
 * notification) slightly before the ringing service publishes its session, so
 * instead of closing immediately it waits up to a few seconds for the session
 * and only finishes if nothing ever starts ringing.
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show above the lock screen, keep the screen on, force fullscreen.
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

        val container = (application as TenStepApplication).container
        setContent {
            AppSettingsTheme(settingsStore = container.settingsStore) {
                val vm: RingingViewModel = viewModel()
                val active by RingingSession.active.collectAsStateWithLifecycle()

                // If we arrived before the service published its session, wait
                // a short while; if nothing ever rings, close.
                LaunchedEffect(Unit) {
                    if (!RingingSession.active.value) {
                        delay(4000)
                        if (!RingingSession.active.value) finish()
                    }
                }

                if (active) {
                    RingingScreen(viewModel = vm, onClose = { finish() })
                } else {
                    RingingWaitingScreen()
                }
            }
        }
    }

    override fun onBackPressed() {
        // Keep ringing; the alarm must be dismissed via the buttons.
    }
}

@Composable
private fun RingingWaitingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.ringing_waiting),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}