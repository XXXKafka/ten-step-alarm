package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.ChallengeType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeEvaluatorTest {

    private fun satisfied(
        type: ChallengeType,
        steps: Int = 0,
        stepTarget: Int = 10,
        shakes: Int = 0,
        shakeTarget: Int = 20,
        mathSolved: Boolean = false,
        qrScanned: Boolean = false
    ) = ChallengeEvaluator.isSatisfied(
        type, steps, stepTarget, shakes, shakeTarget, mathSolved, qrScanned
    )

    @Test
    fun `steps requires reaching the target`() {
        assertFalse(satisfied(ChallengeType.STEPS, steps = 9, stepTarget = 10))
        assertTrue(satisfied(ChallengeType.STEPS, steps = 10, stepTarget = 10))
    }

    @Test
    fun `math requires a solved problem`() {
        assertFalse(satisfied(ChallengeType.MATH))
        assertTrue(satisfied(ChallengeType.MATH, mathSolved = true))
    }

    @Test
    fun `shake requires reaching the shake target`() {
        assertFalse(satisfied(ChallengeType.SHAKE, shakes = 19, shakeTarget = 20))
        assertTrue(satisfied(ChallengeType.SHAKE, shakes = 20, shakeTarget = 20))
    }

    @Test
    fun `qr is satisfied by scanning or by steps fallback`() {
        assertFalse(satisfied(ChallengeType.QR))
        assertTrue(satisfied(ChallengeType.QR, qrScanned = true))
        assertTrue(satisfied(ChallengeType.QR, steps = 10, stepTarget = 10))
    }
}