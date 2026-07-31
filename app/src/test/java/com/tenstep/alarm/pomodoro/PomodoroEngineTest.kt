package com.tenstep.alarm.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroEngineTest {

    @Test
    fun `duration in minutes`() {
        assertEquals(25 * 60_000L, PomodoroEngine.durationMs(25))
        assertEquals(60_000L, PomodoroEngine.durationMs(1))
        assertEquals(60_000L, PomodoroEngine.durationMs(0)) // clamps to >= 1
    }

    @Test
    fun `remaining time is computed from elapsed`() {
        assertEquals(
            5_000L,
            PomodoroEngine.remainingMs(totalMs = 10_000L, startElapsedMs = 1_000L, nowElapsedMs = 6_000L)
        )
    }

    @Test
    fun `remaining time clamps at zero`() {
        assertEquals(
            0L,
            PomodoroEngine.remainingMs(totalMs = 1_000L, startElapsedMs = 0L, nowElapsedMs = 5_000L)
        )
    }

    @Test
    fun `next mode alternates`() {
        assertEquals(PomodoroMode.BREAK, PomodoroEngine.nextMode(PomodoroMode.FOCUS))
        assertEquals(PomodoroMode.FOCUS, PomodoroEngine.nextMode(PomodoroMode.BREAK))
    }
}