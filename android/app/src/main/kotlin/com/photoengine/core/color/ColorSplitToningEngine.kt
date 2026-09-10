package com.photoengine.core.color

import android.graphics.Bitmap
import kotlin.math.*

data class SplitToningConfig(
    val shadowHue: Float = 215.0f,       // 0 .. 360° (default cool teal/blue)
    val shadowSaturation: Float = 30.0f, // 0 .. 100%
    val highlightHue: Float = 40.0f,     // 0 .. 360° (default warm amber/gold)
    val highlightSaturation: Float = 25.0f,// 0 .. 100%
    val balance: Float = 0.0f            // -100 (favor shadows) .. +100 (favor highlights)
)

/**
 * Snapseed 2026 Professional Split Toning Engine.
 * Provides independent Hue & Saturation color grading for Shadows and Highlights
 * with a smoothly parameterized crossover threshold balance.
 */
class ColorSplitToningEngine {

    /**
     * Converts Hue (0-360), Saturation (0-1) to RGB tint factors.
     */
    private fun hslToRgb(h: Float, s: Float): FloatArray {
        val c = s
        val hp = (h % 360.0f) / 60.0f
        val x = c * (1.0f - abs((hp % 2.0f) - 1.0f))
        var r1 = 0f; var g1 = 0f; var b1 = 0f

        when {
            hp < 1.0f -> { r1 = c; g1 = x; b1 = 0f }
            hp < 2.0f -> { r1 = x; g1 = c; b1 = 0f }
            hp < 3.0f -> { r1 = 0f; g1 = c; b1 = x }
            hp < 4.0f -> { r1 = 0f; g1 = x; b1 = c }
            hp < 5.0f -> { r1 = x; g1 = 0f; b1 = c }
            else -> { r1 = c; g1 = 0f; b1 = x }
        }

        return floatArrayOf(r1, g1, b1)
    }

    fun processSplitToning(bitmap: Bitmap, config: SplitToningConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val shadowRgb = hslToRgb(config.shadowHue, config.shadowSaturation / 100.0f)
        val highlightRgb = hslToRgb(config.highlightHue, config.highlightSaturation / 100.0f)

        // Balance shifts crossover point between 0.2 and 0.8
        val balanceShift = (config.balance / 100.0f) * 0.30f
        val crossover = (0.50f + balanceShift).coerceIn(0.15f, 0.85f)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]

                var r = ((c shr 16) and 0xFF) / 255.0f
                var g = ((c shr 8) and 0xFF) / 255.0f
                var b = (c and 0xFF) / 255.0f

                // Rec.709 perceived luminance
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b

                // Shadow weight falls off above crossover
                val shadowWeight = if (luma < crossover) {
                    (1.0f - luma / crossover).pow(1.5f)
                } else 0.0f

                // Highlight weight increases above crossover
                val highlightWeight = if (luma > crossover) {
                    ((luma - crossover) / (1.0f - crossover)).pow(1.5f)
                } else 0.0f

                // Apply shadow tint
                if (shadowWeight > 0.001f && config.shadowSaturation > 0.01f) {
                    r += (shadowRgb[0] - 0.5f * shadowRgb[1] - 0.5f * shadowRgb[2]) * shadowWeight * 0.4f
                    g += (shadowRgb[1] - 0.5f * shadowRgb[0] - 0.5f * shadowRgb[2]) * shadowWeight * 0.4f
                    b += (shadowRgb[2] - 0.5f * shadowRgb[0] - 0.5f * shadowRgb[1]) * shadowWeight * 0.4f
                }

                // Apply highlight tint
                if (highlightWeight > 0.001f && config.highlightSaturation > 0.01f) {
                    r += (highlightRgb[0] - 0.5f * highlightRgb[1] - 0.5f * highlightRgb[2]) * highlightWeight * 0.4f
                    g += (highlightRgb[1] - 0.5f * highlightRgb[0] - 0.5f * highlightRgb[2]) * highlightWeight * 0.4f
                    b += (highlightRgb[2] - 0.5f * highlightRgb[0] - 0.5f * highlightRgb[1]) * highlightWeight * 0.4f
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
