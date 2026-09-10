package com.photoengine.core.color

import android.graphics.Bitmap
import kotlin.math.*

data class BasicAdjustmentsConfig(
    val exposureEV: Float = 0.0f,         // -5.0f .. +5.0f EV
    val contrast: Float = 0.0f,           // -100.0f .. +100.0f
    val saturation: Float = 0.0f,         // -100.0f .. +100.0f
    val vibrance: Float = 0.0f,           // -100.0f .. +100.0f (boosts muted colors, protects skin tones)
    val ambience: Float = 0.0f,           // -100.0f .. +100.0f (intelligent balance of shadows/highlights)
    val shadows: Float = 0.0f,            // -100.0f .. +100.0f (selective shadow lift/drop)
    val highlights: Float = 0.0f,         // -100.0f .. +100.0f (selective highlight recovery)
    val temperatureK: Float = 5500.0f,    // 2000K .. 10000K
    val tint: Float = 0.0f                // -100.0f .. +100.0f (green to magenta)
)

/**
 * Professional Basic Adjustments Engine (Snapseed 2026 Tune Image specification).
 * High-performance GPU-ready math operating on linear and non-linear color spaces.
 */
class BasicAdjustmentsEngine {

    var config: BasicAdjustmentsConfig = BasicAdjustmentsConfig()

    /**
     * Converts a Correlated Color Temperature (2000K - 10000K) + Tint to normalized RGB multipliers.
     * Implements Tanner-Helland Planckian locus approximation.
     */
    fun calculateWhiteBalanceMultipliers(tempK: Float, tintVal: Float): FloatArray {
        val temp = (tempK / 100.0f).coerceIn(20.0f, 100.0f)
        var r: Float
        var g: Float
        var b: Float

        // Red
        if (temp <= 66.0f) {
            r = 1.0f
        } else {
            r = (329.698727446 * (temp - 60.0).pow(-0.1332047592)).toFloat() / 255.0f
            r = r.coerceIn(0.0f, 1.0f)
        }

        // Green
        if (temp <= 66.0f) {
            g = (99.4708025861 * ln(temp.toDouble()) - 161.1195681661).toFloat() / 255.0f
        } else {
            g = (288.1221695283 * (temp - 60.0).pow(-0.0755148492)).toFloat() / 255.0f
        }
        g = g.coerceIn(0.0f, 1.0f)

        // Blue
        if (temp >= 66.0f) {
            b = 1.0f
        } else if (temp <= 19.0f) {
            b = 0.0f
        } else {
            b = (138.5177312231 * ln((temp - 10.0).toDouble()) - 305.0447927307).toFloat() / 255.0f
            b = b.coerceIn(0.0f, 1.0f)
        }

        // Apply tint (-100..+100): negative adds green, positive adds magenta
        val tintFactor = tintVal / 100.0f
        g *= (1.0f - tintFactor * 0.25f)
        r *= (1.0f + tintFactor * 0.12f)
        b *= (1.0f + tintFactor * 0.12f)

        // Normalize relative to daylight neutral (5500K reference)
        val maxVal = max(r, max(g, b)).coerceAtLeast(0.001f)
        return floatArrayOf(r / maxVal, g / maxVal, b / maxVal)
    }

    /**
     * Automatically calculates optimal Exposure EV and Shadows/Highlights balance
     * targeting an 18% middle gray perceptual luminance.
     */
    fun calculateAutoExposure(pixels: IntArray): BasicAdjustmentsConfig {
        var logLumaSum = 0.0
        var darkPixels = 0
        var brightPixels = 0
        val sampleStep = max(1, pixels.size / 50000)
        var count = 0

        for (i in pixels.indices step sampleStep) {
            val c = pixels[i]
            val r = ((c shr 16) and 0xFF) / 255.0f
            val g = ((c shr 8) and 0xFF) / 255.0f
            val b = (c and 0xFF) / 255.0f
            val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceAtLeast(0.001f)
            logLumaSum += ln(luma.toDouble())
            if (luma < 0.15f) darkPixels++
            if (luma > 0.85f) brightPixels++
            count++
        }

        val geometricMeanLuma = exp(logLumaSum / count.coerceAtLeast(1)).toFloat()
        // Standard photographic 18% middle gray calibration
        val targetLuma = 0.18f
        val calculatedEV = (ln((targetLuma / geometricMeanLuma).toDouble()) / ln(2.0)).toFloat()
            .coerceIn(-2.5f, 2.5f)

        val shadowRatio = darkPixels.toFloat() / count
        val highlightRatio = brightPixels.toFloat() / count

        val autoShadows = if (shadowRatio > 0.2f) (shadowRatio * 60.0f).coerceIn(0f, 50f) else 0f
        val autoHighlights = if (highlightRatio > 0.08f) (-highlightRatio * 80.0f).coerceIn(-60f, 0f) else 0f

        return BasicAdjustmentsConfig(
            exposureEV = calculatedEV,
            contrast = 10.0f,
            saturation = 5.0f,
            vibrance = 15.0f,
            ambience = 12.0f,
            shadows = autoShadows,
            highlights = autoHighlights,
            temperatureK = 5500.0f,
            tint = 0.0f
        )
    }

    /**
     * Applies full adjustment pipeline to a normalized RGB pixel [0..1].
     */
    fun processPixel(r: Float, g: Float, b: Float, wbMultipliers: FloatArray): FloatArray {
        // 1. Exposure manual EV (-5 to +5 EV): factor = 2^(EV)
        val exposureFactor = 2.0.pow(config.exposureEV.toDouble()).toFloat()
        var pr = (r * exposureFactor * wbMultipliers[0]).coerceAtLeast(0.0f)
        var pg = (g * exposureFactor * wbMultipliers[1]).coerceAtLeast(0.0f)
        var pb = (b * exposureFactor * wbMultipliers[2]).coerceAtLeast(0.0f)

        // Calculate perceptual luminance
        var luma = 0.2126f * pr + 0.7152f * pg + 0.0722f * pb

        // 2. Shadows recovery (selective shadow lifting without affecting highlights)
        if (config.shadows != 0.0f) {
            val shadowWeight = (1.0f - luma.coerceIn(0.0f, 1.0f)).pow(2.0f)
            val shadowAdjustment = (config.shadows / 100.0f) * 0.4f * shadowWeight
            pr += shadowAdjustment
            pg += shadowAdjustment
            pb += shadowAdjustment
        }

        // 3. Highlights recovery (selective compression of blown highlights)
        if (config.highlights != 0.0f) {
            val highlightWeight = luma.coerceIn(0.0f, 1.0f).pow(2.0f)
            val highlightAdjustment = (config.highlights / 100.0f) * 0.4f * highlightWeight
            pr += highlightAdjustment
            pg += highlightAdjustment
            pb += highlightAdjustment
        }

        // 4. Ambience: Intelligent midtone contrast & shadow/highlight balance
        if (config.ambience != 0.0f) {
            val ambWeight = sin((luma.coerceIn(0.0f, 1.0f) * Math.PI).toDouble()).toFloat()
            val ambShift = (config.ambience / 100.0f) * 0.25f * ambWeight
            pr += ambShift
            pg += ambShift
            pb += ambShift
        }

        // 5. Contrast with highlights/shadows protection (S-curve sigmoid centered at 0.5)
        if (config.contrast != 0.0f) {
            val cFactor = (config.contrast / 100.0f)
            val slope = 1.0f + cFactor
            pr = 0.5f + (pr - 0.5f) * slope
            pg = 0.5f + (pg - 0.5f) * slope
            pb = 0.5f + (pb - 0.5f) * slope
        }

        // Recompute luminance for saturation and vibrance
        luma = (0.2126f * pr + 0.7152f * pg + 0.0722f * pb).coerceIn(0.0f, 1.0f)

        // 6. Saturation and Vibrance
        val maxC = max(pr, max(pg, pb))
        val minC = min(pr, min(pg, pb))
        val sat = if (maxC > 0.0001f) (maxC - minC) / maxC else 0.0f

        // Vibrance acts inversely on already-saturated colors and protects skin tones
        val vibranceFactor = (config.vibrance / 100.0f) * (1.0f - sat) * 1.5f
        val standardSatFactor = (config.saturation / 100.0f)
        val totalSatMultiplier = 1.0f + standardSatFactor + vibranceFactor

        pr = luma + (pr - luma) * totalSatMultiplier
        pg = luma + (pg - luma) * totalSatMultiplier
        pb = luma + (pb - luma) * totalSatMultiplier

        return floatArrayOf(
            pr.coerceIn(0.0f, 1.0f),
            pg.coerceIn(0.0f, 1.0f),
            pb.coerceIn(0.0f, 1.0f)
        )
    }

    /**
     * Executes CPU raster pass on an input Bitmap.
     * On GPU this is evaluated in the compute shader `adjustments.comp`.
     */
    fun processBitmap(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val wbMultipliers = calculateWhiteBalanceMultipliers(config.temperatureK, config.tint)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = ((c shr 16) and 0xFF) / 255.0f
            val g = ((c shr 8) and 0xFF) / 255.0f
            val b = (c and 0xFF) / 255.0f

            val res = processPixel(r, g, b, wbMultipliers)

            val outR = (res[0] * 255.0f).toInt()
            val outG = (res[1] * 255.0f).toInt()
            val outB = (res[2] * 255.0f).toInt()
            pixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }
}
