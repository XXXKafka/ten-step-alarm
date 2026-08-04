package com.tenstep.alarm.timer

/** Pure countdown math for the timer (unit-testable). */
object TimerEngine {
    fun durationMs(minutes: Int): Long = minutes.coerceAtLeast(1) * 60_000L

    fun remainingMs(totalMs: Long, startElapsedMs: Long, nowElapsedMs: Long): Long =
        (totalMs - (nowElapsedMs - startElapsedMs)).coerceAtLeast(0L)
}