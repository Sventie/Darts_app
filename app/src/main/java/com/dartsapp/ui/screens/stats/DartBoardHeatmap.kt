package com.dartsapp.ui.screens.stats

import android.graphics.Bitmap as AndroidBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.dartsapp.domain.usecase.stats.GetHeatPositionsUseCase.HitPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

// ── Board geometry ─────────────────────────────────────────────────────────────

private val BOARD_NUMBERS      = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
private const val R_BULLSEYE       = 0.054f
private const val R_BULL           = 0.134f
private const val R_TRIPLE_IN      = 0.375f
private const val R_TRIPLE_OUT     = 0.483f
private const val R_DOUBLE_IN      = 0.760f
private const val R_DOUBLE_OUT     = 0.894f
private const val R_DOUBLE_OUT_SQ  = R_DOUBLE_OUT * R_DOUBLE_OUT
private const val R_LABEL_RING_OUT = 1.000f
private const val R_LABEL          = 0.945f

private val ColLabelRing = Color(0xFF0D0D0D)
private val ColBoardBg   = Color(0xFF111111)
private val ColWire      = Color(0xFFAAAAAA)

// ── Heat computation ───────────────────────────────────────────────────────────

private const val GRID = 220
// Sigma ≈ 3 % of canvas half-width → roughly 1/6 of a segment width.
// Keeps blobs tight and position-accurate; nearby throws blend only when very close.
private const val SIGMA = GRID * 0.02f

/**
 * Builds a Gaussian-kernel heat bitmap from raw tap positions.
 * Each [HitPosition] contains board-normalised coords (0,0=centre, 1.0=canvas edge).
 * Returns null when the list is empty (no data yet).
 */
private fun computeHeatBitmap(positions: List<HitPosition>): AndroidBitmap? {
    if (positions.isEmpty()) return null

    val half   = GRID / 2f
    val sigma2 = 2f * SIGMA * SIGMA
    val r3     = (3f * SIGMA).toInt()
    val heat   = FloatArray(GRID * GRID)

    for (pos in positions) {
        val px = pos.nx * half + half
        val py = pos.ny * half + half
        val x0 = (px - r3).toInt().coerceAtLeast(0)
        val x1 = (px + r3).toInt().coerceAtMost(GRID - 1)
        val y0 = (py - r3).toInt().coerceAtLeast(0)
        val y1 = (py + r3).toInt().coerceAtMost(GRID - 1)
        for (y in y0..y1) {
            val dy2 = (y - py).let { it * it }
            for (x in x0..x1) {
                val dx = x - px
                heat[y * GRID + x] += exp(-(dx * dx + dy2) / sigma2).toFloat()
            }
        }
    }

    val maxHeat = heat.max().takeIf { it > 0f } ?: return null

    val pixels = IntArray(GRID * GRID)
    for (y in 0 until GRID) {
        val ny = (y - half) / half
        for (x in 0 until GRID) {
            val nx = (x - half) / half
            // Clip to scoring area; label ring stays black
            if (nx * nx + ny * ny > R_DOUBLE_OUT_SQ) {
                pixels[y * GRID + x] = 0
                continue
            }
            val t = (heat[y * GRID + x] / maxHeat).coerceIn(0f, 1f)
            // Only paint pixels that actually carry heat – keeps the board visible
            // in untouched areas. Very faint zones (t < 0.01) are transparent.
            if (t < 0.01f) continue
            pixels[y * GRID + x] = heatToArgb(t)
        }
    }

    return AndroidBitmap.createBitmap(GRID, GRID, AndroidBitmap.Config.ARGB_8888)
        .also { it.setPixels(pixels, 0, GRID, 0, 0, GRID, GRID) }
}

/**
 * Classic heatmap gradient: blue → cyan → green → yellow → orange → red.
 * Alpha ≈ 80 % (204/255) so the dartboard is still partially visible.
 */
private fun heatToArgb(t: Float): Int {
    val alpha = 204  // ~80 %
    val r: Int; val g: Int; val b: Int
    when {
        t < 0.25f -> { val f = t / 0.25f;           r = 0;                         g = (f * 255).roundToInt();        b = 255 }
        t < 0.50f -> { val f = (t - 0.25f) / 0.25f; r = 0;                         g = 255;                           b = ((1f - f) * 255).roundToInt() }
        t < 0.75f -> { val f = (t - 0.50f) / 0.25f; r = (f * 255).roundToInt();    g = 255;                           b = 0 }
        else       -> { val f = (t - 0.75f) / 0.25f; r = 255;                       g = ((1f - f) * 255).roundToInt(); b = 0 }
    }
    return (alpha shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
}

// ── Composable ─────────────────────────────────────────────────────────────────

/**
 * Dartboard heatmap: smooth Gaussian blobs at the exact tap positions stored
 * during play. Areas with no hits remain transparent so the board shows through.
 * Board wires and numbers are drawn on top for readability.
 */
@Composable
fun DartBoardHeatmap(
    hitPositions: List<HitPosition>,
    modifier:     Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val heatBitmap by produceState<AndroidBitmap?>(initialValue = null, key1 = hitPositions) {
        value = withContext(Dispatchers.Default) { computeHeatBitmap(hitPositions) }
    }

    Canvas(modifier = modifier) {
        val R      = size.width / 2f
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val center = Offset(cx, cy)

        // 1. Black backgrounds
        drawCircle(color = ColLabelRing, radius = R * R_LABEL_RING_OUT, center = center)
        drawCircle(color = ColBoardBg,   radius = R * R_DOUBLE_OUT,     center = center)

        // 2. Gaussian heat bitmap (transparent where no hits)
        heatBitmap?.let { bmp ->
            drawImage(
                image     = bmp.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize   = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
        }

        // 3. Wire dividers, ring outlines, number labels
        drawBoardWiresAndLabels(center, R, textMeasurer)
    }
}

/**
 * Dartboard with a dispersion circle. The circle is centered at the bullseye
 * and has a radius proportional to [dispersion] (0=tiny, 1=double ring edge).
 */
@Composable
fun DartBoardDispersion(
    dispersion: Float,
    modifier:   Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val R      = size.width / 2f
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val center = Offset(cx, cy)

        drawCircle(color = ColLabelRing, radius = R * R_LABEL_RING_OUT, center = center)
        drawCircle(color = ColBoardBg,   radius = R * R_DOUBLE_OUT,     center = center)

        val circleRadius = dispersion * R_DOUBLE_OUT * R
        if (circleRadius > 0f) {
            drawCircle(color = Color(0x440088FF), radius = circleRadius, center = center)
            drawCircle(color = Color(0xCC0088FF), radius = circleRadius, center = center, style = Stroke(4f))
        }

        drawBoardWiresAndLabels(center, R, textMeasurer)
    }
}

private fun DrawScope.drawBoardWiresAndLabels(
    center:       Offset,
    R:            Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val cx = center.x
    val cy = center.y

    // Segment dividers
    for (i in 0 until 20) {
        val a    = Math.toRadians(-90.0 + i * 18.0 - 9.0)
        val cosA = cos(a).toFloat()
        val sinA = sin(a).toFloat()
        drawLine(
            color       = ColWire,
            start       = Offset(cx + R * R_BULL * cosA,           cy + R * R_BULL * sinA),
            end         = Offset(cx + R * R_LABEL_RING_OUT * cosA, cy + R * R_LABEL_RING_OUT * sinA),
            strokeWidth = 2f
        )
    }

    // Ring outlines
    listOf(R_BULL, R_TRIPLE_IN, R_TRIPLE_OUT, R_DOUBLE_IN, R_DOUBLE_OUT).forEach { r ->
        drawCircle(color = ColWire, radius = R * r, center = center, style = Stroke(2f))
    }
    drawCircle(color = ColWire, radius = R * R_BULLSEYE,       center = center, style = Stroke(1.5f))
    drawCircle(color = ColWire, radius = R * R_LABEL_RING_OUT, center = center, style = Stroke(2f))

    // Number labels
    val style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    for (i in 0 until 20) {
        val n        = BOARD_NUMBERS[i]
        val a        = Math.toRadians(-90.0 + i * 18.0)
        val labelPos = Offset(
            cx + (R * R_LABEL * cos(a)).toFloat(),
            cy + (R * R_LABEL * sin(a)).toFloat()
        )
        val measured = textMeasurer.measure("$n", style)
        drawText(
            textMeasurer = textMeasurer,
            text         = "$n",
            style        = style,
            topLeft      = Offset(
                labelPos.x - measured.size.width  / 2f,
                labelPos.y - measured.size.height / 2f
            )
        )
    }
}
