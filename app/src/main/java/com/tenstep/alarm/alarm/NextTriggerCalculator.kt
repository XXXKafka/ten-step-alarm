package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.RepeatDays
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Pure date-time math for alarms (no Android dependencies, unit-testable).
 */
object NextTriggerCalculator {

    /**
     * Next occurrence of [hour]:[minute] after [from].
     *
     * @param oneShot when true only the next single occurrence (today or
     *   tomorrow) is considered; repeating alarms look up to 7 days ahead.
     */
    fun nextTrigger(
        hour: Int,
        minute: Int,
        daysOfWeek: Int,
        oneShot: Boolean,
        from: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? {
        val zone = from.zone
        val maxDays = if (oneShot) 1 else 7
        for (offset in 0..maxDays) {
            val date = from.toLocalDate().plusDays(offset.toLong())
            val selected = (daysOfWeek and RepeatDays.ALL) != 0
            if (selected && (daysOfWeek and RepeatDays.dayBit(date.dayOfWeek)) == 0) continue
            val candidate = LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone)
            if (candidate.isAfter(from)) return candidate
        }
        return null
    }
}