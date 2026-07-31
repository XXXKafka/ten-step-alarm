package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.RepeatDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerCalculatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)

    @Test
    fun `one shot same day future time`() {
        val result = NextTriggerCalculator.nextTrigger(
            8, 30, 0, oneShot = true, from = at(7, 1, 7, 0)
        )
        assertEquals(at(7, 1, 8, 30), result)
    }

    @Test
    fun `one shot rolls to tomorrow when time already passed`() {
        val result = NextTriggerCalculator.nextTrigger(
            7, 0, 0, oneShot = true, from = at(7, 1, 8, 0)
        )
        assertEquals(at(7, 2, 7, 0), result)
    }

    @Test
    fun `repeat skips non selected weekdays`() {
        // 2026-07-06 is a Monday; next Wednesday is 2026-07-08.
        val result = NextTriggerCalculator.nextTrigger(
            9, 0, RepeatDays.WED, oneShot = false, from = at(7, 6, 20, 0)
        )
        assertEquals(at(7, 8, 9, 0), result)
    }

    @Test
    fun `repeat wraps around the week`() {
        // 2026-07-05 is a Sunday; next Monday is 2026-07-06.
        val result = NextTriggerCalculator.nextTrigger(
            8, 0, RepeatDays.MON, oneShot = false, from = at(7, 5, 10, 0)
        )
        assertEquals(at(7, 6, 8, 0), result)
    }

    @Test
    fun `repeat considers today when the time is still ahead`() {
        // 2026-07-06 is a Monday.
        val result = NextTriggerCalculator.nextTrigger(
            6, 0, RepeatDays.MON, oneShot = false, from = at(7, 6, 5, 0)
        )
        assertEquals(at(7, 6, 6, 0), result)
    }
}