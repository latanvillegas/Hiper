package com.photoengine.core.color

import android.graphics.Bitmap
import kotlin.math.*

enum class BwColorFilter(val filterName: String, val rWeight: Float, val gWeight: Float, val bWeight: Float) {
    NEUTRAL("Neutral", 0.299f, 0.587f, 0.114f),
    RED("Red (Dramatic Sky & Contrast)", 0.90f, 0.08f, 0.02f),
    ORANGE("Orange (Skin Smoothing & Clouds)", 0.70f, 0.26f, 0.04f),
    YELLOW("Yellow (Natural Landscapes)", 0.45f, 0.50f, 0.05f),
    GREEN("Green (Foliage & Tonal Range)", 0.15f, 0.75f, 0.10f),
    BLUE("Blue (Haze & Atmospheric)", 0.05f, 0.15f, 0.80f)
}

enum class BwToning(val toningName: String) {
    NONE("Pure Monochrome"),
    SEPIA("Sepia Tone"),
    PLATINUM("Platinum Warm Silver"),
    CYANOTYPE("Cyanotype Classic Blue")
}

data class BlackAndWhiteConfig(
    val filter: BwColorFilter = BwColorFilter.NEUTRAL,
    val brightness: Float = 0.0f,     // -100 .. +100
    val contrast: Float = 25.0f,      // -100 .. +100
    val grain: Float = 15.0f,         // 0 .. 100
    val toning: BwToning = BwToning.NONE,
    val toningStrength: Float = 40.0f // 0 .. 100
)

/**
 * Professional Black and White Engine (Snapseed 2026 B&W Tool specification).
 * Implements physical optical colored filters, fine-grained contrast curve adjustment,
 * photographic silver halide film grain, and chemical toning (Sepia, Platinum, Cyanotype).
 */
class BlackAndWhiteEngine {

    fun processBw(bitmap: Bitmap, config: BlackAndWhiteConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val rw = config.filter.rWeight
        val gw = config.filter.gWeight
        val bw = config.filter.bWeight

        val bOffset = (config.brightness / 100.0f) * 0.35f
        val cFactor = 1.0f + (config.contrast / 100.0f)
        val grainAmt = (config.grain / 100.0f) * 0.25f
        val toneStrength = (config.toningStrength / 100.0f)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]

                val r = ((c shr 16) and 0xFF) / 255.0f
                val g = ((c shr 8) and 0xFF) / 255.0f
                val b = (c and 0xFF) / 255.0f

                // 1. Spectral filter weighting
                var mono = (r * rw + g * gw + b * bw) / (rw + gw + bw)

                // 2. Brightness & Contrast
                mono += bOffset
                mono = 0.5f + (mono - 0.5f) * cFactor

                // 3. Film Grain
                if (grainAmt > 0.001f) {
                    val noiseSeed = (x * 12.9898f + y * 78.233f)
                    val rand = ((sin(noiseSeed.toDouble()) * 43758.5453).toFloat() % 1.0f) - 0.5f
                    val midtoneMask = 4.0f * mono.coerceIn(0f, 1f) * (1.0f - mono.coerceIn(0f, 1f))
                    mono += rand * grainAmt * midtoneMask
                }

                mono = mono.coerceIn(0.0f, 1.0f)

                // 4. Chemical Toning
                var outR = mono
                var outG = mono
                var outB = mono

                when (config.toning) {
                    BwToning.NONE -> { /* Neutral gray */ }
                    BwToning.SEPIA -> {
                        val sepR = mono * 1.15f + 0.05f
                        val sepG = mono * 0.95f + 0.02f
                        val sepB = mono * 0.75f
                        outR = mono * (1.0f - toneStrength) + sepR * toneStrength
                        outG = mono * (1.0f - toneStrength) + sepG * toneStrength
                        outB = mono * (1.0f - toneStrength) + sepB * toneStrength
                    }
                    BwToning.PLATINUM -> {
                        val plR = mono * 1.04f + 0.02f
                        val plG = mono * 1.02f + 0.01f
                        val plB = mono * 0.98f
                        outR = mono * (1.0f - toneStrength) + plR * toneStrength
                        outG = mono * (1.0f - toneStrength) + plG * toneStrength
                        outB = mono * (1.0f - toneStrength) + plB * toneStrength
                    }
                    BwToning.CYANOTYPE -> {
                        val cyR = mono * 0.70f
                        val cyG = mono * 0.88f + 0.03f
                        val cyB = mono * 1.18f + 0.06f
                        outR = mono * (1.0f - toneStrength) + cyR * toneStrength
                        outG = mono * (1.0f - toneStrength) + cyG * toneStrength
                        outB = mono * (1.0f - toneStrength) + cyB * toneStrength
                    }
                }

                val finalR = (outR.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val finalG = (outG.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val finalB = (outB.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                pixels[idx] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }
}
