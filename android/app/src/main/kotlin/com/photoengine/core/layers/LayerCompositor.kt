package com.photoengine.core.layers

import android.graphics.Bitmap
import kotlin.math.*

enum class BlendMode(val id: Int) {
    NORMAL(0),
    MULTIPLY(1),
    SCREEN(2),
    OVERLAY(3),
    SOFT_LIGHT(4),
    HARD_LIGHT(5),
    COLOR_DODGE(6),
    COLOR_BURN(7),
    DIFFERENCE(8),
    EXCLUSION(9),
    HUE(10),
    SATURATION(11),
    COLOR(12),
    LUMINOSITY(13),
    LINEAR_DODGE_ADD(14)
}

data class Layer(
    val id: String,
    val name: String,
    var bitmap: Bitmap,
    var opacity: Float = 1.0f,
    var blendMode: BlendMode = BlendMode.NORMAL,
    var isVisible: Boolean = true,
    var maskBitmap: Bitmap? = null
)

/**
 * Multi-Layer Professional Compositor.
 * Executes industry-standard Photoshop/Porter-Duff blending algorithms.
 * Direct GPU compute implementation available in `blend_modes.comp`.
 */
class LayerCompositor {

    fun blendPixel(
        baseR: Float, baseG: Float, baseB: Float,
        blendR: Float, blendG: Float, blendB: Float,
        mode: BlendMode
    ): FloatArray {
        var r = blendR
        var g = blendG
        var b = blendB

        when (mode) {
            BlendMode.NORMAL -> {
                r = blendR; g = blendG; b = blendB
            }
            BlendMode.MULTIPLY -> {
                r = baseR * blendR
                g = baseG * blendG
                b = baseB * blendB
            }
            BlendMode.SCREEN -> {
                r = 1.0f - (1.0f - baseR) * (1.0f - blendR)
                g = 1.0f - (1.0f - baseG) * (1.0f - blendG)
                b = 1.0f - (1.0f - baseB) * (1.0f - blendB)
            }
            BlendMode.OVERLAY -> {
                r = if (baseR < 0.5f) 2.0f * baseR * blendR else 1.0f - 2.0f * (1.0f - baseR) * (1.0f - blendR)
                g = if (baseG < 0.5f) 2.0f * baseG * blendG else 1.0f - 2.0f * (1.0f - baseG) * (1.0f - blendG)
                b = if (baseB < 0.5f) 2.0f * baseB * blendB else 1.0f - 2.0f * (1.0f - baseB) * (1.0f - blendB)
            }
            BlendMode.SOFT_LIGHT -> {
                fun softLightChannel(cb: Float, cs: Float): Float {
                    val d = if (cb <= 0.25f) ((16.0f * cb - 12.0f) * cb + 4.0f) * cb else sqrt(cb)
                    return if (cs <= 0.5f) cb - (1.0f - 2.0f * cs) * cb * (1.0f - cb) else cb + (2.0f * cs - 1.0f) * (d - cb)
                }
                r = softLightChannel(baseR, blendR)
                g = softLightChannel(baseG, blendG)
                b = softLightChannel(baseB, blendB)
            }
            BlendMode.HARD_LIGHT -> {
                r = if (blendR < 0.5f) 2.0f * baseR * blendR else 1.0f - 2.0f * (1.0f - baseR) * (1.0f - blendR)
                g = if (blendG < 0.5f) 2.0f * baseG * blendG else 1.0f - 2.0f * (1.0f - baseG) * (1.0f - blendG)
                b = if (blendB < 0.5f) 2.0f * baseB * blendB else 1.0f - 2.0f * (1.0f - baseB) * (1.0f - blendB)
            }
            BlendMode.COLOR_DODGE -> {
                r = if (blendR >= 1.0f) 1.0f else (baseR / (1.0f - blendR)).coerceIn(0.0f, 1.0f)
                g = if (blendG >= 1.0f) 1.0f else (baseG / (1.0f - blendG)).coerceIn(0.0f, 1.0f)
                b = if (blendB >= 1.0f) 1.0f else (baseB / (1.0f - blendB)).coerceIn(0.0f, 1.0f)
            }
            BlendMode.COLOR_BURN -> {
                r = if (blendR <= 0.0f) 0.0f else (1.0f - (1.0f - baseR) / blendR).coerceIn(0.0f, 1.0f)
                g = if (blendG <= 0.0f) 0.0f else (1.0f - (1.0f - baseG) / blendG).coerceIn(0.0f, 1.0f)
                b = if (blendB <= 0.0f) 0.0f else (1.0f - (1.0f - baseB) / blendB).coerceIn(0.0f, 1.0f)
            }
            BlendMode.DIFFERENCE -> {
                r = abs(baseR - blendR)
                g = abs(baseG - blendG)
                b = abs(baseB - blendB)
            }
            BlendMode.EXCLUSION -> {
                r = baseR + blendR - 2.0f * baseR * blendR
                g = baseG + blendG - 2.0f * baseG * blendG
                b = baseB + blendB - 2.0f * baseB * blendB
            }
            BlendMode.LINEAR_DODGE_ADD -> {
                r = (baseR + blendR).coerceIn(0.0f, 1.0f)
                g = (baseG + blendG).coerceIn(0.0f, 1.0f)
                b = (baseB + blendB).coerceIn(0.0f, 1.0f)
            }
            BlendMode.LUMINOSITY -> {
                val blendLum = 0.2126f * blendR + 0.7152f * blendG + 0.0722f * blendB
                val baseLum = 0.2126f * baseR + 0.7152f * baseG + 0.0722f * baseB
                val delta = blendLum - baseLum
                r = (baseR + delta).coerceIn(0.0f, 1.0f)
                g = (baseG + delta).coerceIn(0.0f, 1.0f)
                b = (baseB + delta).coerceIn(0.0f, 1.0f)
            }
            else -> {
                r = blendR; g = blendG; b = blendB
            }
        }

        return floatArrayOf(r, g, b)
    }
}
