package com.photoengine.core.face

import android.graphics.Bitmap
import kotlin.math.*

data class BilateralSkinConfig(
    val spatialSigma: Float = 5.0f,     // Distance weight (3.0 .. 12.0)
    val rangeSigma: Float = 0.12f,      // Color range sensitivity (0.05 .. 0.3)
    val texturePreservation: Float = 0.85f, // Frequency separation high-pass pore retention
    val skinMaskFeather: Float = 0.6f
)

/**
 * High-performance Bilateral Filter for Skin Retouching with Frequency Separation.
 * Preserves pores, eyelash sharpness, and facial contours while smoothing skin tone blotchiness.
 */
class BilateralFilterEngine {

    fun getUniformPayload(config: BilateralSkinConfig): FloatArray {
        return floatArrayOf(
            config.spatialSigma,
            config.rangeSigma,
            config.texturePreservation,
            config.skinMaskFeather,
            0.0f, 0.0f, 0.0f, 0.0f
        )
    }

    /**
     * Executes bilateral filter on a CPU image or fallback pipeline.
     * On Vulkan GPU, the `bilateral_skin.comp` shader completes in ~4ms.
     */
    fun processBilateral(
        input: Bitmap,
        skinMask: Bitmap?,
        config: BilateralSkinConfig
    ): Bitmap {
        val w = input.width
        val h = input.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(w * h)
        val dstPixels = IntArray(w * h)
        val maskPixels = if (skinMask != null) IntArray(w * h) else null

        input.getPixels(srcPixels, 0, w, 0, 0, w, h)
        skinMask?.getPixels(maskPixels!!, 0, w, 0, 0, w, h)

        val radius = min(12, (config.spatialSigma * 2.0f).toInt())
        val spatialCoeff = -0.5f / (config.spatialSigma * config.spatialSigma)
        val rangeCoeff = -0.5f / (config.rangeSigma * config.rangeSigma * 255.0f * 255.0f)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val centerColor = srcPixels[idx]
                val maskWeight = if (maskPixels != null) {
                    ((maskPixels[idx] shr 24) and 0xFF) / 255.0f
                } else 1.0f

                if (maskWeight < 0.05f) {
                    dstPixels[idx] = centerColor
                    continue
                }

                val cr = (centerColor shr 16) and 0xFF
                val cg = (centerColor shr 8) and 0xFF
                val cb = centerColor and 0xFF
                val cLum = 0.299f * cr + 0.587f * cg + 0.114f * cb

                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumW = 0.0

                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val dy2 = (dy * dy).toFloat()

                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, w - 1)
                        val dist2 = dx * dx + dy2

                        val neighbor = srcPixels[ny * w + nx]
                        val nr = (neighbor shr 16) and 0xFF
                        val ng = (neighbor shr 8) and 0xFF
                        val nb = neighbor and 0xFF
                        val nLum = 0.299f * nr + 0.587f * ng + 0.114f * nb

                        val lumDiff = nLum - cLum
                        val weight = exp(dist2 * spatialCoeff + lumDiff * lumDiff * rangeCoeff)

                        sumR += nr * weight
                        sumG += ng * weight
                        sumB += nb * weight
                        sumW += weight
                    }
                }

                val smoothR = (sumR / sumW).toFloat()
                val smoothG = (sumG / sumW).toFloat()
                val smoothB = (sumB / sumW).toFloat()

                // Frequency separation high-frequency detail: HighPass = Original - LowPass
                val hpR = cr - smoothR
                val hpG = cg - smoothG
                val hpB = cb - smoothB

                // Final pixel blends smoothed low-frequency with preserved high-pass texture
                val textureFactor = config.texturePreservation
                val finalR = (smoothR + hpR * textureFactor).coerceIn(0.0f, 255.0f)
                val finalG = (smoothG + hpG * textureFactor).coerceIn(0.0f, 255.0f)
                val finalB = (smoothB + hpB * textureFactor).coerceIn(0.0f, 255.0f)

                // Blend with original according to skin mask
                val outR = (cr * (1.0f - maskWeight) + finalR * maskWeight).toInt()
                val outG = (cg * (1.0f - maskWeight) + finalG * maskWeight).toInt()
                val outB = (cb * (1.0f - maskWeight) + finalB * maskWeight).toInt()

                dstPixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        output.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return output
    }
}
