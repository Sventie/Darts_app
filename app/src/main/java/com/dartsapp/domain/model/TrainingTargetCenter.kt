package com.dartsapp.domain.model

import kotlin.math.cos
import kotlin.math.sin

// Board geometry constants (same fractions as DartBoardInput / DartBoardHeatmap)
private val BOARD_NUMBERS = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
private const val R_BULL       = 0.134f
private const val R_TRIPLE_IN  = 0.375f
private const val R_TRIPLE_OUT = 0.483f
private const val R_DOUBLE_IN  = 0.760f
private const val R_DOUBLE_OUT = 0.894f

private fun segmentAngleRad(number: Int): Double {
    val idx = BOARD_NUMBERS.indexOf(number)
    return Math.toRadians(-90.0 + idx * 18.0)
}

private fun polarToNorm(r: Float, number: Int): Pair<Float, Float> {
    val a = segmentAngleRad(number)
    return (r * cos(a)).toFloat() to (r * sin(a)).toFloat()
}

/**
 * Returns the normalised board coordinates (0,0 = centre, ±1 = canvas edge) of
 * the geometric centre of a Zielfeld target string such as "S20", "D5", "T17",
 * "Bull" or "Bullseye".  Returns (0, 0) for unrecognised / miss entries.
 */
fun targetCenterForZielfeldField(fieldStr: String): Pair<Float, Float> {
    return when {
        fieldStr == "Bullseye" || fieldStr == "Bull" -> 0f to 0f
        fieldStr.startsWith("S") -> {
            val n = fieldStr.substring(1).toIntOrNull() ?: return 0f to 0f
            polarToNorm((R_TRIPLE_OUT + R_DOUBLE_IN) / 2f, n)
        }
        fieldStr.startsWith("D") -> {
            val n = fieldStr.substring(1).toIntOrNull() ?: return 0f to 0f
            polarToNorm((R_DOUBLE_IN + R_DOUBLE_OUT) / 2f, n)
        }
        fieldStr.startsWith("T") -> {
            val n = fieldStr.substring(1).toIntOrNull() ?: return 0f to 0f
            polarToNorm((R_TRIPLE_IN + R_TRIPLE_OUT) / 2f, n)
        }
        else -> 0f to 0f
    }
}

/**
 * Returns the normalised board coordinates of the target centre for an
 * Around the Clock throw at [number] (1–20).
 * [requiresDouble] selects between the double ring and the single ring.
 */
fun targetCenterForAtcNumber(number: Int, requiresDouble: Boolean): Pair<Float, Float> {
    val r = if (requiresDouble) (R_DOUBLE_IN + R_DOUBLE_OUT) / 2f
            else                (R_TRIPLE_OUT + R_DOUBLE_IN) / 2f
    return polarToNorm(r, number)
}
