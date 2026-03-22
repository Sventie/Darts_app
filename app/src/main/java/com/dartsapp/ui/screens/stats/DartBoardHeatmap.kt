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
import com.dartsapp.domain.model.FieldHitFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ── Board geometry ────────────────────────────────────────────────────────────

private val BOARD_NUMBERS = listOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

private const val R_BULLSEYE       = 0.054f
private const val R_BULL           = 0.134f
private const val R_TRIPLE_IN      = 0.375f
private const val R_TRIPLE_OUT     = 0.483f
private const val R_DOUBLE_IN      = 0.760f
private const val R_DOUBLE_OUT     = 0.894f
private const val R_LABEL_RING_OUT = 1.000f
private const val R_LABEL          = 0.945f

private val ColLabelRing = Color(0xFF0D0D0D)
private val ColBoardBg   = Color(0xFF111111)
private val ColWire      = Color(0xFFAAAAAA)

// ── Heat computation ──────────────────────────────────────────────────────────

private const val GRID = 220          // pixels of the heat grid (square)
private const val SIGMA = GRID * 0.14f  // Gaussian spread (≈ 1 segment width)

/** A single heat point: normalized board coordinates and the hit count. */
private data class HeatPoint(val nx: Float, val ny: Float, val count: Int)

/**
 * Converts frequency data to heat points with normalized board positions.
 * Normalized coordinates: 0,0 = center; 1.0 = canvas half-width.
 */
private fun buildHeatPoints(frequencies: List<FieldHitFrequency>): List<HeatPoint> {
    val out = mutableListOf<HeatPoint>()
    for (freq in frequencies) {
        when (freq.field) {
            0  -> Unit // misses – no position on board
            50 -> if (freq.totalHits > 0)
                      out += HeatPoint(0f, 0f, freq.totalHits)
            25 -> if (freq.totalHits > 0) {
                      // Distribute bull hits evenly around the bull ring
                      val r = (R_BULLSEYE + R_BULL) / 2f
                      val n = 8
                      val each = maxOf(1, freq.totalHits / n)
                      repeat(n) { i ->
                          val a = Math.toRadians(i * 360.0 / n)
                          out += HeatPoint((r * cos(a)).toFloat(), (r * sin(a)).toFloat(), each)
                      }
                  }
            else -> {
                val idx  = BOARD_NUMBERS.indexOf(freq.field)
                val aRad = Math.toRadians(-90.0 + idx * 18.0)
                val cosA = cos(aRad).toFloat()
                val sinA = sin(aRad).toFloat()

                // Single: center of combined single area (inner + outer)
                if (freq.singleCount > 0) {
                    val r = (R_BULL + R_DOUBLE_IN) / 2f
                    out += HeatPoint(r * cosA, r * sinA, freq.singleCount)
                }
                // Triple
                if (freq.tripleCount > 0) {
                    val r = (R_TRIPLE_IN + R_TRIPLE_OUT) / 2f
                    out += HeatPoint(r * cosA, r * sinA, freq.tripleCount)
                }
                // Double
                if (freq.doubleCount > 0) {
                    val r = (R_DOUBLE_IN + R_DOUBLE_OUT) / 2f
                    out += HeatPoint(r * cosA, r * sinA, freq.doubleCount)
                }
            }
        }
    }
    return out
}

/**
 * Renders the heat data into an Android Bitmap using Gaussian kernels.
 * Returns null when there is no data to show.
 */
private fun computeHeatBitmap(points: List<HeatPoint>): AndroidBitmap? {
    if (points.isEmpty()) return null

    val half  = GRID / 2f
    val sigma2 = 2f * SIGMA * SIGMA
    val r3     = (3f * SIGMA).toInt()

    // Accumulate Gaussian contributions into heat grid
    val heat = FloatArray(GRID * GRID)
    for (pt in points) {
        val px = pt.nx * half + half
        val py = pt.ny * half + half
        val x0 = (px - r3).toInt().coerceAtLeast(0)
        val x1 = (px + r3).toInt().coerceAtMost(GRID - 1)
        val y0 = (py - r3).toInt().coerceAtLeast(0)
        val y1 = (py + r3).toInt().coerceAtMost(GRID - 1)
        for (y in y0..y1) {
            val dy = y - py
            val dy2 = dy * dy
            for (x in x0..x1) {
                val dx = x - px
                heat[y * GRID + x] += pt.count * exp(-(dx * dx + dy2) / sigma2).toFloat()
            }
        }
    }

    val maxHeat = heat.max().takeIf { it > 0f } ?: return null

    // Map heat → ARGB pixels; clip to board scoring area (inside R_DOUBLE_OUT)
    val pixels = IntArray(GRID * GRID)
    for (y in 0 until GRID) {
        val ny = (y - half) / half
        for (x in 0 until GRID) {
            val nx = (x - half) / half
            if (sqrt(nx * nx + ny * ny) > R_DOUBLE_OUT) {
                pixels[y * GRID + x] = 0 // transparent outside board
                continue
            }
            val t = (heat[y * GRID + x] / maxHeat).coerceIn(0f, 1f)
            pixels[y * GRID + x] = heatToArgb(t)
        }
    }

    return AndroidBitmap.createBitmap(GRID, GRID, AndroidBitmap.Config.ARGB_8888)
        .also { it.setPixels(pixels, 0, GRID, 0, 0, GRID, GRID) }
}

/**
 * Classic heatmap gradient: blue (0) → cyan → green → yellow → orange → red (1).
 * Alpha is constant at 210/255 ≈ 82%.
 */
private fun heatToArgb(t: Float): Int {
    val r: Int; val g: Int; val b: Int
    when {
        t < 0.25f -> { val f = t / 0.25f;         r = 0;                        g = (f * 255).roundToInt();       b = 255 }
        t < 0.50f -> { val f = (t - 0.25f) / 0.25f; r = 0;                     g = 255;                          b = ((1f - f) * 255).roundToInt() }
        t < 0.75f -> { val f = (t - 0.50f) / 0.25f; r = (f * 255).roundToInt(); g = 255;                         b = 0 }
        else       -> { val f = (t - 0.75f) / 0.25f; r = 255;                   g = ((1f - f) * 255).roundToInt(); b = 0 }
    }
    return (210 shl 24) or (r.coerceIn(0,255) shl 16) or (g.coerceIn(0,255) shl 8) or b.coerceIn(0,255)
}

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Dartboard heatmap: smooth Gaussian heat blobs (blue→red) per hit zone,
 * with board wires and numbers rendered on top for readability.
 */
@Composable
fun DartBoardHeatmap(
    frequencies: List<FieldHitFrequency>,
    modifier:    Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Compute the heat bitmap on a background thread
    val heatBitmap by produceState<AndroidBitmap?>(initialValue = null, key1 = frequencies) {
        value = withContext(Dispatchers.Default) {
            computeHeatBitmap(buildHeatPoints(frequencies))
        }
    }

    Canvas(modifier = modifier) {
        val R      = size.width / 2f
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val center = Offset(cx, cy)

        // ── 1. Black backgrounds ──────────────────────────────────────────────
        drawCircle(color = ColLabelRing, radius = R * R_LABEL_RING_OUT, center = center)
        drawCircle(color = ColBoardBg,   radius = R * R_DOUBLE_OUT,     center = center)

        // ── 2. Gaussian heat bitmap ───────────────────────────────────────────
        heatBitmap?.let { bmp ->
            drawImage(
                image     = bmp.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize   = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
        }

        // ── 3. Wire lines and ring circles on top ─────────────────────────────
        drawBoardWiresAndLabels(center, R, textMeasurer)
    }
}

/** Draws only the wire dividers, ring outlines, and number labels. */
private fun DrawScope.drawBoardWiresAndLabels(
    center:      Offset,
    R:           Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val cx = center.x
    val cy = center.y

    // Segment divider lines
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
