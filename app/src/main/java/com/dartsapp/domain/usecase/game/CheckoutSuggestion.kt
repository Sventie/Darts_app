package com.dartsapp.domain.usecase.game

import com.dartsapp.domain.model.CloseCondition

private data class DartScore(val label: String, val value: Int)

private val FINISH_SCORES: List<DartScore> = buildList {
    for (n in 20 downTo 1) add(DartScore("D$n", n * 2))
    add(DartScore("D25", 50)) // bullseye
}

private val ALL_SCORES: List<DartScore> = buildList {
    for (n in 20 downTo 1) add(DartScore("T$n", n * 3))
    for (n in 20 downTo 1) add(DartScore("$n", n))
    add(DartScore("Bull", 25))
    add(DartScore("D25", 50))
}

private val ALL_SCORES_SINGLE_OUT: List<DartScore> = buildList {
    for (n in 20 downTo 1) add(DartScore("T$n", n * 3))
    for (n in 20 downTo 1) add(DartScore("D$n", n * 2))
    for (n in 20 downTo 1) add(DartScore("$n", n))
    add(DartScore("Bull", 25))
    add(DartScore("D25", 50))
}

private val FINISH_BY_VALUE_DOUBLE: Map<Int, DartScore> = FINISH_SCORES.associateBy { it.value }
private val FINISH_BY_VALUE_SINGLE: Map<Int, DartScore> = ALL_SCORES_SINGLE_OUT.associateBy { it.value }

object CheckoutSuggestion {

    /**
     * Returns a list of dart labels (e.g. ["T20", "T20", "D20"]) representing
     * a suggested checkout path, or null if no checkout is possible within the
     * given number of darts.
     */
    fun suggest(
        remaining: Int,
        dartsLeft: Int,
        closeCondition: CloseCondition
    ): List<String>? {
        if (remaining <= 0 || dartsLeft <= 0) return null
        return when (closeCondition) {
            CloseCondition.DOUBLE_OUT -> suggestDoubleOut(remaining, dartsLeft)
            CloseCondition.SINGLE_OUT -> suggestSingleOut(remaining, dartsLeft)
        }
    }

    private fun suggestDoubleOut(remaining: Int, dartsLeft: Int): List<String>? {
        if (remaining > 170 || remaining == 1) return null

        // 1-dart finish
        if (dartsLeft >= 1) {
            FINISH_BY_VALUE_DOUBLE[remaining]?.let { return listOf(it.label) }
        }

        // 2-dart finish
        if (dartsLeft >= 2) {
            for (first in ALL_SCORES) {
                val r = remaining - first.value
                if (r <= 0) continue
                FINISH_BY_VALUE_DOUBLE[r]?.let { fin ->
                    return listOf(first.label, fin.label)
                }
            }
        }

        // 3-dart finish
        if (dartsLeft >= 3) {
            for (first in ALL_SCORES) {
                val r1 = remaining - first.value
                if (r1 <= 0 || r1 > 110) continue
                for (second in ALL_SCORES) {
                    val r2 = r1 - second.value
                    if (r2 <= 0) continue
                    FINISH_BY_VALUE_DOUBLE[r2]?.let { fin ->
                        return listOf(first.label, second.label, fin.label)
                    }
                }
            }
        }

        return null
    }

    private fun suggestSingleOut(remaining: Int, dartsLeft: Int): List<String>? {
        if (remaining > 180) return null

        // 1-dart finish
        if (dartsLeft >= 1) {
            FINISH_BY_VALUE_SINGLE[remaining]?.let { return listOf(it.label) }
        }

        // 2-dart finish
        if (dartsLeft >= 2) {
            for (first in ALL_SCORES_SINGLE_OUT) {
                val r = remaining - first.value
                if (r <= 0) continue
                FINISH_BY_VALUE_SINGLE[r]?.let { fin ->
                    return listOf(first.label, fin.label)
                }
            }
        }

        // 3-dart finish
        if (dartsLeft >= 3) {
            for (first in ALL_SCORES_SINGLE_OUT) {
                val r1 = remaining - first.value
                if (r1 <= 0) continue
                for (second in ALL_SCORES_SINGLE_OUT) {
                    val r2 = r1 - second.value
                    if (r2 <= 0) continue
                    FINISH_BY_VALUE_SINGLE[r2]?.let { fin ->
                        return listOf(first.label, second.label, fin.label)
                    }
                }
            }
        }

        return null
    }
}
