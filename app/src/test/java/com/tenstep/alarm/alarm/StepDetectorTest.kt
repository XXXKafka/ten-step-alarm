package com.tenstep.alarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class StepDetectorTest {

    private fun calibratedState() = StepDetector.State(
        lowPass = 9.8f,
        prevHighPass = 0f,
        lastStepTimeMs = 0L,
        steps = 0
    )

    @Test
    fun `counts two separated peaks`() {
        val state = calibratedState()
        assertEquals(1, StepDetector.process(12f, 1_000, state))
        // Valley between the peaks.
        assertEquals(1, StepDetector.process(9.8f, 1_400, state))
        assertEquals(2, StepDetector.process(12.5f, 2_000, state))
    }

    @Test
    fun `ignores peaks too close together`() {
        val state = calibratedState()
        StepDetector.process(12f, 1_000, state)
        StepDetector.process(12.5f, 1_100, state)
        assertEquals(1, state.steps)
    }

    @Test
    fun `no step below threshold`() {
        val state = calibratedState()
        StepDetector.process(10f, 1_000, state)
        assertEquals(0, state.steps)
    }

    @Test
    fun `falling edge alone is not a step`() {
        val state = calibratedState()
        StepDetector.process(9.8f, 1_000, state)
        assertEquals(0, state.steps)
    }
}