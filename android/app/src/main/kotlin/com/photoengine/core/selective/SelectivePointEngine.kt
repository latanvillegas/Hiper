package com.photoengine.core.selective

import android.graphics.Bitmap
import kotlin.math.*

data class UPoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    var x: Float = 0.5f,              // Normalized 0.0 .. 1.0
    var y: Float = 0.5f,              // Normalized 0.0 .. 1.0
    var size: Float = 40.0f,          // 1.0 .. 100.0 (affects radius in image pixels)
    var intensity: Float = 100.0f,    // 0.0 .. 100.0 (overall strength)
    var brightness: Float = 0.0f,     // -100.0 .. +100.0
    var contrast: Float = 0.0f,       // -100.0 .. +100.0
    var saturation: Float = 0.0f,     // -100.0 .. +100.0
    var structure: Float = 0.0f,      // -100.0 .. +100.0
    var sampleR: Float = 0.5f,        // Sampled reference color at point location
    var sampleG: Float = 0.5f,
    var sampleB: Float = 0.5f
)

/**
 * Professional U-Point (Selective Control Points) Engine.
 * Supports unlimited control points with Nik Software / Snapseed similarity matching.
 * Compares local pixels to the anchor point's color in perceptual CIE-Lab space.
 */
class SelectivePointEngine {

    val controlPoints = mutableListOf<UPoint>()

    fun addPoint(point: UPoint) {
        controlPoints.add(point)
    }

    fun removePoint(id: String) {
        controlPoints.removeAll { it.id == id }
    }

    fun clearPoints() {
        controlPoints.clear()
    }

    /**
     * Samples the color at the point's position from the input bitmap to calibrate similarity.
     */
    fun sampleAnchorColor(bitmap: Bitmap, point: UPoint) {
        val px = (point.x * (bitmap.width - 1)).toInt().coerceIn(0, bitmap.width - 1)
        val py = (point.y * (bitmap.height - 1)).toInt().coerceIn(0, bitmap.height - 1)
        val color = bitmap.getPixel(px, py)
        point.sampleR = ((color shr 16) and 0xFF) / 255.0f
        point.sampleG = ((color shr 8) and 0xFF) / 255.0f
        point.sampleB = (color and 0xFF) / 255.0f
    }

    /**
     * Converts RGB to approximate CIE-Lab [L*, a*, b*] for perceptual Delta E color distance.
     */
    private fun rgbToLab(r: Float, g: Float, b: Float): FloatArray {
        // sRGB to Linear
        val lr = if (r > 0.04045f) ((r + 0.055f) / 1.055f).pow(2.4f) else r / 12.92f
        val lg = if (g > 0.04045f) ((g + 0.055f) / 1.055f).pow(2.4f) else g / 12.92f
        val lb = if (b > 0.04045f) ((b + 0.055f) / 1.055f).pow(2.4f) else b / 12.92f

        // Linear RGB to XYZ (D65 illuminant)
        val x = (0.4124564f * lr + 0.3575761f * lg + 0.1804375f * lb) / 0.95047f
        val y = (0.2126729f * lr + 0.7151522f * lg + 0.0721750f * lb) / 1.00000f
        val z = (0.0193339f * lr + 0.1191920f * lg + 0.9503041f * lb) / 1.08883f

        fun f(t: Float): Float = if (t > 0.008856f) t.pow(1.0f / 3.0f) else (7.787f * t) + (16.0f / 116.0f)

        val fx = f(x)
        val fy = f(y)
        val fz = f(z)

        val l = (116.0f * fy) - 16.0f
        val a = 500.0f * (fx - fy)
        val bVal = 200.0f * (fy - fz)
        return floatArrayOf(l, a, bVal)
    }

    /**
     * Calculates the selective influence weight of a U-Point at coordinate (u, v) with color (r, g, b).
     * Combines spatial Euclidean decay with CIE-Lab perceptual Delta E similarity.
     */
    fun calculateWeight(
        point: UPoint,
        u: Float,
        v: Float,
        r: Float,
        g: Float,
        b: Float,
        imageAspect: Float
    ): Float {
        // 1. Spatial distance falloff (Euclidean with aspect correction)
        val dx = (u - point.x) * imageAspect
        val dy = (v - point.y)
        val dist = sqrt(dx * dx + dy * dy)

        val maxRadius = (point.size / 100.0f) * 0.65f // Up to 65% of screen
        if (dist >= maxRadius) return 0.0f

        val spatialWeight = (1.0f - dist / maxRadius).pow(2.0f)

        // 2. Color similarity falloff in CIE-Lab space
        val labPoint = rgbToLab(point.sampleR, point.sampleG, point.sampleB)
        val labPixel = rgbToLab(r, g, b)

        val dL = labPoint[0] - labPixel[0]
        val da = labPoint[1] - labPixel[1]
        val db = labPoint[2] - labPixel[2]
        val deltaE = sqrt(dL * dL + da * da + db * db)

        // Typical JND (Just Noticeable Difference) in Lab is 2.3; generous tolerance sigma = 22.0
        val colorSigma = 24.0f
        val colorWeight = exp(- (deltaE * deltaE) / (2.0f * colorSigma * colorSigma))

        val overallStrength = point.intensity / 100.0f
        return (spatialWeight * colorWeight * overallStrength).coerceIn(0.0f, 1.0f)
    }

    /**
     * Processes bitmap with all active selective control points.
     */
    fun processBitmap(bitmap: Bitmap): Bitmap {
        if (controlPoints.isEmpty()) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val aspect = w.toFloat() / h.toFloat()
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Ensure anchor colors are sampled
        controlPoints.forEach { pt ->
            sampleAnchorColor(bitmap, pt)
        }

        for (y in 0 until h) {
            val v = y.toFloat() / (h - 1)
            for (x in 0 until w) {
                val u = x.toFloat() / (w - 1)
                val idx = y * w + x
                val c = pixels[idx]

                var r = ((c shr 16) and 0xFF) / 255.0f
                var g = ((c shr 8) and 0xFF) / 255.0f
                var b = (c and 0xFF) / 255.0f

                for (pt in controlPoints) {
                    val weight = calculateWeight(pt, u, v, r, g, b, aspect)
                    if (weight <= 0.001f) continue

                    // 1. Brightness
                    if (pt.brightness != 0.0f) {
                        val bShift = (pt.brightness / 100.0f) * 0.35f * weight
                        r += bShift
                        g += bShift
                        b += bShift
                    }

                    // 2. Contrast
                    if (pt.contrast != 0.0f) {
                        val cFactor = (pt.contrast / 100.0f) * weight
                        val slope = 1.0f + cFactor
                        r = 0.5f + (r - 0.5f) * slope
                        g = 0.5f + (g - 0.5f) * slope
                        b = 0.5f + (b - 0.5f) * slope
                    }

                    // 3. Saturation
                    if (pt.saturation != 0.0f) {
                        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                        val sFactor = 1.0f + (pt.saturation / 100.0f) * weight
                        r = luma + (r - luma) * sFactor
                        g = luma + (g - luma) * sFactor
                        b = luma + (b - luma) * sFactor
                    }

                    // 4. Structure / Clarity
                    if (pt.structure != 0.0f) {
                        // High-pass micro-contrast approximation
                        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                        val structureFactor = (pt.structure / 100.0f) * 0.25f * weight
                        val midToneMask = sin((luma.coerceIn(0f, 1f) * Math.PI).toDouble()).toFloat()
                        val diff = (luma - 0.5f) * structureFactor * midToneMask
                        r += diff
                        g += diff
                        b += diff
                    }
                }

                val outR = (r.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val outG = (g.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val outB = (b.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                pixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }
}
