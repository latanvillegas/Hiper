package com.photoengine.core.effects

import android.graphics.Bitmap
import kotlin.math.*

data class DehazeConfig(
    val amount: Float = 40.0f,            // -100 (artistic fog addition) .. +100 (full dehaze penetration)
    val distanceContrast: Float = 25.0f,  // 0 .. 100 (boost contrast in distant hazy background)
    val atmosphericColorR: Float = 0.85f, // Atmospheric light vector
    val atmosphericColorG: Float = 0.88f,
    val atmosphericColorB: Float = 0.92f
)

/**
 * Snapseed 2026 Professional Dehaze & Atmospheric Scattering Engine.
 * Formulated on the physical optical radiative transfer model (Dark Channel Prior).
 * Penetrates dense mist, smog, smoke, and water haze, or inverts to synthesize atmospheric mist.
 */
class DehazeAtmosphericEngine {

    fun processDehaze(bitmap: Bitmap, config: DehazeConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val amount = config.amount / 100.0f // -1.0 .. +1.0
        val distContrast = 1.0f + (config.distanceContrast / 100.0f) * 0.5f

        val ar = config.atmosphericColorR
        val ag = config.atmosphericColorG
        val ab = config.atmosphericColorB

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val c = pixels[idx]

                val r = ((c shr 16) and 0xFF) / 255.0f
                val g = ((c shr 8) and 0xFF) / 255.0f
                val b = (c and 0xFF) / 255.0f

                // 1. Dark Channel estimation (minimum across color channels normalized by atmospheric airlight)
                val darkChannel = min(r / ar, min(g / ag, b / ab))

                // 2. Transmission map t(x) = 1 - omega * darkChannel
                val omega = 0.85f
                val rawTransmission = (1.0f - omega * darkChannel).coerceIn(0.1f, 1.0f)

                var finalR: Float
                var finalG: Float
                var finalB: Float

                if (amount >= 0.0f) {
                    // Dehaze mode: recover haze-free radiance J = (I - A)/max(t, t0) + A
                    val effectiveT = (rawTransmission.pow(amount)).coerceAtLeast(0.12f)
                    var jr = (r - ar) / effectiveT + ar
                    var jg = (g - ag) / effectiveT + ag
                    var jb = (b - ab) / effectiveT + ab

                    // Distant region contrast enhancement
                    if (rawTransmission < 0.65f) {
                        val hazeDepth = (0.65f - rawTransmission) / 0.65f
                        val contrastFactor = 1.0f + (distContrast - 1.0f) * hazeDepth
                        jr = 0.5f + (jr - 0.5f) * contrastFactor
                        jg = 0.5f + (jg - 0.5f) * contrastFactor
                        jb = 0.5f + (jb - 0.5f) * contrastFactor
                    }

                    finalR = r * (1.0f - amount) + jr * amount
                    finalG = g * (1.0f - amount) + jg * amount
                    finalB = b * (1.0f - amount) + jb * amount
                } else {
                    // Inverted mode: Add artistic atmospheric mist / fog
                    val mistStrength = -amount
                    val mistTransmission = (1.0f - mistStrength * (1.0f - rawTransmission * 0.5f)).coerceIn(0.05f, 1.0f)
                    val fogR = r * mistTransmission + ar * (1.0f - mistTransmission)
                    val fogG = g * mistTransmission + ag * (1.0f - mistTransmission)
                    val fogB = b * mistTransmission + ab * (1.0f - mistTransmission)

                    finalR = fogR
                    finalG = fogG
                    finalB = fogB
                }

                val outR = (finalR.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val outG = (finalG.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                val outB = (finalB.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                pixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }
}
