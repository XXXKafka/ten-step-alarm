package com.tenstep.alarm.alarm

import com.tenstep.alarm.data.ChallengeType

/**
 * Pure rule for "can the alarm be dismissed yet", per challenge type.
 * Extracted so it can be unit-tested without Android dependencies.
 */
object ChallengeEvaluator {

    fun isSatisfied(
        type: ChallengeType,
        steps: Int,
        stepTarget: Int,
        shakes: Int,
        shakeTarget: Int,
        mathSolved: Boolean,
        qrScanned: Boolean
    ): Boolean = when (type) {
        ChallengeType.STEPS -> steps >= stepTarget
        // QR is satisfied by scanning any code, or by the step fallback when
        // the camera cannot be used.
        ChallengeType.QR -> qrScanned || steps >= stepTarget
        ChallengeType.MATH -> mathSolved
        ChallengeType.SHAKE -> shakes >= shakeTarget
    }
}