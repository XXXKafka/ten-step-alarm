package com.tenstep.alarm.ui.alarm

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tenstep.alarm.TenStepApplication
import com.tenstep.alarm.data.AlarmEntity
import com.tenstep.alarm.data.ChallengeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the challenge type / step target survive a save + read-back roundtrip. */
@RunWith(AndroidJUnit4::class)
class AlarmEditChallengeRoundtripTest {

    @Test
    fun challengeTypeAndStepTargetPersist() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TenStepApplication>()
        val repository = app.container.alarmRepository

        val alarm = AlarmEntity(
            hour = 6,
            minute = 30,
            daysOfWeek = 0,
            label = "roundtrip-test",
            ringtoneUri = "",
            volume = 70,
            vibrate = true,
            snoozeEnabled = true,
            enabled = true,
            oneShot = true,
            challengeType = ChallengeType.MATH,
            stepTarget = 10
        )
        val id = repository.upsert(alarm)
        try {
            val loaded = repository.getAlarm(id)
            assertEquals(ChallengeType.MATH, loaded?.challengeType)
            assertEquals(10, loaded?.stepTarget)
        } finally {
            repository.getAlarm(id)?.let { repository.delete(it) }
        }
    }
}