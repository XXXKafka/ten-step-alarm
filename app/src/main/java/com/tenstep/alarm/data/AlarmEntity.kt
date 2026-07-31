package com.tenstep.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/** Bitmask of the seven weekdays: bit 0 = Monday … bit 6 = Sunday. */
object RepeatDays {
    const val MON = 1 shl 0
    const val TUE = 1 shl 1
    const val WED = 1 shl 2
    const val THU = 1 shl 3
    const val FRI = 1 shl 4
    const val SAT = 1 shl 5
    const val SUN = 1 shl 6
    const val ALL = MON or TUE or WED or THU or FRI or SAT or SUN
    const val WORKDAYS = MON or TUE or WED or THU or FRI

    val ALL_DAYS_LIST: List<Int> = listOf(MON, TUE, WED, THU, FRI, SAT, SUN)

    fun dayBit(dayOfWeek: DayOfWeek): Int = 1 shl ((dayOfWeek.value + 6) % 7)

    /** Index into the [day]-indexed resource arrays: 0 = Monday … 6 = Sunday. */
    fun indexOf(bit: Int): Int = Integer.numberOfTrailingZeros(bit)
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    /** [RepeatDays] bitmask; 0 means "no repeat" (one-shot alarm). */
    val daysOfWeek: Int,
    val label: String,
    val ringtoneUri: String,
    /** Ringtone volume scale, 0..100. */
    val volume: Int,
    val vibrate: Boolean,
    val enabled: Boolean,
    val oneShot: Boolean,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** True when the alarm was created without any repeating weekday. */
    fun isOneTime(): Boolean = oneShot || daysOfWeek == 0
}