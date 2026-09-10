package com.photoengine.core.effects

import android.graphics.Bitmap
import kotlin.math.*

data class VignetteConfig(
    val outerBrightness: Float = -45.0f, // -100 .. +100 (darkening or white halo)
    val innerBrightness: Float = 10.0f,  // -100 .. +100 (center exposure boost)
    val size: Float = 60.0f,             // 1 .. 100 (radius of inner core)
    val roundness: Float = 0.0f,         // -100 (rectangle/aspect-bound) .. +100 (perfect circle)
    val centerX: Float = 0.5f,           // 0.0 .. 1.0 (normalized custom center)
    val centerY: Float = 0.5f            // 0.0 .. 1.0 (normalized custom center)
)

/**
 * Snapseed 2026 Professional Vignette Engine.
 * Features:
 * - Movable focal center
 * - Independent outer edge darkening/lightening and inner core illumination
 * - Adjustable size and continuously variable aspect-to-circle roundness interpolation
 * - Smooth cubic Hermite falloff
 */
class VignetteEngine {

    fun processVignette(bitmap: Bitmap, config: VignetteConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val cx = config.centerX * w
        val cy = config.centerY * h

        val aspect = w.toFloat() / h.toFloat()
        // roundness: -100 uses screen aspect ratio, +100 is perfectly circular (aspect = 1.0)
        val roundnessT = ((config.roundness + 100.0f) / 200.0f).coerceIn(0.0f, 1.0f)
        val effectiveAspectX = 1.0f * (1.0f - roundnessT) + aspect * roundnessT

        val maxRadius = sqrt((w * 0.5f).pow(2) + (h * 0.5f).pow(2))
        val innerRadius = maxRadius * (config.size / 100.0f) * 0.75f
        val outerRadius = maxRadius * 1.05f

        val outerGain = (config.outerBrightness / 100.0f) * 0.75f
        val innerGain = (config.innerBrightness / 100.0f) * 0.50f

        for (y in 0 until h) {
            val dy = y - cy
            for (x in 0 until w) {
                val dx = (x - cx) * effectiveAspectX
                val dist = sqrt(dx * dx + dy * dy)

                // Normalized falloff factor [0.0 = center, 1.0 = outer border]
                val t = if (dist <= innerRadius) {
                    0.0f
                } else if (dist >= outerRadius) {
                    1.0f
                } else {
                    val norm = (dist - innerRadius) / (outerRadius - innerRadius)
                    norm * norm * (3.0f - 2.0f * norm) // Smoothstep
                }

                // Interpolate exposure modifier between innerGain and outerGain
                val gain = innerGain * (1.0f - t) + outerGain * t

                val idx = y * w + x
                val c = pixels[idx]
                var r = ((c shr 16) and 0xFF) / 255.0f
                var g = ((c shr 8) and 0xFF) / 255.0f
                var b = (c and 0xFF) / 255.0f

                if (gain >= 0.0f) {
                    // Lighten / add
                    r = (r + gain * (1.0f - r)).coerceIn(0f, 1f)
                    g = (g + gain * (1.0f - g)).coerceIn(0f, 1f)
                    b = (b + gain * (1.0f - b)).coerceIn(0f, 1f)
                } else {
                    // Darken / multiply
                    val factor = 1.0f + gain
                    r = (r * factor).coerceIn(0f, 1f)
                    g = (g * factor).coerceIn(0f, 1f)
                    b = (b * factor).coerceIn(0f, 1f)
                }

                val outR = (r * 255.0f).toInt()
                val outG = (g * 255.0f).toInt()
                val outB = (b * 255.0f).toInt()
                pixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }
}
