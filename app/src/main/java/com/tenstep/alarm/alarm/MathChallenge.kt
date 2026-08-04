package com.tenstep.alarm.alarm

import kotlin.random.Random

/**
 * Generates simple two-digit addition/subtraction problems used by the MATH
 * alarm challenge. Pure logic (injectable [Random]) so it can be unit-tested.
 */
class MathChallenge(private val random: Random = Random.Default) {

    data class Problem(val a: Int, val b: Int, val op: Char, val answer: Int) {
        fun check(input: Int): Boolean = input == answer

        /** Human-readable expression without the answer, e.g. "34 + 57". */
        fun text(): String = "$a $op $b"
    }

    fun nextProblem(): Problem {
        val a = random.nextInt(10, 100)
        val b = random.nextInt(10, 100)
        return if (random.nextBoolean()) {
            Problem(a, b, '+', a + b)
        } else {
            Problem(a, b, '-', a - b)
        }
    }
}