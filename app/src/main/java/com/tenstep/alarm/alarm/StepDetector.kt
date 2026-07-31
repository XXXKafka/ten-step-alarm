package com.tenstep.alarm.alarm

import kotlin.math.sqrt

/**
 * Accelerometer-based step estimation used when the system step counter
 * (TYPE_STEP_COUNTER) is missing or the ACTIVITY_RECOGNITION permission was
 * denied. Pure logic (no Android APIs) so it can be unit-tested.
 *
 * Algorithm: low-pass filter the acceleration magnitude, then count rising
 * edges of the high-pass signal above a threshold, enforcing a minimum time
 * between steps.
 */
object StepDetector {

    /** Low-pass factor; higher = smoother, less responsive. */
    const val ALPHA = 0.8f

    /** High-pass magnitude (m/s^2) above which a peak counts as a step. */
    const val THRESHOLD = 1.5f

    /** Minimum time between two steps, in milliseconds. */
    const val MIN_STEP_INTERVAL_MS = 250L

    class State(
        var lowPass: Float = 0f,
        var prevHighPass: Float = 0f,
        var lastStepTimeMs: Long = 0L,
        var steps: Int = 0
    )

    /** Feeds one accelerometer sample; returns the current step count. */
    fun process(magnitude: Float, timeMs: Long, state: State): Int {
        state.lowPass = ALPHA * state.lowPass + (1 - ALPHA) * magnitude
        val highPass = magnitude - state.lowPass
        val stepDetected =
            highPass > THRESHOLD &&
                state.prevHighPass <= THRESHOLD &&
                (timeMs - state.lastStepTimeMs) >= MIN_STEP_INTERVAL_MS
        if (stepDetected) {
            state.steps++
            state.lastStepTimeMs = timeMs
        }
        state.prevHighPass = highPass
        return state.steps
    }

    /** Convenience: magnitude from raw x/y/z accelerometer values. */
    fun magnitude(x: Float, y: Float, z: Float): Float =
        sqrt(x * x + y * y + z * z)
}