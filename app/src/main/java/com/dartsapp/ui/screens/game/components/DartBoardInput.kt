package com.dartsapp.ui.screens.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.dartsapp.data.model.ScoreMultiplier
import com.dartsapp.domain.model.DartInput
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Standard dartboard number order, clockwise starting from top (20)
private val BOARD_NUMBERS = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

// All radii as fractions of the canvas half-width.
// Board occupies ~83% of canvas so number labels fit around it.
private const val R_BULLSEYE   = 0.028f  // inner bull  (50 pts)
private const val R_BULL       = 0.072f  // outer bull  (25 pts)
private const val R_TRIPLE_IN  = 0.432f  // triple ring inner edge
private const val R_TRIPLE_OUT = 0.473f  // triple ring outer edge
private const val R_DOUBLE_IN  = 0.711f  // double ring inner edge
private const val R_DOUBLE_OUT = 0.752f  // double ring outer edge  (= board playable radius)
private const val R_LABEL      = 0.860f  // number label radius

private val ColBoardBg  = Color(0xFF111111)
private val ColBlack    = Color(0xFF1A1A1A)
private val ColCream    = Color(0xFFD4B483)
private val ColRed      = Color(0xFFB22222)
private val ColGreen    = Color(0xFF1B6B2A)
private val ColBull     = Color(0xFF2E7D32)
private val ColBullseye = Color(0xFFB71C1C)
private val ColWire     = Color(0xFFAAAAAA)

@Composable
fun DartBoardInput(
    onDartEntered: (DartInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val R  = size.width / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val rNorm = sqrt(dx * dx + dy * dy) / R

                    val input: DartInput = when {
                        rNorm < R_BULLSEYE -> DartInput(50, ScoreMultiplier.SINGLE, 50)
                        rNorm < R_BULL     -> DartInput(25, ScoreMultiplier.SINGLE, 25)
                        rNorm > R_DOUBLE_OUT + 0.01f -> DartInput(0, ScoreMultiplier.SINGLE, 0)
                        else -> {
                            val angleDeg   = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val boardAngle = (angleDeg + 90f + 360f) % 360f
                            val segIndex   = ((boardAngle + 9f) / 18f).toInt() % 20
                            val field      = BOARD_NUMBERS[segIndex]
                            val mult = when {
                                rNorm >= R_TRIPLE_IN && rNorm <= R_TRIPLE_OUT -> ScoreMultiplier.TRIPLE
                                rNorm >= R_DOUBLE_IN                          -> ScoreMultiplier.DOUBLE
                                else                                          -> ScoreMultiplier.SINGLE
                            }
                            DartInput(field, mult, field * mult.value)
                        }
                    }
                    onDartEntered(input)
                }
            }
    ) {
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val R = size.width / 2f

        // Dark board background circle
        drawCircle(color = ColBoardBg, radius = R * R_DOUBLE_OUT + 6f, center = center)

        // 20 segments, 4 rings each
        for (i in 0 until 20) {
            val startAngle = -90f + i * 18f - 9f
            val sweep      = 18f
            val isEven     = (i % 2 == 0)
            val colSingle  = if (isEven) ColCream else ColBlack
            val colScore   = if (isEven) ColRed   else ColGreen

            drawAnnularSector(center, R * R_TRIPLE_IN,  R * R_BULL,       startAngle, sweep, colSingle)
            drawAnnularSector(center, R * R_TRIPLE_OUT, R * R_TRIPLE_IN,  startAngle, sweep, colScore)
            drawAnnularSector(center, R * R_DOUBLE_IN,  R * R_TRIPLE_OUT, startAngle, sweep, colSingle)
            drawAnnularSector(center, R * R_DOUBLE_OUT, R * R_DOUBLE_IN,  startAngle, sweep, colScore)
        }

        // Bull rings (drawn on top of segments)
        drawCircle(color = ColBull,     radius = R * R_BULL,     center = center)
        drawCircle(color = ColBullseye, radius = R * R_BULLSEYE, center = center)

        // Wire: segment divider lines
        for (i in 0 until 20) {
            val angleRad = Math.toRadians(-90.0 + i * 18.0 - 9.0)
            val cosA = cos(angleRad).toFloat()
            val sinA = sin(angleRad).toFloat()
            drawLine(
                color       = ColWire,
                start       = Offset(cx + R * R_BULL * cosA,       cy + R * R_BULL * sinA),
                end         = Offset(cx + R * R_DOUBLE_OUT * cosA, cy + R * R_DOUBLE_OUT * sinA),
                strokeWidth = 2f
            )
        }

        // Wire: ring outlines
        listOf(R_BULL, R_TRIPLE_IN, R_TRIPLE_OUT, R_DOUBLE_IN, R_DOUBLE_OUT).forEach { r ->
            drawCircle(color = ColWire, radius = R * r, center = center, style = Stroke(2f))
        }
        drawCircle(color = ColWire, radius = R * R_BULLSEYE, center = center, style = Stroke(1.5f))

        // Number labels
        val labelStyle = TextStyle(
            color      = Color.White,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold
        )
        for (i in 0 until 20) {
            val number   = BOARD_NUMBERS[i]
            val angleRad = Math.toRadians(-90.0 + i * 18.0)
            val labelPos = Offset(
                cx + (R * R_LABEL * cos(angleRad)).toFloat(),
                cy + (R * R_LABEL * sin(angleRad)).toFloat()
            )
            val measured = textMeasurer.measure("$number", labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text         = "$number",
                style        = labelStyle,
                topLeft      = Offset(
                    labelPos.x - measured.size.width  / 2f,
                    labelPos.y - measured.size.height / 2f
                )
            )
        }
    }
}

private fun DrawScope.drawAnnularSector(
    center: Offset,
    outerR: Float,
    innerR: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color
) {
    val startRad = Math.toRadians(startAngle.toDouble())
    val endRad   = Math.toRadians((startAngle + sweepAngle).toDouble())

    val path = Path()
    // Start at beginning of outer arc
    path.moveTo(
        center.x + (outerR * cos(startRad)).toFloat(),
        center.y + (outerR * sin(startRad)).toFloat()
    )
    // Outer arc (clockwise)
    path.arcTo(
        rect              = Rect(Offset(center.x - outerR, center.y - outerR), Size(outerR * 2f, outerR * 2f)),
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweepAngle,
        forceMoveTo       = false
    )
    // Line to end of inner arc
    path.lineTo(
        center.x + (innerR * cos(endRad)).toFloat(),
        center.y + (innerR * sin(endRad)).toFloat()
    )
    // Inner arc (counter-clockwise = negative sweep)
    path.arcTo(
        rect              = Rect(Offset(center.x - innerR, center.y - innerR), Size(innerR * 2f, innerR * 2f)),
        startAngleDegrees = startAngle + sweepAngle,
        sweepAngleDegrees = -sweepAngle,
        forceMoveTo       = false
    )
    path.close()
    drawPath(path, color)
}
