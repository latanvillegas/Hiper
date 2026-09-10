package com.photoengine.core.effects

import android.graphics.Bitmap
import kotlin.math.*

enum class DoubleExposureBlendMode {
    MULTIPLY,
    SCREEN,
    OVERLAY,
    SOFT_LIGHT,
    HARD_LIGHT,
    COLOR_DODGE,
    COLOR_BURN,
    ADD,
    LIGHTEN,
    DARKEN,
    DIFFERENCE
}

data class DoubleExposureConfig(
    val blendMode: DoubleExposureBlendMode = DoubleExposureBlendMode.SCREEN,
    val opacity: Float = 0.8f,          // 0.0 .. 1.0 (0 to 100%)
    val invertMask: Boolean = false
)

/**
 * Professional Double Exposure Engine (Snapseed 2026 Double Exposure specification).
 * High precision photorealistic blending with GPU-compliant mathematical formulations,
 * per-pixel opacity modulation and alpha masking.
 */
class DoubleExposureEngine {

    /**
     * Blends a base image with a secondary image according to the specified blend mode and opacity.
     * Optionally modulates by a grayscale/alpha mask.
     */
    fun blendImages(
        baseBitmap: Bitmap,
        secondaryBitmap: Bitmap,
        config: DoubleExposureConfig,
        maskBitmap: Bitmap? = null
    ): Bitmap {
        val w = baseBitmap.width
        val h = baseBitmap.height

        // Ensure secondary image matches target dimensions
        val scaledSecondary = if (secondaryBitmap.width != w || secondaryBitmap.height != h) {
            Bitmap.createScaledBitmap(secondaryBitmap, w, h, true)
        } else {
            secondaryBitmap
        }

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val basePixels = IntArray(w * h)
        val secPixels = IntArray(w * h)
        val maskPixels = if (maskBitmap != null) IntArray(w * h) else null

        baseBitmap.getPixels(basePixels, 0, w, 0, 0, w, h)
        scaledSecondary.getPixels(secPixels, 0, w, 0, 0, w, h)

        if (maskBitmap != null) {
            val scaledMask = if (maskBitmap.width != w || maskBitmap.height != h) {
                Bitmap.createScaledBitmap(maskBitmap, w, h, true)
            } else {
                maskBitmap
            }
            scaledMask.getPixels(maskPixels!!, 0, w, 0, 0, w, h)
        }

        for (i in basePixels.indices) {
            val cb = basePixels[i]
            val cs = secPixels[i]

            val br = ((cb shr 16) and 0xFF) / 255.0f
            val bg = ((cb shr 8) and 0xFF) / 255.0f
            val bb = (cb and 0xFF) / 255.0f

            val sr = ((cs shr 16) and 0xFF) / 255.0f
            val sg = ((cs shr 8) and 0xFF) / 255.0f
            val sb = (cs and 0xFF) / 255.0f

            val blendedR = blendComponent(br, sr, config.blendMode)
            val blendedG = blendComponent(bg, sg, config.blendMode)
            val blendedB = blendComponent(bb, sb, config.blendMode)

            // Evaluate per-pixel mask weight
            var maskWeight = 1.0f
            if (maskPixels != null) {
                val mc = maskPixels[i]
                val mlum = (((mc shr 16) and 0xFF) * 0.2126f + ((mc shr 8) and 0xFF) * 0.7152f + (mc and 0xFF) * 0.0722f) / 255.0f
                maskWeight = if (config.invertMask) (1.0f - mlum) else mlum
            }

            val effectiveAlpha = (config.opacity * maskWeight).coerceIn(0.0f, 1.0f)

            val finalR = (br * (1.0f - effectiveAlpha) + blendedR * effectiveAlpha).coerceIn(0.0f, 1.0f)
            val finalG = (bg * (1.0f - effectiveAlpha) + blendedG * effectiveAlpha).coerceIn(0.0f, 1.0f)
            val finalB = (bb * (1.0f - effectiveAlpha) + blendedB * effectiveAlpha).coerceIn(0.0f, 1.0f)

            val outR = (finalR * 255.0f).toInt()
            val outG = (finalG * 255.0f).toInt()
            val outB = (finalB * 255.0f).toInt()

            basePixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(basePixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * GLSL-equivalent blend mode math per color component [0..1].
     */
    fun blendComponent(b: Float, s: Float, mode: DoubleExposureBlendMode): Float = when (mode) {
        DoubleExposureBlendMode.MULTIPLY -> b * s
        DoubleExposureBlendMode.SCREEN -> 1.0f - (1.0f - b) * (1.0f - s)
        DoubleExposureBlendMode.OVERLAY -> if (b <= 0.5f) 2.0f * b * s else 1.0f - 2.0f * (1.0f - b) * (1.0f - s)
        DoubleExposureBlendMode.SOFT_LIGHT -> {
            if (s <= 0.5f) {
                b - (1.0f - 2.0f * s) * b * (1.0f - b)
            } else {
                val d = if (b <= 0.25f) ((16.0f * b - 12.0f) * b + 4.0f) * b else sqrt(b)
                b + (2.0f * s - 1.0f) * (d - b)
            }
        }
        DoubleExposureBlendMode.HARD_LIGHT -> if (s <= 0.5f) 2.0f * b * s else 1.0f - 2.0f * (1.0f - b) * (1.0f - s)
        DoubleExposureBlendMode.COLOR_DODGE -> if (s >= 1.0f) 1.0f else (b / (1.0f - s).coerceAtLeast(0.0001f)).coerceAtMost(1.0f)
        DoubleExposureBlendMode.COLOR_BURN -> if (s <= 0.0f) 0.0f else (1.0f - (1.0f - b) / s.coerceAtLeast(0.0001f)).coerceAtLeast(0.0f)
        DoubleExposureBlendMode.ADD -> (b + s).coerceAtMost(1.0f)
        DoubleExposureBlendMode.LIGHTEN -> max(b, s)
        DoubleExposureBlendMode.DARKEN -> min(b, s)
        DoubleExposureBlendMode.DIFFERENCE -> abs(b - s)
    }
}
