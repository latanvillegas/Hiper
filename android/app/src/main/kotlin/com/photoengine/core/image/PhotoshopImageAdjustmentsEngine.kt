package com.photoengine.core.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

data class ChannelMixerConfig(
    val redChannelR: Float = 100f, val redChannelG: Float = 0f, val redChannelB: Float = 0f, val redConst: Float = 0f,
    val greenChannelR: Float = 0f, val greenChannelG: Float = 100f, val greenChannelB: Float = 0f, val greenConst: Float = 0f,
    val blueChannelR: Float = 0f, val blueChannelG: Float = 0f, val blueChannelB: Float = 100f, val blueConst: Float = 0f,
    val isMonochrome: Boolean = false,
    val monoR: Float = 40f, val monoG: Float = 40f, val monoB: Float = 20f, val monoConst: Float = 0f
)

/**
 * 3D LUT (Look-Up Table) container for Adobe / DaVinci .cube format.
 */
class Lut3D(val size: Int, val table: Array<FloatArray>) {
    fun sample(r: Float, g: Float, b: Float): FloatArray {
        val s = (size - 1).toFloat()
        val rx = (r * s).coerceIn(0f, s)
        val gy = (g * s).coerceIn(0f, s)
        val bz = (b * s).coerceIn(0f, s)

        val r0 = rx.toInt().coerceIn(0, size - 2)
        val r1 = r0 + 1
        val g0 = gy.toInt().coerceIn(0, size - 2)
        val g1 = g0 + 1
        val b0 = bz.toInt().coerceIn(0, size - 2)
        val b1 = b0 + 1

        val fr = rx - r0
        val fg = gy - g0
        val fb = bz - b0

        fun getLut(rIdx: Int, gIdx: Int, bIdx: Int): FloatArray {
            val idx = (bIdx * size * size) + (gIdx * size) + rIdx
            return table[idx.coerceIn(0, table.size - 1)]
        }

        // Trilinear interpolation
        val c000 = getLut(r0, g0, b0)
        val c100 = getLut(r1, g0, b0)
        val c010 = getLut(r0, g1, b0)
        val c110 = getLut(r1, g1, b0)
        val c001 = getLut(r0, g0, b1)
        val c101 = getLut(r1, g0, b1)
        val c011 = getLut(r0, g1, b1)
        val c111 = getLut(r1, g1, b1)

        val out = FloatArray(3)
        for (i in 0..2) {
            val c00 = c000[i] * (1f - fr) + c100[i] * fr
            val c10 = c010[i] * (1f - fr) + c110[i] * fr
            val c01 = c001[i] * (1f - fr) + c101[i] * fr
            val c11 = c011[i] * (1f - fr) + c111[i] * fr

            val c0 = c00 * (1f - fg) + c10 * fg
            val c1 = c01 * (1f - fg) + c11 * fg
            out[i] = (c0 * (1f - fb) + c1 * fb).coerceIn(0f, 1f)
        }
        return out
    }
}

/**
 * Professional Photoshop Image Adjustments.
 * Includes:
 * - Auto Tone (RGB histogram stretch per individual channel)
 * - Auto Contrast (Master luminance histogram stretch)
 * - Auto Color (Histogram clip with neutral midtone color cast neutralization)
 * - Channel Mixer (Full RGB + Monochrome matrix)
 * - 3D LUT Color Lookup (Trilinear interpolation)
 * - Split Toning (Shadows & Highlights chromatic split with crossover balance)
 */
class PhotoshopImageAdjustmentsEngine {

    // --- 1. Auto Tone, Auto Contrast, Auto Color ---

    fun autoTone(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val histR = IntArray(256)
        val histG = IntArray(256)
        val histB = IntArray(256)

        for (c in pixels) {
            histR[(c ushr 16) and 0xFF]++
            histG[(c ushr 8) and 0xFF]++
            histB[c and 0xFF]++
        }

        val total = w * h
        val clipLow = (total * 0.005f).toInt()
        val clipHigh = (total * 0.995f).toInt()

        fun findLimits(hist: IntArray): Pair<Int, Int> {
            var cum = 0
            var low = 0
            for (i in 0..255) {
                cum += hist[i]
                if (cum >= clipLow) { low = i; break }
            }
            cum = 0
            var high = 255
            for (i in 255 downTo 0) {
                cum += hist[i]
                if (cum >= (total - clipHigh)) { high = i; break }
            }
            return low to max(low + 1, high)
        }

        val (rMin, rMax) = findLimits(histR)
        val (gMin, gMax) = findLimits(histG)
        val (bMin, bMax) = findLimits(histB)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val r = ((((c ushr 16) and 0xFF) - rMin) * 255f / (rMax - rMin)).toInt().coerceIn(0, 255)
            val g = ((((c ushr 8) and 0xFF) - gMin) * 255f / (gMax - gMin)).toInt().coerceIn(0, 255)
            val b = (((c and 0xFF) - bMin) * 255f / (bMax - bMin)).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    fun autoContrast(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val histLuma = IntArray(256)
        for (c in pixels) {
            val luma = (0.299f * ((c ushr 16) and 0xFF) + 0.587f * ((c ushr 8) and 0xFF) + 0.114f * (c and 0xFF)).toInt()
            histLuma[luma]++
        }

        val total = w * h
        val clipLow = (total * 0.005f).toInt()
        val clipHigh = (total * 0.995f).toInt()

        var cum = 0
        var lMin = 0
        for (i in 0..255) {
            cum += histLuma[i]
            if (cum >= clipLow) { lMin = i; break }
        }
        cum = 0
        var lMax = 255
        for (i in 255 downTo 0) {
            cum += histLuma[i]
            if (cum >= (total - clipHigh)) { lMax = i; break }
        }
        val range = max(1, lMax - lMin).toFloat()

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val r = ((((c ushr 16) and 0xFF) - lMin) * 255f / range).toInt().coerceIn(0, 255)
            val g = ((((c ushr 8) and 0xFF) - lMin) * 255f / range).toInt().coerceIn(0, 255)
            val b = (((c and 0xFF) - lMin) * 255f / range).toInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    fun autoColor(source: Bitmap): Bitmap {
        // Auto Tone + Midtone Gray neutralization
        val toned = autoTone(source)
        val w = toned.width
        val h = toned.height
        val pixels = IntArray(w * h)
        toned.getPixels(pixels, 0, w, 0, 0, w, h)

        var sumR = 0L; var sumG = 0L; var sumB = 0L; var count = 0
        for (c in pixels) {
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            val luma = 0.299f * r + 0.587f * g + 0.114f * b
            // Consider midtones (25% to 75% luma)
            if (luma in 64f..192f) {
                sumR += r; sumG += g; sumB += b
                count++
            }
        }

        if (count > 0) {
            val avgR = sumR.toFloat() / count
            val avgG = sumG.toFloat() / count
            val avgB = sumB.toFloat() / count
            val avgGray = (avgR + avgG + avgB) / 3f

            val rScale = avgGray / avgR.coerceAtLeast(1f)
            val gScale = avgGray / avgG.coerceAtLeast(1f)
            val bScale = avgGray / avgB.coerceAtLeast(1f)

            for (i in pixels.indices) {
                val c = pixels[i]
                val a = c ushr 24 and 0xFF
                val r = (((c ushr 16) and 0xFF) * rScale).toInt().coerceIn(0, 255)
                val g = (((c ushr 8) and 0xFF) * gScale).toInt().coerceIn(0, 255)
                val b = ((c and 0xFF) * bScale).toInt().coerceIn(0, 255)
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            toned.setPixels(pixels, 0, w, 0, 0, w, h)
        }
        return toned
    }

    // --- 2. Channel Mixer ---

    fun applyChannelMixer(source: Bitmap, config: ChannelMixerConfig): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val inR = ((c ushr 16) and 0xFF).toFloat()
            val inG = ((c ushr 8) and 0xFF).toFloat()
            val inB = (c and 0xFF).toFloat()

            if (config.isMonochrome) {
                val mono = (inR * (config.monoR / 100f) + inG * (config.monoG / 100f) + inB * (config.monoB / 100f) + config.monoConst * 2.55f).toInt().coerceIn(0, 255)
                pixels[i] = (a shl 24) or (mono shl 16) or (mono shl 8) or mono
            } else {
                val outR = (inR * (config.redChannelR / 100f) + inG * (config.redChannelG / 100f) + inB * (config.redChannelB / 100f) + config.redConst * 2.55f).toInt().coerceIn(0, 255)
                val outG = (inR * (config.greenChannelR / 100f) + inG * (config.greenChannelG / 100f) + inB * (config.greenChannelB / 100f) + config.greenConst * 2.55f).toInt().coerceIn(0, 255)
                val outB = (inR * (config.blueChannelR / 100f) + inG * (config.blueChannelG / 100f) + inB * (config.blueChannelB / 100f) + config.blueConst * 2.55f).toInt().coerceIn(0, 255)
                pixels[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // --- 3. Color Lookup (3D LUT) ---

    fun applyColorLookup(source: Bitmap, lut: Lut3D): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val r = ((c ushr 16) and 0xFF) / 255f
            val g = ((c ushr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f

            val lutSample = lut.sample(r, g, b)
            val oR = (lutSample[0] * 255f).toInt()
            val oG = (lutSample[1] * 255f).toInt()
            val oB = (lutSample[2] * 255f).toInt()

            pixels[i] = (a shl 24) or (oR shl 16) or (oG shl 8) or oB
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    // --- 4. Split Toning ---

    fun applySplitToning(
        source: Bitmap,
        shadowHue: Float, shadowSat: Float,
        highlightHue: Float, highlightSat: Float,
        balance: Float = 0f // -100 .. +100
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val sCol = Color.HSVToColor(floatArrayOf(shadowHue, shadowSat / 100f, 1f))
        val sR = ((sCol ushr 16) and 0xFF) / 255f
        val sG = ((sCol ushr 8) and 0xFF) / 255f
        val sB = (sCol and 0xFF) / 255f

        val hCol = Color.HSVToColor(floatArrayOf(highlightHue, highlightSat / 100f, 1f))
        val hR = ((hCol ushr 16) and 0xFF) / 255f
        val hG = ((hCol ushr 8) and 0xFF) / 255f
        val hB = (hCol and 0xFF) / 255f

        val crossover = 0.5f + (balance / 100f) * 0.25f

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            var r = ((c ushr 16) and 0xFF) / 255f
            var g = ((c ushr 8) and 0xFF) / 255f
            var b = (c and 0xFF) / 255f

            val luma = 0.299f * r + 0.587f * g + 0.114f * b

            if (luma < crossover && shadowSat > 0f) {
                val weight = (1f - luma / crossover).coerceIn(0f, 1f) * (shadowSat / 100f) * 0.4f
                r = r * (1f - weight) + sR * luma * weight
                g = g * (1f - weight) + sG * luma * weight
                b = b * (1f - weight) + sB * luma * weight
            } else if (luma >= crossover && highlightSat > 0f) {
                val weight = ((luma - crossover) / (1f - crossover)).coerceIn(0f, 1f) * (highlightSat / 100f) * 0.4f
                r = r * (1f - weight) + hR * luma * weight
                g = g * (1f - weight) + hG * luma * weight
                b = b * (1f - weight) + hB * luma * weight
            }

            pixels[i] = (a shl 24) or ((r.coerceIn(0f, 1f) * 255).toInt() shl 16) or ((g.coerceIn(0f, 1f) * 255).toInt() shl 8) or (b.coerceIn(0f, 1f) * 255).toInt()
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }
}
