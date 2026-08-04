package com.tenstep.alarm.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {

    @Test
    fun `duration in minutes`() {
        assertEquals(5 * 60_000L, TimerEngine.durationMs(5))
        assertEquals(60_000L, TimerEngine.durationMs(1))
        assertEquals(60_000L, TimerEngine.durationMs(0)) // clamps to >= 1
    }

    @Test
    fun `remaining time is computed from elapsed`() {
        assertEquals(
            5_000L,
            TimerEngine.remainingMs(totalMs = 10_000L, startElapsedMs = 1_000L, nowElapsedMs = 6_000L)
        )
    }

    @Test
    fun `remaining time clamps at zero`() {
        assertEquals(
            0L,
            TimerEngine.remainingMs(totalMs = 1_000L, startElapsedMs = 0L, nowElapsedMs = 5_000L)
        )
    }
}