package com.dartsapp.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.dartsapp.domain.model.FieldHitFrequency
import com.dartsapp.ui.screens.game.components.DartBoardIllustration
import kotlin.math.cos
import kotlin.math.sin

// Geometry constants (mirror of DartBoardInput.kt)
private val BOARD_NUMBERS = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
private const val R_BULLSEYE   = 0.054f
private const val R_BULL       = 0.134f
private const val R_TRIPLE_IN  = 0.375f
private const val R_TRIPLE_OUT = 0.483f
private const val R_DOUBLE_IN  = 0.760f
private const val R_DOUBLE_OUT = 0.894f

/**
 * Draws a dartboard with a heat overlay:
 * each scoring zone is tinted yellow→red proportional to how often the player hit it.
 */
@Composable
fun DartBoardHeatmap(
    frequencies: List<FieldHitFrequency>,
    modifier: Modifier = Modifier
) {
    val freqMap   = frequencies.associateBy { it.field }
    val maxCount  = frequencies
        .flatMap { f ->
            if (f.field in 1..20) listOf(f.singleCount, f.doubleCount, f.tripleCount)
            else listOf(f.totalHits)
        }
        .maxOrNull()
        ?.takeIf { it > 0 }
        ?: 0

    Box(modifier = modifier) {
        DartBoardIllustration(Modifier.matchParentSize())

        if (maxCount > 0) {
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val R      = size.width / 2f

                // Bull overlay (annular ring between R_BULLSEYE and R_BULL)
                freqMap[25]?.let { f ->
                    val intensity = f.totalHits.toFloat() / maxCount
                    if (intensity > 0f)
                        drawAnnularCircle(center, R * R_BULL, R * R_BULLSEYE, heatColor(intensity))
                }

                // Bullseye overlay
                freqMap[50]?.let { f ->
                    val intensity = f.totalHits.toFloat() / maxCount
                    if (intensity > 0f)
                        drawCircle(color = heatColor(intensity), radius = R * R_BULLSEYE, center = center)
                }

                // 20 segments
                for (i in 0 until 20) {
                    val field      = BOARD_NUMBERS[i]
                    val startAngle = -90f + i * 18f - 9f
                    val sweep      = 18f
                    val freq       = freqMap[field]

                    val singleInt = (freq?.singleCount ?: 0).toFloat() / maxCount
                    val tripleInt = (freq?.tripleCount ?: 0).toFloat() / maxCount
                    val doubleInt = (freq?.doubleCount ?: 0).toFloat() / maxCount

                    // Inner single
                    if (singleInt > 0f)
                        drawAnnularSector(center, R * R_TRIPLE_IN,  R * R_BULL,       startAngle, sweep, heatColor(singleInt))
                    // Triple
                    if (tripleInt > 0f)
                        drawAnnularSector(center, R * R_TRIPLE_OUT, R * R_TRIPLE_IN,  startAngle, sweep, heatColor(tripleInt))
                    // Outer single
                    if (singleInt > 0f)
                        drawAnnularSector(center, R * R_DOUBLE_IN,  R * R_TRIPLE_OUT, startAngle, sweep, heatColor(singleInt))
                    // Double
                    if (doubleInt > 0f)
                        drawAnnularSector(center, R * R_DOUBLE_OUT, R * R_DOUBLE_IN,  startAngle, sweep, heatColor(doubleInt))
                }
            }
        }
    }
}

/** Yellow (low) → red (high), fully transparent at zero. */
private fun heatColor(intensity: Float): Color = Color(
    red   = 1f,
    green = (1f - intensity).coerceIn(0f, 1f),
    blue  = 0f,
    alpha = (0.15f + intensity * 0.65f).coerceIn(0f, 0.8f)
)

private fun DrawScope.drawAnnularCircle(center: Offset, outerR: Float, innerR: Float, color: Color) {
    val path = Path().apply { fillType = PathFillType.EvenOdd }
    path.addOval(Rect(Offset(center.x - outerR, center.y - outerR), Size(outerR * 2f, outerR * 2f)))
    path.addOval(Rect(Offset(center.x - innerR, center.y - innerR), Size(innerR * 2f, innerR * 2f)))
    drawPath(path, color)
}

private fun DrawScope.drawAnnularSector(
    center:     Offset,
    outerR:     Float,
    innerR:     Float,
    startAngle: Float,
    sweepAngle: Float,
    color:      Color
) {
    val startRad = Math.toRadians(startAngle.toDouble())
    val endRad   = Math.toRadians((startAngle + sweepAngle).toDouble())

    val path = Path()
    path.moveTo(
        center.x + (outerR * cos(startRad)).toFloat(),
        center.y + (outerR * sin(startRad)).toFloat()
    )
    path.arcTo(
        rect              = Rect(Offset(center.x - outerR, center.y - outerR), Size(outerR * 2f, outerR * 2f)),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweepAngle,
        forceMoveTo       = false
    )
    path.lineTo(
        center.x + (innerR * cos(endRad)).toFloat(),
        center.y + (innerR * sin(endRad)).toFloat()
    )
    path.arcTo(
        rect              = Rect(Offset(center.x - innerR, center.y - innerR), Size(innerR * 2f, innerR * 2f)),
        startAngleDegrees = startAngle + sweepAngle,
        sweepAngleDegrees = -sweepAngle,
        forceMoveTo       = false
    )
    path.close()
    drawPath(path, color)
}
