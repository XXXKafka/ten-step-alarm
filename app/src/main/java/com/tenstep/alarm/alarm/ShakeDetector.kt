package com.tenstep.alarm.alarm

import kotlin.math.abs

/**
 * Accelerometer-based shake counting for the SHAKE alarm challenge.
 * Pure logic (no Android APIs) so it can be unit-tested.
 *
 * A shake is counted whenever the high-pass magnitude deviates from gravity by
 * more than [THRESHOLD] and returns through the threshold, with a minimum time
 * between shakes to reject noise.
 */
object ShakeDetector {

    /** Low-pass factor; higher = smoother, less responsive. */
    const val ALPHA = 0.8f

    /** |high-pass magnitude| (m/s^2) above which a deviation counts as a shake. */
    const val THRESHOLD = 3.0f

    /** Minimum time between two shakes, in milliseconds. */
    const val MIN_INTERVAL_MS = 150L

    class State(
        var lowPass: Float = 0f,
        var prevHighPass: Float = 0f,
        var lastShakeTimeMs: Long = 0L,
        var shakes: Int = 0
    )

    /** Feeds one accelerometer magnitude sample; returns the current shake count. */
    fun process(magnitude: Float, timeMs: Long, state: State): Int {
        state.lowPass = ALPHA * state.lowPass + (1 - ALPHA) * magnitude
        val highPass = magnitude - state.lowPass
        val shakeDetected =
            abs(highPass) > THRESHOLD &&
                abs(state.prevHighPass) <= THRESHOLD &&
                (timeMs - state.lastShakeTimeMs) >= MIN_INTERVAL_MS
        if (shakeDetected) {
            state.shakes++
            state.lastShakeTimeMs = timeMs
        }
        state.prevHighPass = highPass
        return state.shakes
    }
}