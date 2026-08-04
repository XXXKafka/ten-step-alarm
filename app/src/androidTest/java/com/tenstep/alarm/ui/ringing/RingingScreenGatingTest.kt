package com.tenstep.alarm.ui.ringing

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tenstep.alarm.alarm.RingingSession
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the 10-step gate: dismiss is disabled until the step target is met. */
@RunWith(AndroidJUnit4::class)
class RingingScreenGatingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val alarm = AlarmEntity(
            hour = 7,
            minute = 0,
            daysOfWeek = 0,
            label = "gating",
            ringtoneUri = "",
            volume = 70,
            vibrate = true,
            snoozeEnabled = true,
            enabled = true,
            oneShot = true,
            challengeType = ChallengeType.STEPS,
            stepTarget = 10
        )
        RingingSession.start(alarm, snooze = false)
    }

    @After
    fun tearDown() {
        RingingSession.stop()
    }

    @Test
    fun dismissIsDisabledUntilTargetSteps() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        composeRule.setContent {
            RingingScreen(viewModel = RingingViewModel(app), onClose = {})
        }

        composeRule.onNodeWithTag("dismiss_button").assertIsNotEnabled()

        composeRule.runOnUiThread { RingingSession.updateSteps(10) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dismiss_button").assertIsEnabled()
    }
}