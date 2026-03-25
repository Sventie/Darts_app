package com.dartsapp.ui.screens.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
// Scaled so R_LABEL_RING_OUT = 1.0: the board fills the full composable bounds with no
// empty border. Scale factor = 1 / 0.895 ≈ 1.117 applied to original values.
private const val R_BULLSEYE       = 0.054f  // inner bull  (50 pts)
private const val R_BULL           = 0.134f  // outer bull  (25 pts)
private const val R_TRIPLE_IN      = 0.375f  // triple ring inner edge
private const val R_TRIPLE_OUT     = 0.483f  // triple ring outer edge
private const val R_DOUBLE_IN      = 0.760f  // double ring inner edge
private const val R_DOUBLE_OUT     = 0.894f  // double ring outer edge
private const val R_LABEL_RING_OUT = 1.000f  // outer edge of the dark number ring (fills composable)
private const val R_LABEL          = 0.945f  // center of label ring

private val ColBoardBg   = Color(0xFF111111)
private val ColLabelRing = Color(0xFF0D0D0D)
private val ColBlack    = Color(0xFF1A1A1A)
private val ColCream    = Color(0xFFD4B483)
private val ColRed      = Color(0xFFB22222)
private val ColGreen    = Color(0xFF1B6B2A)
private val ColBull     = Color(0xFF2E7D32)
private val ColBullseye = Color(0xFFB71C1C)
private val ColWire     = Color(0xFFAAAAAA)

@Composable
fun DartBoardIllustration(modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val center = Offset(cx, cy)
        val R      = size.width  / 2f
        drawBoard(center, R, textMeasurer)
    }
}

@Composable
fun DartBoardInput(
    onDartEntered: (DartInput) -> Unit,
    currentRoundDarts: List<DartInput>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Local tap positions – updated immediately in the tap handler so dart 3 is never
    // lost when the ViewModel auto-commits the round in the same frame.
    var livePositions  by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var ghostPositions by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    val ghostAlpha     = remember { Animatable(0f) }

    // Set to true in the tap handler so the LaunchedEffect can distinguish a round
    // commit (triggered by a tap) from an undo (triggered externally).
    var justTapped by remember { mutableStateOf(false) }

    // Current finger position while pressed – drives the magnifier overlay.
    // Read here (composition scope) so Canvas redraws on every position update.
    var magnifierPosition by remember { mutableStateOf<Offset?>(null) }
    val currentMagnifierPos = magnifierPosition

    LaunchedEffect(currentRoundDarts) {
        val wasTap = justTapped
        justTapped = false

        when {
            currentRoundDarts.isEmpty() && livePositions.isNotEmpty() && wasTap -> {
                // Round committed via tap: hold markers for 1 s, then fade out
                ghostPositions = livePositions
                livePositions  = emptyList()
                ghostAlpha.snapTo(1f)
                delay(1000)
                ghostAlpha.animateTo(0f, animationSpec = tween(durationMillis = 800))
                ghostPositions = emptyList()
            }
            currentRoundDarts.isNotEmpty() -> {
                // Dart added, or undo/player-switch with darts remaining: sync immediately
                livePositions = currentRoundDarts.mapNotNull { d ->
                    val tx = d.tapX; val ty = d.tapY
                    if (tx != null && ty != null) tx to ty else null
                }
                ghostAlpha.snapTo(0f)
                ghostPositions = emptyList()
            }
            else -> {
                // Undo of last dart, player switch, or round cleared externally
                livePositions  = emptyList()
                ghostAlpha.snapTo(0f)
                ghostPositions = emptyList()
            }
        }
    }

    // Read in composition scope so recomposition (and therefore redraw) is triggered
    // during the ghost fade animation.
    val ghostA = ghostAlpha.value

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val downEvent   = awaitPointerEvent()
                    val firstChange = downEvent.changes.firstOrNull() ?: return@awaitEachGesture
                    if (!firstChange.pressed) return@awaitEachGesture
                    firstChange.consume()
                    val downId   = firstChange.id
                    var position = firstChange.position

                    // Show magnifier only after 500 ms of holding; coroutineScope gives
                    // us a CoroutineScope for launch while keeping AwaitPointerEventScope
                    // in scope for awaitPointerEvent calls below.
                    coroutineScope {
                        val magnifierJob = launch {
                            delay(500)
                            magnifierPosition = position
                        }

                        // Track finger until lifted
                        while (true) {
                            val event  = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == downId } ?: break
                            if (!change.pressed) break
                            position = change.position
                            if (magnifierJob.isCompleted) magnifierPosition = position
                            change.consume()
                        }
                        magnifierJob.cancel()
                        magnifierPosition = null
                    }

                    // Dart calculation – same logic as before, using final release position
                    val cx    = size.width  / 2f
                    val cy    = size.height / 2f
                    val R     = size.width  / 2f
                    val dx    = position.x - cx
                    val dy    = position.y - cy
                    val rNorm = sqrt(dx * dx + dy * dy) / R
                    val nx    = dx / R
                    val ny    = dy / R
                    val input: DartInput = when {
                        rNorm < R_BULLSEYE   -> DartInput(50, ScoreMultiplier.SINGLE, 50, nx, ny)
                        rNorm < R_BULL       -> DartInput(25, ScoreMultiplier.SINGLE, 25, nx, ny)
                        rNorm > R_DOUBLE_OUT -> DartInput(0,  ScoreMultiplier.SINGLE,  0, nx, ny)
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
                            DartInput(field, mult, field * mult.value, nx, ny)
                        }
                    }
                    // Capture position locally BEFORE onDartEntered so that dart 3 is
                    // never lost when the ViewModel auto-commits the round in the same frame.
                    justTapped    = true
                    livePositions = livePositions + (nx to ny)
                    onDartEntered(input)
                }
            }
    ) {
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val center = Offset(cx, cy)
        val R      = size.width  / 2f

        drawBoard(center, R, textMeasurer)

        val markerRadius = R * 0.02f

        // Live markers – from local state so dart 3 is visible even when the ViewModel
        // auto-commits the round before the next frame (livePositions is synced back
        // from currentRoundDarts on undo / player switch via the LaunchedEffect above).
        livePositions.forEach { (nx, ny) ->
            val pos = Offset(nx * R + cx, ny * R + cy)
            drawCircle(color = Color.Red,   radius = markerRadius, center = pos)
            drawCircle(color = Color.White, radius = markerRadius, center = pos, style = Stroke(2f))
        }

        // Ghost markers – previous round fading out
        if (ghostA > 0f) {
            ghostPositions.forEach { (nx, ny) ->
                val pos = Offset(nx * R + cx, ny * R + cy)
                drawCircle(color = Color.Red.copy(alpha = ghostA),   radius = markerRadius, center = pos)
                drawCircle(color = Color.White.copy(alpha = ghostA), radius = markerRadius, center = pos, style = Stroke(2f))
            }
        }

        // Magnifier – shown while the finger is held down
        currentMagnifierPos?.let { touchPos ->
            val magnRadius = R * 0.28f
            val zoomFactor = 3f

            // Position above the finger; clamp so the circle stays within the canvas
            val magnCx = touchPos.x.coerceIn(magnRadius, size.width  - magnRadius)
            val magnCy = (touchPos.y - magnRadius - R * 0.08f)
                .coerceIn(magnRadius, size.height - magnRadius)
            val magnCenter = Offset(magnCx, magnCy)

            val circlePath = Path().apply { addOval(Rect(magnCenter, magnRadius)) }

            // Clipped zoomed view
            clipPath(circlePath) {
                // Board background fill (covers area outside the actual board)
                drawCircle(ColBoardBg, magnRadius, magnCenter)
                // Scale around touchPos so it stays fixed, then shift it to magnCenter.
                // Result: every point P maps to magnCenter + (P - touchPos) * zoom
                translate(magnCenter.x - touchPos.x, magnCenter.y - touchPos.y) {
                    scale(zoomFactor, zoomFactor, pivot = touchPos) {
                        drawBoard(center, R, textMeasurer)
                        // Crosshair at exact touch position
                        drawCircle(Color.Red,   markerRadius, touchPos)
                        drawCircle(Color.White, markerRadius, touchPos,
                                   style = Stroke(2f / zoomFactor))
                    }
                }
            }

            // Outer ring: thick white + thin dark outline (matches iOS magnifier look)
            drawCircle(Color.White,           magnRadius + 1f, magnCenter, style = Stroke(7f))
            drawCircle(Color(0xFF555555), magnRadius + 4f, magnCenter, style = Stroke(1.5f))
        }
    }
}

private fun DrawScope.drawBoard(center: Offset, R: Float, textMeasurer: androidx.compose.ui.text.TextMeasurer) {
    val cx = center.x
    val cy = center.y

    // Dark number ring (outermost layer, behind everything else)
    drawCircle(color = ColLabelRing, radius = R * R_LABEL_RING_OUT, center = center)
    // Thin wire border around the label ring
    drawCircle(color = ColWire, radius = R * R_LABEL_RING_OUT, center = center, style = Stroke(2f))

    // Dark board background circle (sits inside the label ring)
    drawCircle(color = ColBoardBg, radius = R * R_DOUBLE_OUT, center = center)

    // 20 segments, 4 rings each
    for (i in 0 until 20) {
        val startAngle = -90f + i * 18f - 9f
        val sweep      = 18f
        val isEven     = (i % 2 == 0)
        val colSingle  = if (isEven) ColBlack else ColCream
        val colScore   = if (isEven) ColRed   else ColGreen

        drawAnnularSector(center, R * R_TRIPLE_IN,  R * R_BULL,       startAngle, sweep, colSingle)
        drawAnnularSector(center, R * R_TRIPLE_OUT, R * R_TRIPLE_IN,  startAngle, sweep, colScore)
        drawAnnularSector(center, R * R_DOUBLE_IN,  R * R_TRIPLE_OUT, startAngle, sweep, colSingle)
        drawAnnularSector(center, R * R_DOUBLE_OUT, R * R_DOUBLE_IN,  startAngle, sweep, colScore)
    }

    // Bull rings (drawn on top of segments)
    drawCircle(color = ColBull,     radius = R * R_BULL,     center = center)
    drawCircle(color = ColBullseye, radius = R * R_BULLSEYE, center = center)

    // Wire: segment divider lines (extend into label ring)
    for (i in 0 until 20) {
        val angleRad = Math.toRadians(-90.0 + i * 18.0 - 9.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        drawLine(
            color       = ColWire,
            start       = Offset(cx + R * R_BULL * cosA,           cy + R * R_BULL * sinA),
            end         = Offset(cx + R * R_LABEL_RING_OUT * cosA, cy + R * R_LABEL_RING_OUT * sinA),
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
        fontSize   = 11.sp,
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
