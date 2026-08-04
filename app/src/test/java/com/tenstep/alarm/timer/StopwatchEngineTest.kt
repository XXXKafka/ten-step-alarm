package com.tenstep.alarm.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchEngineTest {

    @Test
    fun `elapsed adds accumulated and current session`() {
        assertEquals(
            1_500L,
            StopwatchEngine.elapsedMs(accumulatedMs = 1_000L, startElapsedMs = 10_000L, nowElapsedMs = 10_500L)
        )
    }

    @Test
    fun `elapsed never goes negative`() {
        assertEquals(
            1_000L,
            StopwatchEngine.elapsedMs(accumulatedMs = 1_000L, startElapsedMs = 10_000L, nowElapsedMs = 9_000L)
        )
    }

    @Test
    fun `format is mm ss hundredths`() {
        assertEquals("00:00.00", StopwatchEngine.format(0))
        assertEquals("00:01.23", StopwatchEngine.format(1_230))
        assertEquals("01:02.03", StopwatchEngine.format(62_030))
    }
}