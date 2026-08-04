package com.tenstep.alarm.ui.ringing

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.alarm.RingingSession
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TenStepApplication::class, sdk = [34])
class RingingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        RingingSession.stop()
    }

    private fun app(): TenStepApplication =
        ApplicationProvider.getApplicationContext()

    private fun alarm(challengeType: ChallengeType = ChallengeType.STEPS) = AlarmEntity(
        hour = 7,
        minute = 0,
        daysOfWeek = 0,
        label = "test",
        ringtoneUri = "",
        volume = 70,
        vibrate = true,
        snoozeEnabled = true,
        enabled = true,
        oneShot = true,
        challengeType = challengeType,
        stepTarget = 10
    )

    @Test
    fun `snooze schedules a snooze when enabled`() = runTest(dispatcher) {
        RingingSession.start(alarm(), snooze = false)
        val viewModel = RingingViewModel(app())

        viewModel.snooze()
        advanceUntilIdle()

        val shadowAlarm = Shadows.shadowOf(
            app().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        )
        assertTrue(shadowAlarm.scheduledAlarms.isNotEmpty())
    }

    @Test
    fun `snooze does not schedule when disabled`() = runTest(dispatcher) {
        RingingSession.start(alarm().copy(snoozeEnabled = false), snooze = false)
        val viewModel = RingingViewModel(app())

        viewModel.snooze()
        advanceUntilIdle()

        val shadowAlarm = Shadows.shadowOf(
            app().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        )
        assertTrue(shadowAlarm.scheduledAlarms.isEmpty())
    }

    @Test
    fun `dismiss keeps the session until the service stops`() = runTest(dispatcher) {
        RingingSession.start(alarm(), snooze = false)
        val viewModel = RingingViewModel(app())

        viewModel.dismiss()
        advanceUntilIdle()

        // The ViewModel only requests the service to stop; without a running
        // service there is nothing to tear down, so the session stays intact.
        assertTrue(RingingSession.active.value)
    }

    @Test
    fun `steps challenge satisfied at target`() = runTest(dispatcher) {
        RingingSession.start(alarm(ChallengeType.STEPS), snooze = false)
        val viewModel = RingingViewModel(app())

        assertFalse(viewModel.challengeSatisfied())
        RingingSession.updateSteps(10)
        assertTrue(viewModel.challengeSatisfied())
    }

    @Test
    fun `math challenge generates a problem and solves it`() = runTest(dispatcher) {
        RingingSession.start(alarm(ChallengeType.MATH), snooze = false)
        val viewModel = RingingViewModel(app())
        advanceUntilIdle()

        val question = viewModel.mathQuestion.value
        assertNotNull(question)
        assertFalse(viewModel.challengeSatisfied())

        viewModel.onMathAnswer(question!!.answer)
        assertTrue(viewModel.challengeSatisfied())
    }

    @Test
    fun `shake challenge satisfied at shake target`() = runTest(dispatcher) {
        RingingSession.start(alarm(ChallengeType.SHAKE), snooze = false)
        val viewModel = RingingViewModel(app())

        assertFalse(viewModel.challengeSatisfied())
        RingingSession.updateShakes(20)
        assertTrue(viewModel.challengeSatisfied())
    }
}