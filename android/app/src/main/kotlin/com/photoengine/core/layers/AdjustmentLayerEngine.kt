package com.photoengine.core.layers

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

sealed class AdjustmentConfig {
    data class BrightnessContrast(
        val brightness: Float = 0.0f, // -100 .. +100
        val contrast: Float = 0.0f,   // -100 .. +100
        val useLegacy: Boolean = false
    ) : AdjustmentConfig()

    data class Levels(
        val inBlack: Int = 0,       // 0 .. 253
        val gamma: Float = 1.0f,    // 0.1 .. 9.99
        val inWhite: Int = 255,     // 2 .. 255
        val outBlack: Int = 0,      // 0 .. 255
        val outWhite: Int = 255     // 0 .. 255
    ) : AdjustmentConfig()

    data class Curves(
        val rgbPoints: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val redPoints: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val greenPoints: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
        val bluePoints: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f)
    ) : AdjustmentConfig()

    data class Exposure(
        val exposure: Float = 0.0f, // -5.0 .. +5.0 EV
        val offset: Float = 0.0f,   // -0.5 .. +0.5
        val gamma: Float = 1.0f     // 0.2 .. 5.0
    ) : AdjustmentConfig()

    data class Vibrance(
        val vibrance: Float = 0.0f,   // -100 .. +100
        val saturation: Float = 0.0f  // -100 .. +100
    ) : AdjustmentConfig()

    data class HueSaturation(
        val hue: Float = 0.0f,        // -180 .. +180°
        val saturation: Float = 0.0f, // -100 .. +100%
        val lightness: Float = 0.0f,  // -100 .. +100%
        val colorize: Boolean = false
    ) : AdjustmentConfig()

    data class ColorBalance(
        // Shadows, Midtones, Highlights with (Cyan-Red, Magenta-Green, Yellow-Blue) in -100..+100
        val shadowCR: Float = 0f, val shadowMG: Float = 0f, val shadowYB: Float = 0f,
        val midtoneCR: Float = 0f, val midtoneMG: Float = 0f, val midtoneYB: Float = 0f,
        val highlightCR: Float = 0f, val highlightMG: Float = 0f, val highlightYB: Float = 0f,
        val preserveLuminosity: Boolean = true
    ) : AdjustmentConfig()

    data class BlackAndWhite(
        val redWeight: Float = 40f,
        val yellowWeight: Float = 60f,
        val greenWeight: Float = 40f,
        val cyanWeight: Float = 60f,
        val blueWeight: Float = 20f,
        val magentaWeight: Float = 80f,
        val tintHue: Float? = null,
        val tintSat: Float = 0f
    ) : AdjustmentConfig()

    data class PhotoFilter(
        val filterColor: Int = Color.rgb(236, 138, 0), // Default Warming Filter 85
        val density: Float = 0.25f,                   // 0.0 .. 1.0
        val preserveLuminosity: Boolean = true
    ) : AdjustmentConfig()

    object Invert : AdjustmentConfig()

    data class Posterize(
        val levels: Int = 4 // 2 .. 255
    ) : AdjustmentConfig()

    data class Threshold(
        val threshold: Int = 128 // 1 .. 255
    ) : AdjustmentConfig()

    data class GradientMapStop(val position: Float, val color: Int)
    data class GradientMap(
        val stops: List<GradientMapStop> = listOf(
            GradientMapStop(0.0f, Color.BLACK),
            GradientMapStop(1.0f, Color.WHITE)
        ),
        val reverse: Boolean = false
    ) : AdjustmentConfig()
}

/**
 * High-performance non-destructive Photoshop Adjustment Layer Engine.
 * Evaluates full 32-bit floating point lookups with accurate perceptual color science.
 */
class AdjustmentLayerEngine {

    fun applyAdjustment(source: Bitmap, config: AdjustmentConfig): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        when (config) {
            is AdjustmentConfig.BrightnessContrast -> processBrightnessContrast(pixels, config)
            is AdjustmentConfig.Levels -> processLevels(pixels, config)
            is AdjustmentConfig.Curves -> processCurves(pixels, config)
            is AdjustmentConfig.Exposure -> processExposure(pixels, config)
            is AdjustmentConfig.Vibrance -> processVibrance(pixels, config)
            is AdjustmentConfig.HueSaturation -> processHueSaturation(pixels, config)
            is AdjustmentConfig.ColorBalance -> processColorBalance(pixels, config)
            is AdjustmentConfig.BlackAndWhite -> processBlackAndWhite(pixels, config)
            is AdjustmentConfig.PhotoFilter -> processPhotoFilter(pixels, config)
            is AdjustmentConfig.Invert -> processInvert(pixels)
            is AdjustmentConfig.Posterize -> processPosterize(pixels, config)
            is AdjustmentConfig.Threshold -> processThreshold(pixels, config)
            is AdjustmentConfig.GradientMap -> processGradientMap(pixels, config)
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun processBrightnessContrast(pixels: IntArray, cfg: AdjustmentConfig.BrightnessContrast) {
        val b = cfg.brightness / 100.0f
        val c = cfg.contrast / 100.0f
        val factor = (1.0f + c) / (1.0f - c).coerceAtLeast(0.001f)

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            var r = (((col ushr 16) and 0xFF) / 255.0f)
            var g = (((col ushr 8) and 0xFF) / 255.0f)
            var bVal = ((col and 0xFF) / 255.0f)

            // Brightness
            r = (r + b).coerceIn(0f, 1f)
            g = (g + b).coerceIn(0f, 1f)
            bVal = (bVal + b).coerceIn(0f, 1f)

            // Contrast around 0.5 midpoint
            r = ((r - 0.5f) * factor + 0.5f).coerceIn(0f, 1f)
            g = ((g - 0.5f) * factor + 0.5f).coerceIn(0f, 1f)
            bVal = ((bVal - 0.5f) * factor + 0.5f).coerceIn(0f, 1f)

            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (bVal * 255).toInt()
        }
    }

    private fun processLevels(pixels: IntArray, cfg: AdjustmentConfig.Levels) {
        val inMin = cfg.inBlack / 255.0f
        val inMax = cfg.inWhite / 255.0f
        val inRange = (inMax - inMin).coerceAtLeast(0.001f)
        val invGamma = 1.0f / cfg.gamma.coerceAtLeast(0.01f)
        val outMin = cfg.outBlack / 255.0f
        val outMax = cfg.outWhite / 255.0f
        val outRange = outMax - outMin

        fun mapLevel(v: Float): Float {
            val normalized = ((v - inMin) / inRange).coerceIn(0f, 1f)
            val gammaCorrected = normalized.pow(invGamma)
            return (outMin + gammaCorrected * outRange).coerceIn(0f, 1f)
        }

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = mapLevel(((col ushr 16) and 0xFF) / 255.0f)
            val g = mapLevel(((col ushr 8) and 0xFF) / 255.0f)
            val b = mapLevel((col and 0xFF) / 255.0f)
            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }

    private fun processCurves(pixels: IntArray, cfg: AdjustmentConfig.Curves) {
        val lutR = buildCurveLut(cfg.rgbPoints, cfg.redPoints)
        val lutG = buildCurveLut(cfg.rgbPoints, cfg.greenPoints)
        val lutB = buildCurveLut(cfg.rgbPoints, cfg.bluePoints)

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = (col ushr 16) and 0xFF
            val g = (col ushr 8) and 0xFF
            val b = col and 0xFF
            pixels[i] = (a shl 24) or (lutR[r] shl 16) or (lutG[g] shl 8) or lutB[b]
        }
    }

    private fun buildCurveLut(master: List<Pair<Float, Float>>, channel: List<Pair<Float, Float>>): IntArray {
        val lut = IntArray(256)
        for (i in 0..255) {
            val x = i / 255.0f
            val mVal = interpolateSpline(master, x)
            val cVal = interpolateSpline(channel, mVal)
            lut[i] = (cVal.coerceIn(0f, 1f) * 255).toInt()
        }
        return lut
    }

    private fun interpolateSpline(points: List<Pair<Float, Float>>, x: Float): Float {
        if (points.isEmpty()) return x
        if (points.size == 1) return points[0].second
        if (x <= points.first().first) return points.first().second
        if (x >= points.last().first) return points.last().second

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            if (x in p0.first..p1.first) {
                val t = (x - p0.first) / (p1.first - p0.first).coerceAtLeast(0.0001f)
                // Smooth Hermite interpolation
                val t2 = t * t
                val t3 = t2 * t
                val h00 = 2 * t3 - 3 * t2 + 1
                val h10 = t3 - 2 * t2 + t
                val h01 = -2 * t3 + 3 * t2
                val h11 = t3 - t2
                return h00 * p0.second + h01 * p1.second
            }
        }
        return x
    }

    private fun processExposure(pixels: IntArray, cfg: AdjustmentConfig.Exposure) {
        val mult = 2.0f.pow(cfg.exposure)
        val off = cfg.offset
        val invGamma = 1.0f / cfg.gamma.coerceAtLeast(0.01f)

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            var r = (((col ushr 16) and 0xFF) / 255.0f) * mult + off
            var g = (((col ushr 8) and 0xFF) / 255.0f) * mult + off
            var b = ((col and 0xFF) / 255.0f) * mult + off

            r = r.coerceAtLeast(0f).pow(invGamma).coerceIn(0f, 1f)
            g = g.coerceAtLeast(0f).pow(invGamma).coerceIn(0f, 1f)
            b = b.coerceAtLeast(0f).pow(invGamma).coerceIn(0f, 1f)

            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }

    private fun processVibrance(pixels: IntArray, cfg: AdjustmentConfig.Vibrance) {
        val vib = cfg.vibrance / 100.0f
        val sat = cfg.saturation / 100.0f

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            var r = ((col ushr 16) and 0xFF) / 255.0f
            var g = ((col ushr 8) and 0xFF) / 255.0f
            var b = (col and 0xFF) / 255.0f

            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            val currentSat = if (mx <= 0.001f) 0f else (mx - mn) / mx

            // Vibrance acts more strongly on desaturated pixels
            val vibBoost = (1.0f - currentSat) * vib * 0.8f
            val totalSat = (sat + vibBoost).coerceIn(-1.0f, 1.0f)

            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            r = (luma + (r - luma) * (1.0f + totalSat)).coerceIn(0f, 1f)
            g = (luma + (g - luma) * (1.0f + totalSat)).coerceIn(0f, 1f)
            b = (luma + (b - luma) * (1.0f + totalSat)).coerceIn(0f, 1f)

            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }

    private fun processHueSaturation(pixels: IntArray, cfg: AdjustmentConfig.HueSaturation) {
        val hsv = FloatArray(3)
        val dHue = cfg.hue
        val dSat = cfg.saturation / 100.0f
        val dLit = cfg.lightness / 100.0f

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            Color.colorToHSV(col, hsv)

            if (cfg.colorize) {
                hsv[0] = (dHue + 180f).coerceIn(0f, 360f)
                hsv[1] = (dSat + 1f).coerceIn(0f, 1f) * 0.5f
            } else {
                hsv[0] = (hsv[0] + dHue + 360f) % 360f
                hsv[1] = (hsv[1] * (1.0f + dSat)).coerceIn(0f, 1f)
            }
            hsv[2] = (hsv[2] + dLit).coerceIn(0f, 1f)

            val newColor = Color.HSVToColor(a, hsv)
            pixels[i] = newColor
        }
    }

    private fun processColorBalance(pixels: IntArray, cfg: AdjustmentConfig.ColorBalance) {
        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            var r = ((col ushr 16) and 0xFF) / 255.0f
            var g = ((col ushr 8) and 0xFF) / 255.0f
            var b = (col and 0xFF) / 255.0f

            val luma = 0.299f * r + 0.587f * g + 0.114f * b

            // Shadow weight (0..0.33), Midtone (0.33..0.66), Highlight (0.66..1.0)
            val wShadow = (1.0f - luma / 0.5f).coerceIn(0f, 1f)
            val wHighlight = ((luma - 0.5f) / 0.5f).coerceIn(0f, 1f)
            val wMidtone = (1.0f - abs(luma - 0.5f) * 2f).coerceIn(0f, 1f)

            val dR = (cfg.shadowCR * wShadow + cfg.midtoneCR * wMidtone + cfg.highlightCR * wHighlight) / 100f * 0.25f
            val dG = (cfg.shadowMG * wShadow + cfg.midtoneMG * wMidtone + cfg.highlightMG * wHighlight) / 100f * 0.25f
            val dB = (cfg.shadowYB * wShadow + cfg.midtoneYB * wMidtone + cfg.highlightYB * wHighlight) / 100f * 0.25f

            r = (r + dR).coerceIn(0f, 1f)
            g = (g + dG).coerceIn(0f, 1f)
            b = (b + dB).coerceIn(0f, 1f)

            if (cfg.preserveLuminosity) {
                val newLuma = 0.299f * r + 0.587f * g + 0.114f * b
                val diff = luma - newLuma
                r = (r + diff).coerceIn(0f, 1f)
                g = (g + diff).coerceIn(0f, 1f)
                b = (b + diff).coerceIn(0f, 1f)
            }

            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }

    private fun processBlackAndWhite(pixels: IntArray, cfg: AdjustmentConfig.BlackAndWhite) {
        val total = cfg.redWeight + cfg.yellowWeight + cfg.greenWeight + cfg.cyanWeight + cfg.blueWeight + cfg.magentaWeight
        val wR = cfg.redWeight / total
        val wY = cfg.yellowWeight / total
        val wG = cfg.greenWeight / total
        val wC = cfg.cyanWeight / total
        val wB = cfg.blueWeight / total
        val wM = cfg.magentaWeight / total

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = ((col ushr 16) and 0xFF) / 255.0f
            val g = ((col ushr 8) and 0xFF) / 255.0f
            val b = (col and 0xFF) / 255.0f

            // Photoshop 6-channel B&W weighting
            val gray = (r * wR + (r * 0.5f + g * 0.5f) * wY + g * wG + (g * 0.5f + b * 0.5f) * wC + b * wB + (r * 0.5f + b * 0.5f) * wM).coerceIn(0f, 1f)

            if (cfg.tintHue != null && cfg.tintSat > 0f) {
                val tintColor = Color.HSVToColor(floatArrayOf(cfg.tintHue, cfg.tintSat / 100f, gray))
                pixels[i] = (a shl 24) or (tintColor and 0x00FFFFFF)
            } else {
                val gByte = (gray * 255).toInt()
                pixels[i] = (a shl 24) or (gByte shl 16) or (gByte shl 8) or gByte
            }
        }
    }

    private fun processPhotoFilter(pixels: IntArray, cfg: AdjustmentConfig.PhotoFilter) {
        val fR = ((cfg.filterColor ushr 16) and 0xFF) / 255.0f
        val fG = ((cfg.filterColor ushr 8) and 0xFF) / 255.0f
        val fB = (cfg.filterColor and 0xFF) / 255.0f
        val d = cfg.density

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            var r = ((col ushr 16) and 0xFF) / 255.0f
            var g = ((col ushr 8) and 0xFF) / 255.0f
            var b = (col and 0xFF) / 255.0f

            val origLuma = 0.299f * r + 0.587f * g + 0.114f * b
            r = (r * (1f - d) + (r * fR) * d).coerceIn(0f, 1f)
            g = (g * (1f - d) + (g * fG) * d).coerceIn(0f, 1f)
            b = (b * (1f - d) + (b * fB) * d).coerceIn(0f, 1f)

            if (cfg.preserveLuminosity) {
                val newLuma = 0.299f * r + 0.587f * g + 0.114f * b
                val diff = origLuma - newLuma
                r = (r + diff).coerceIn(0f, 1f)
                g = (g + diff).coerceIn(0f, 1f)
                b = (b + diff).coerceIn(0f, 1f)
            }

            pixels[i] = (a shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }
    }

    private fun processInvert(pixels: IntArray) {
        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = 255 - ((col ushr 16) and 0xFF)
            val g = 255 - ((col ushr 8) and 0xFF)
            val b = 255 - (col and 0xFF)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private fun processPosterize(pixels: IntArray, cfg: AdjustmentConfig.Posterize) {
        val steps = cfg.levels.coerceIn(2, 255)
        val stepSize = 255.0f / (steps - 1)

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = (round(((col ushr 16) and 0xFF) / stepSize) * stepSize).toInt().coerceIn(0, 255)
            val g = (round(((col ushr 8) and 0xFF) / stepSize) * stepSize).toInt().coerceIn(0, 255)
            val b = (round((col and 0xFF) / stepSize) * stepSize).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private fun processThreshold(pixels: IntArray, cfg: AdjustmentConfig.Threshold) {
        val th = cfg.threshold
        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = (col ushr 16) and 0xFF
            val g = (col ushr 8) and 0xFF
            val b = col and 0xFF
            val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            val bin = if (luma >= th) 255 else 0
            pixels[i] = (a shl 24) or (bin shl 16) or (bin shl 8) or bin
        }
    }

    private fun processGradientMap(pixels: IntArray, cfg: AdjustmentConfig.GradientMap) {
        val stops = if (cfg.reverse) cfg.stops.reversed().map { it.copy(position = 1.0f - it.position) } else cfg.stops
        val rampLut = IntArray(256)

        for (i in 0..255) {
            val pos = i / 255.0f
            rampLut[i] = sampleGradient(stops, pos)
        }

        for (i in pixels.indices) {
            val col = pixels[i]
            val a = (col ushr 24) and 0xFF
            val r = (col ushr 16) and 0xFF
            val g = (col ushr 8) and 0xFF
            val b = col and 0xFF
            val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            val mapped = rampLut[luma]
            pixels[i] = (a shl 24) or (mapped and 0x00FFFFFF)
        }
    }

    private fun sampleGradient(stops: List<AdjustmentConfig.GradientMapStop>, pos: Float): Int {
        if (stops.isEmpty()) return Color.BLACK
        if (stops.size == 1) return stops[0].color
        if (pos <= stops.first().position) return stops.first().color
        if (pos >= stops.last().position) return stops.last().color

        for (i in 0 until stops.size - 1) {
            val s0 = stops[i]
            val s1 = stops[i + 1]
            if (pos in s0.position..s1.position) {
                val t = (pos - s0.position) / (s1.position - s0.position).coerceAtLeast(0.0001f)
                val r = ((1f - t) * ((s0.color ushr 16) and 0xFF) + t * ((s1.color ushr 16) and 0xFF)).toInt()
                val g = ((1f - t) * ((s0.color ushr 8) and 0xFF) + t * ((s1.color ushr 8) and 0xFF)).toInt()
                val b = ((1f - t) * (s0.color and 0xFF) + t * (s1.color and 0xFF)).toInt()
                return Color.rgb(r, g, b)
            }
        }
        return stops.last().color
    }
}
