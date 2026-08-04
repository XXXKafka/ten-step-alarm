package com.tenstep.alarm.ui.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class FlipTimeTest {

    private fun at(hour: Int, minute: Int, second: Int = 0) =
        LocalDateTime.of(2026, 8, 5, hour, minute, second)

    @Test
    fun `24 hour no seconds`() {
        val cells = FlipTime.cells(at(7, 5), is24Hour = true, showSeconds = false)
        assertEquals(listOf('0', '7', ':', '0', '5'), cells.digits)
        assertNull(cells.amPm)
    }

    @Test
    fun `24 hour with seconds`() {
        val cells = FlipTime.cells(at(7, 5, 9), is24Hour = true, showSeconds = true)
        assertEquals(listOf('0', '7', ':', '0', '5', ':', '0', '9'), cells.digits)
    }

    @Test
    fun `24 hour midnight is 00`() {
        val cells = FlipTime.cells(at(0, 15), is24Hour = true, showSeconds = false)
        assertEquals(listOf('0', '0', ':', '1', '5'), cells.digits)
    }

    @Test
    fun `12 hour midnight is 12 AM`() {
        val cells = FlipTime.cells(at(0, 15), is24Hour = false, showSeconds = false)
        assertEquals(listOf('1', '2', ':', '1', '5'), cells.digits)
        assertEquals("AM", cells.amPm)
    }

    @Test
    fun `12 hour morning`() {
        val cells = FlipTime.cells(at(9, 5), is24Hour = false, showSeconds = false)
        assertEquals(listOf('0', '9', ':', '0', '5'), cells.digits)
        assertEquals("AM", cells.amPm)
    }

    @Test
    fun `12 hour afternoon`() {
        val cells = FlipTime.cells(at(13, 5), is24Hour = false, showSeconds = false)
        assertEquals(listOf('0', '1', ':', '0', '5'), cells.digits)
        assertEquals("PM", cells.amPm)
    }

    @Test
    fun `12 hour noon is 12 PM`() {
        val cells = FlipTime.cells(at(12, 0), is24Hour = false, showSeconds = false)
        assertEquals(listOf('1', '2', ':', '0', '0'), cells.digits)
        assertEquals("PM", cells.amPm)
    }
}