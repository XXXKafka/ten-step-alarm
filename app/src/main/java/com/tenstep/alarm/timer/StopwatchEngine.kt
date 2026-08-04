package com.tenstep.alarm.timer

/** Pure stopwatch math (unit-testable). */
object StopwatchEngine {

    /** Elapsed time given an accumulated baseline and a running session start. */
    fun elapsedMs(accumulatedMs: Long, startElapsedMs: Long, nowElapsedMs: Long): Long =
        accumulatedMs + (nowElapsedMs - startElapsedMs).coerceAtLeast(0L)

    /** Formats mm:ss.cc (hundredths). */
    fun format(ms: Long): String {
        val totalCs = (ms / 10).coerceAtLeast(0)
        val centis = totalCs % 100
        val totalSeconds = totalCs / 100
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return "%02d:%02d.%02d".format(minutes, seconds, centis)
    }
}