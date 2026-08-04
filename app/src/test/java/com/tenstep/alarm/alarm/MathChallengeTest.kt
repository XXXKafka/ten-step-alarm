package com.tenstep.alarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathChallengeTest {

    private val challenge = MathChallenge(Random(42))

    @Test
    fun `problems are two-digit with correct answers`() {
        repeat(200) {
            val p = challenge.nextProblem()
            assertTrue(p.a in 10..99)
            assertTrue(p.b in 10..99)
            val expected = if (p.op == '+') p.a + p.b else p.a - p.b
            assertEquals(expected, p.answer)
        }
    }

    @Test
    fun `check accepts the correct answer`() {
        val p = challenge.nextProblem()
        assertTrue(p.check(p.answer))
    }

    @Test
    fun `check rejects a wrong answer`() {
        val p = challenge.nextProblem()
        assertTrue(!p.check(p.answer + 1))
    }
}