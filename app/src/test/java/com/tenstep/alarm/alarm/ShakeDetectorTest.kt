package com.tenstep.alarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class ShakeDetectorTest {

    private fun calibratedState() = ShakeDetector.State(
        lowPass = 9.8f,
        prevHighPass = 0f,
        lastShakeTimeMs = 0L,
        shakes = 0
    )

    @Test
    fun `counts two separated shakes`() {
        val state = calibratedState()
        assertEquals(1, ShakeDetector.process(14f, 1_000, state))
        assertEquals(1, ShakeDetector.process(9.8f, 1_300, state))
        assertEquals(2, ShakeDetector.process(6f, 2_000, state))
    }

    @Test
    fun `ignores shakes too close together`() {
        val state = calibratedState()
        ShakeDetector.process(14f, 1_000, state)
        ShakeDetector.process(6f, 1_100, state)
        assertEquals(1, state.shakes)
    }

    @Test
    fun `no shake below threshold`() {
        val state = calibratedState()
        ShakeDetector.process(11f, 1_000, state)
        assertEquals(0, state.shakes)
    }

    @Test
    fun `small drift does not count`() {
        val state = calibratedState()
        ShakeDetector.process(10.2f, 1_000, state)
        ShakeDetector.process(9.6f, 1_500, state)
        assertEquals(0, state.shakes)
    }
}