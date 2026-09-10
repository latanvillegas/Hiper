package com.photoengine.core.drawing

import android.graphics.*
import java.util.*
import kotlin.math.*

enum class GradientType {
    LINEAR,
    RADIAL,
    ANGLE,
    REFLECTED,
    DIAMOND
}

enum class TonalRange {
    SHADOWS,
    MIDTONES,
    HIGHLIGHTS
}

data class BrushDynamics(
    var size: Float = 24.0f,       // 1 .. 5000 px
    var hardness: Float = 0.8f,    // 0.0 .. 1.0 (Feather edge to sharp edge)
    var opacity: Float = 1.0f,     // 0.0 .. 1.0
    var flow: Float = 1.0f,        // 0.0 .. 1.0
    var spacing: Float = 0.25f,    // Fraction of brush radius (0.1 .. 2.0)
    var smoothing: Float = 0.5f    // Exponential stroke stabilizer (0.0 .. 1.0)
)

/**
 * Production-grade Photoshop Drawing Engine.
 * Supports:
 * - Anti-aliased & soft-feathered Brush with Flow & Spacing dynamics
 * - Crisp 1-bit Pencil tool
 * - Alpha Eraser with brush dynamics
 * - Flood Paint Bucket with tolerance, contiguous & multi-layer sampling
 * - Photorealistic 5-mode Gradients (Linear, Radial, Angle, Reflected, Diamond)
 * - Dodge & Burn across Shadows, Midtones, Highlights
 * - Sponge tool (Desaturate / Saturate)
 * - Smudge tool with Finger Painting color blending
 * - Sharpen & Blur brush with local convolution kernels
 */
class PhotoshopDrawingEngine(val canvasWidth: Int, val canvasHeight: Int) {

    private var lastStrokePoint: PointF? = null
    private var smoothedPoint: PointF? = null

    // Stamp brush tip alpha cache
    private var cachedTipBitmap: Bitmap? = null
    private var cachedTipParams: Pair<Float, Float>? = null

    private fun getBrushTip(size: Float, hardness: Float): Bitmap {
        val key = size to hardness
        if (cachedTipBitmap != null && cachedTipParams == key) {
            return cachedTipBitmap!!
        }

        val dim = (size * 2).toInt().coerceAtLeast(4)
        val tip = Bitmap.createBitmap(dim, dim, Bitmap.Config.ALPHA_8)
        val center = dim / 2.0f
        val radius = size
        val hardRadius = radius * hardness

        val bytes = ByteArray(dim * dim)
        for (y in 0 until dim) {
            for (x in 0 until dim) {
                val dx = x - center
                val dy = y - center
                val dist = sqrt(dx * dx + dy * dy)
                val alpha = when {
                    dist <= hardRadius -> 1.0f
                    dist >= radius -> 0.0f
                    else -> 1.0f - ((dist - hardRadius) / (radius - hardRadius).coerceAtLeast(0.001f))
                }
                bytes[y * dim + x] = (alpha.coerceIn(0f, 1f) * 255).toInt().toByte()
            }
        }

        tip.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        cachedTipBitmap?.recycle()
        cachedTipBitmap = tip
        cachedTipParams = key
        return tip
    }

    // --- 1. Brush & Pencil ---

    fun paintStroke(
        target: Bitmap,
        points: List<PointF>,
        color: Int,
        dynamics: BrushDynamics,
        isPencil: Boolean = false
    ) {
        if (points.isEmpty()) return
        val canvas = Canvas(target)
        val paint = Paint(if (isPencil) 0 else Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.color = color
            this.alpha = (dynamics.opacity * dynamics.flow * 255).toInt().coerceIn(0, 255)
        }

        val tip = if (isPencil) getBrushTip(dynamics.size, 1.0f) else getBrushTip(dynamics.size, dynamics.hardness)
        val tipHalf = tip.width / 2f
        val minStep = (dynamics.size * dynamics.spacing).coerceAtLeast(1.0f)

        var prev = lastStrokePoint ?: points.first()

        for (pt in points) {
            // Apply smoothing filter
            val smoothPt = if (dynamics.smoothing > 0f) {
                val sm = dynamics.smoothing.coerceIn(0f, 0.95f)
                PointF(prev.x * sm + pt.x * (1f - sm), prev.y * sm + pt.y * (1f - sm))
            } else pt

            val dx = smoothPt.x - prev.x
            val dy = smoothPt.y - prev.y
            val dist = sqrt(dx * dx + dy * dy)
            val steps = (dist / minStep).toInt().coerceAtLeast(1)

            for (s in 1..steps) {
                val t = s.toFloat() / steps
                val ix = prev.x + dx * t
                val iy = prev.y + dy * t
                canvas.drawBitmap(tip, ix - tipHalf, iy - tipHalf, paint)
            }

            prev = smoothPt
        }

        lastStrokePoint = prev
    }

    fun endStroke() {
        lastStrokePoint = null
        smoothedPoint = null
    }

    // --- 2. Eraser ---

    fun eraseStroke(
        target: Bitmap,
        points: List<PointF>,
        dynamics: BrushDynamics
    ) {
        val canvas = Canvas(target)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            this.alpha = (dynamics.opacity * dynamics.flow * 255).toInt().coerceIn(0, 255)
        }

        val tip = getBrushTip(dynamics.size, dynamics.hardness)
        val tipHalf = tip.width / 2f
        val minStep = (dynamics.size * dynamics.spacing).coerceAtLeast(1.0f)

        var prev = lastStrokePoint ?: points.first()
        for (pt in points) {
            val dx = pt.x - prev.x
            val dy = pt.y - prev.y
            val dist = sqrt(dx * dx + dy * dy)
            val steps = (dist / minStep).toInt().coerceAtLeast(1)

            for (s in 1..steps) {
                val t = s.toFloat() / steps
                val ix = prev.x + dx * t
                val iy = prev.y + dy * t
                canvas.drawBitmap(tip, ix - tipHalf, iy - tipHalf, paint)
            }
            prev = pt
        }
        lastStrokePoint = prev
    }

    // --- 3. Paint Bucket (Flood Fill) ---

    fun floodFill(
        target: Bitmap,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Float = 32.0f,
        contiguous: Boolean = true
    ) {
        val w = target.width
        val h = target.height
        val sx = startX.coerceIn(0, w - 1)
        val sy = startY.coerceIn(0, h - 1)
        val srcCol = target.getPixel(sx, sy)

        if (srcCol == fillColor) return

        val sr = (srcCol ushr 16) and 0xFF
        val sg = (srcCol ushr 8) and 0xFF
        val sb = srcCol and 0xFF

        val pixels = IntArray(w * h)
        target.getPixels(pixels, 0, w, 0, 0, w, h)

        if (contiguous) {
            val visited = BooleanArray(w * h)
            val q: Queue<Int> = ArrayDeque()
            val startIdx = sy * w + sx
            q.add(startIdx)
            visited[startIdx] = true

            while (q.isNotEmpty()) {
                val idx = q.poll() ?: break
                val x = idx % w
                val y = idx / w

                val c = pixels[idx]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val dist = sqrt(((r - sr) * (r - sr) + (g - sg) * (g - sg) + (b - sb) * (b - sb)).toFloat())

                if (dist <= tolerance) {
                    pixels[idx] = fillColor

                    if (x > 0 && !visited[idx - 1]) { visited[idx - 1] = true; q.add(idx - 1) }
                    if (x < w - 1 && !visited[idx + 1]) { visited[idx + 1] = true; q.add(idx + 1) }
                    if (y > 0 && !visited[idx - w]) { visited[idx - w] = true; q.add(idx - w) }
                    if (y < h - 1 && !visited[idx + w]) { visited[idx + w] = true; q.add(idx + w) }
                }
            }
        } else {
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val dist = sqrt(((r - sr) * (r - sr) + (g - sg) * (g - sg) + (b - sb) * (b - sb)).toFloat())
                if (dist <= tolerance) {
                    pixels[i] = fillColor
                }
            }
        }

        target.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    // --- 4. Gradients (Linear, Radial, Angle, Reflected, Diamond) ---

    fun renderGradient(
        target: Bitmap,
        type: GradientType,
        start: PointF,
        end: PointF,
        colors: IntArray,
        positions: FloatArray? = null
    ) {
        val w = target.width
        val h = target.height
        val pixels = IntArray(w * h)
        target.getPixels(pixels, 0, w, 0, 0, w, h)

        val dx = end.x - start.x
        val dy = end.y - start.y
        val lenSq = dx * dx + dy * dy
        val len = sqrt(lenSq).coerceAtLeast(0.001f)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val px = x - start.x
                val py = y - start.y

                val t: Float = when (type) {
                    GradientType.LINEAR -> {
                        // Project onto vector
                        ((px * dx + py * dy) / lenSq).coerceIn(0f, 1f)
                    }
                    GradientType.RADIAL -> {
                        val dist = sqrt(px * px + py * py)
                        (dist / len).coerceIn(0f, 1f)
                    }
                    GradientType.ANGLE -> {
                        val angle = (atan2(py, px) - atan2(dy, dx) + 2f * Math.PI.toFloat()) % (2f * Math.PI.toFloat())
                        (angle / (2f * Math.PI.toFloat())).coerceIn(0f, 1f)
                    }
                    GradientType.REFLECTED -> {
                        val proj = ((px * dx + py * dy) / lenSq)
                        (abs(proj) / 1.0f).coerceIn(0f, 1f)
                    }
                    GradientType.DIAMOND -> {
                        // Diamond metric: (|u| + |v|)
                        val cosA = dx / len
                        val sinA = dy / len
                        val u = abs(px * cosA + py * sinA)
                        val v = abs(-px * sinA + py * cosA)
                        ((u + v) / len).coerceIn(0f, 1f)
                    }
                }

                val gradColor = evaluateColorRamp(colors, positions, t)
                pixels[idx] = gradColor
            }
        }

        target.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun evaluateColorRamp(colors: IntArray, positions: FloatArray?, t: Float): Int {
        if (colors.isEmpty()) return Color.BLACK
        if (colors.size == 1) return colors[0]

        val pos = positions ?: FloatArray(colors.size) { it / (colors.size - 1).toFloat() }
        if (t <= pos.first()) return colors.first()
        if (t >= pos.last()) return colors.last()

        for (i in 0 until pos.size - 1) {
            if (t in pos[i]..pos[i + 1]) {
                val f = (t - pos[i]) / (pos[i + 1] - pos[i]).coerceAtLeast(0.0001f)
                val c0 = colors[i]
                val c1 = colors[i + 1]
                val a = ((1f - f) * ((c0 ushr 24) and 0xFF) + f * ((c1 ushr 24) and 0xFF)).toInt()
                val r = ((1f - f) * ((c0 ushr 16) and 0xFF) + f * ((c1 ushr 16) and 0xFF)).toInt()
                val g = ((1f - f) * ((c0 ushr 8) and 0xFF) + f * ((c1 ushr 8) and 0xFF)).toInt()
                val b = ((1f - f) * (c0 and 0xFF) + f * (c1 and 0xFF)).toInt()
                return (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return colors.last()
    }

    // --- 5. Dodge & Burn ---

    fun applyDodgeBurn(
        target: Bitmap,
        center: PointF,
        radius: Float,
        exposure: Float, // -1.0 (Burn) .. +1.0 (Dodge)
        range: TonalRange
    ) {
        val w = target.width
        val h = target.height
        val minX = (center.x - radius).toInt().coerceIn(0, w - 1)
        val maxX = (center.x + radius).toInt().coerceIn(0, w - 1)
        val minY = (center.y - radius).toInt().coerceIn(0, h - 1)
        val maxY = (center.y + radius).toInt().coerceIn(0, h - 1)

        val rSq = radius * radius

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - center.x
                val dy = y - center.y
                val distSq = dx * dx + dy * dy
                if (distSq > rSq) continue

                val falloff = 1.0f - sqrt(distSq) / radius
                val col = target.getPixel(x, y)
                val a = col ushr 24 and 0xFF
                var r = ((col ushr 16) and 0xFF) / 255.0f
                var g = ((col ushr 8) and 0xFF) / 255.0f
                var b = (col and 0xFF) / 255.0f

                val luma = 0.299f * r + 0.587f * g + 0.114f * b
                val rangeWeight = when (range) {
                    TonalRange.SHADOWS -> (1.0f - luma / 0.5f).coerceIn(0f, 1f)
                    TonalRange.MIDTONES -> (1.0f - abs(luma - 0.5f) * 2f).coerceIn(0f, 1f)
                    TonalRange.HIGHLIGHTS -> ((luma - 0.5f) / 0.5f).coerceIn(0f, 1f)
                }

                val delta = exposure * falloff * rangeWeight * 0.4f
                r = (r + delta).coerceIn(0f, 1f)
                g = (g + delta).coerceIn(0f, 1f)
                b = (b + delta).coerceIn(0f, 1f)

                val outCol = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
                target.setPixel(x, y, outCol)
            }
        }
    }

    // --- 6. Sponge Tool (Saturate / Desaturate) ---

    fun applySponge(
        target: Bitmap,
        center: PointF,
        radius: Float,
        flow: Float, // +0.5 (Saturate) or -0.5 (Desaturate)
        isSaturate: Boolean
    ) {
        val w = target.width
        val h = target.height
        val minX = (center.x - radius).toInt().coerceIn(0, w - 1)
        val maxX = (center.x + radius).toInt().coerceIn(0, w - 1)
        val minY = (center.y - radius).toInt().coerceIn(0, h - 1)
        val maxY = (center.y + radius).toInt().coerceIn(0, h - 1)

        val hsv = FloatArray(3)
        val deltaSat = if (isSaturate) flow else -flow

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - center.x
                val dy = y - center.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius) continue

                val falloff = 1.0f - dist / radius
                val col = target.getPixel(x, y)
                val a = col ushr 24 and 0xFF
                Color.colorToHSV(col, hsv)

                hsv[1] = (hsv[1] + deltaSat * falloff * 0.3f).coerceIn(0f, 1f)
                val outCol = (a shl 24) or (Color.HSVToColor(hsv) and 0x00FFFFFF)
                target.setPixel(x, y, outCol)
            }
        }
    }

    // --- 7. Smudge Tool (Finger Painting) ---

    private var smudgeSampleColor: Int? = null

    fun applySmudge(
        target: Bitmap,
        from: PointF,
        to: PointF,
        radius: Float,
        strength: Float = 0.5f,
        useFingerPainting: Boolean = false,
        fingerPaintColor: Int = Color.BLACK
    ) {
        val w = target.width
        val h = target.height
        val fx = from.x.toInt().coerceIn(0, w - 1)
        val fy = from.y.toInt().coerceIn(0, h - 1)

        if (useFingerPainting && smudgeSampleColor == null) {
            smudgeSampleColor = fingerPaintColor
        } else if (smudgeSampleColor == null) {
            smudgeSampleColor = target.getPixel(fx, fy)
        }

        val sample = smudgeSampleColor ?: target.getPixel(fx, fy)
        val sR = (sample ushr 16) and 0xFF
        val sG = (sample ushr 8) and 0xFF
        val sB = sample and 0xFF

        val tx = to.x.toInt()
        val ty = to.y.toInt()
        val minX = (tx - radius).toInt().coerceIn(0, w - 1)
        val maxX = (tx + radius).toInt().coerceIn(0, w - 1)
        val minY = (ty - radius).toInt().coerceIn(0, h - 1)
        val maxY = (ty + radius).toInt().coerceIn(0, h - 1)

        var totalR = 0
        var totalG = 0
        var totalB = 0
        var count = 0

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - to.x
                val dy = y - to.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius) continue

                val falloff = (1.0f - dist / radius) * strength
                val cur = target.getPixel(x, y)
                val a = cur ushr 24 and 0xFF
                val cR = (cur ushr 16) and 0xFF
                val cG = (cur ushr 8) and 0xFF
                val cB = cur and 0xFF

                val nR = (cR * (1f - falloff) + sR * falloff).toInt().coerceIn(0, 255)
                val nG = (cG * (1f - falloff) + sG * falloff).toInt().coerceIn(0, 255)
                val nB = (cB * (1f - falloff) + sB * falloff).toInt().coerceIn(0, 255)

                target.setPixel(x, y, (a shl 24) or (nR shl 16) or (nG shl 8) or nB)

                totalR += nR; totalG += nG; totalB += nB
                count++
            }
        }

        // Smudge picks up color along stroke
        if (count > 0) {
            smudgeSampleColor = Color.rgb(totalR / count, totalG / count, totalB / count)
        }
    }

    // --- 8. Sharpen & Blur Brush ---

    fun applySharpenBlurBrush(
        target: Bitmap,
        center: PointF,
        radius: Float,
        isSharpen: Boolean,
        strength: Float = 0.5f
    ) {
        val w = target.width
        val h = target.height
        val minX = (center.x - radius).toInt().coerceIn(1, w - 2)
        val maxX = (center.x + radius).toInt().coerceIn(1, w - 2)
        val minY = (center.y - radius).toInt().coerceIn(1, h - 2)
        val maxY = (center.y + radius).toInt().coerceIn(1, h - 2)

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val dx = x - center.x
                val dy = y - center.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist > radius) continue

                val falloff = (1.0f - dist / radius) * strength
                val cur = target.getPixel(x, y)
                val a = cur ushr 24 and 0xFF

                // 3x3 local neighborhood average
                var sumR = 0; var sumG = 0; var sumB = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val nb = target.getPixel(x + kx, y + ky)
                        sumR += (nb ushr 16) and 0xFF
                        sumG += (nb ushr 8) and 0xFF
                        sumB += nb and 0xFF
                    }
                }
                val avgR = sumR / 9.0f
                val avgG = sumG / 9.0f
                val avgB = sumB / 9.0f

                val cR = ((cur ushr 16) and 0xFF).toFloat()
                val cG = ((cur ushr 8) and 0xFF).toFloat()
                val cB = (cur and 0xFF).toFloat()

                val resR: Float
                val resG: Float
                val resB: Float

                if (isSharpen) {
                    // High-pass sharpening: cur + (cur - avg) * falloff
                    resR = (cR + (cR - avgR) * falloff).coerceIn(0f, 255f)
                    resG = (cG + (cG - avgG) * falloff).coerceIn(0f, 255f)
                    resB = (cB + (cB - avgB) * falloff).coerceIn(0f, 255f)
                } else {
                    // Box blur: blend towards average
                    resR = (cR * (1f - falloff) + avgR * falloff).coerceIn(0f, 255f)
                    resG = (cG * (1f - falloff) + avgG * falloff).coerceIn(0f, 255f)
                    resB = (cB * (1f - falloff) + avgB * falloff).coerceIn(0f, 255f)
                }

                target.setPixel(x, y, (a shl 24) or (resR.toInt() shl 16) or (resG.toInt() shl 8) or resB.toInt())
            }
        }
    }
}
