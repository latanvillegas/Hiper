package com.photoengine.core.color

import kotlin.math.*

enum class HslChannel {
    RED,        // Center: 0° (or 360°)
    YELLOW,     // Center: 60°
    GREEN,      // Center: 120°
    CYAN,       // Center: 180°
    BLUE,       // Center: 240°
    MAGENTA,    // Center: 300°
    SHADOWS,    // Tonal zone: Luma < 0.35
    HIGHLIGHTS  // Tonal zone: Luma > 0.65
}

data class HslAdjustment(
    val hueShift: Float = 0.0f,       // -180.0° .. +180.0°
    val saturation: Float = 0.0f,     // -1.0 .. +1.0 (-100% to +100%)
    val luminance: Float = 0.0f       // -1.0 .. +1.0 (-100% to +100%)
)

/**
 * Professional 8-Channel HSL Engine (Red, Yellow, Green, Cyan, Blue, Magenta, Shadows, Highlights).
 * Employs continuous Gaussian/Cosine angular falloff to prevent banding and hard color splits.
 * Can be serialized into uniform buffers for direct GPU fragment/compute execution.
 */
class Hsl8ChannelEngine {

    private val adjustments = mutableMapOf<HslChannel, HslAdjustment>().apply {
        HslChannel.values().forEach { put(it, HslAdjustment()) }
    }

    fun setAdjustment(channel: HslChannel, adjustment: HslAdjustment) {
        adjustments[channel] = adjustment
    }

    fun getAdjustment(channel: HslChannel): HslAdjustment =
        adjustments[channel] ?: HslAdjustment()

    fun reset() {
        HslChannel.values().forEach { adjustments[it] = HslAdjustment() }
    }

    /**
     * Converts adjustments into a FloatArray of 8 * 4 values (HueShift, Sat, Lum, Weight)
     * suitable for uniform buffer binding in Vulkan/OpenGL.
     */
    fun getUniformData(): FloatArray {
        val data = FloatArray(8 * 4)
        val channels = listOf(
            HslChannel.RED, HslChannel.YELLOW, HslChannel.GREEN,
            HslChannel.CYAN, HslChannel.BLUE, HslChannel.MAGENTA,
            HslChannel.SHADOWS, HslChannel.HIGHLIGHTS
        )
        for (i in channels.indices) {
            val adj = adjustments[channels[i]] ?: HslAdjustment()
            data[i * 4 + 0] = adj.hueShift / 360.0f // normalized
            data[i * 4 + 1] = adj.saturation
            data[i * 4 + 2] = adj.luminance
            data[i * 4 + 3] = 0.0f // padding / alignment
        }
        return data
    }

    /**
     * Applies the 8-channel HSL transformation on a normalized RGB pixel [0..1].
     */
    fun processPixel(r: Float, g: Float, b: Float): FloatArray {
        // RGB to HSL
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val luma = (maxC + minC) * 0.5f

        var h = 0.0f
        var s = 0.0f
        val l = luma

        if (delta > 0.00001f) {
            s = if (l <= 0.5f) delta / (maxC + minC) else delta / (2.0f - maxC - minC)
            h = when (maxC) {
                r -> ((g - b) / delta) % 6.0f
                g -> ((b - r) / delta) + 2.0f
                else -> ((r - g) / delta) + 4.0f
            } * 60.0f
            if (h < 0.0f) h += 360.0f
        }

        // Calculate channel weights
        var totalHueShift = 0.0f
        var totalSatMod = 0.0f
        var totalLumMod = 0.0f

        // Chromatic weights based on Hue angle
        val hueCenters = mapOf(
            HslChannel.RED to 0.0f,
            HslChannel.YELLOW to 60.0f,
            HslChannel.GREEN to 120.0f,
            HslChannel.CYAN to 180.0f,
            HslChannel.BLUE to 240.0f,
            HslChannel.MAGENTA to 300.0f
        )

        for ((chan, center) in hueCenters) {
            val diff = abs(((h - center + 180.0f) % 360.0f) - 180.0f)
            // Gaussian bell curve with standard deviation of ~30 degrees
            val weight = exp(-(diff * diff) / (2.0f * 25.0f * 25.0f))
            val adj = adjustments[chan] ?: continue

            totalHueShift += adj.hueShift * weight
            totalSatMod += adj.saturation * weight
            totalLumMod += adj.luminance * weight
        }

        // Shadows weight (smooth step around 0.35 luma)
        val shadowWeight = (1.0f - smoothstep(0.05f, 0.45f, l))
        val shadowAdj = adjustments[HslChannel.SHADOWS] ?: HslAdjustment()
        totalHueShift += shadowAdj.hueShift * shadowWeight
        totalSatMod += shadowAdj.saturation * shadowWeight
        totalLumMod += shadowAdj.luminance * shadowWeight

        // Highlights weight (smooth step around 0.65 luma)
        val highlightWeight = smoothstep(0.55f, 0.95f, l)
        val highlightAdj = adjustments[HslChannel.HIGHLIGHTS] ?: HslAdjustment()
        totalHueShift += highlightAdj.hueShift * highlightWeight
        totalSatMod += highlightAdj.saturation * highlightWeight
        totalLumMod += highlightAdj.luminance * highlightWeight

        // Apply modifications
        var newH = (h + totalHueShift) % 360.0f
        if (newH < 0.0f) newH += 360.0f
        val newS = (s * (1.0f + totalSatMod)).coerceIn(0.0f, 1.0f)
        val newL = (l + totalLumMod * 0.5f).coerceIn(0.0f, 1.0f)

        return hslToRgb(newH, newS, newL)
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0f, 1.0f)
        return t * t * (3.0f - 2.0f * t)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
        val c = (1.0f - abs(2.0f * l - 1.0f)) * s
        val x = c * (1.0f - abs((h / 60.0f) % 2.0f - 1.0f))
        val m = l - c * 0.5f

        val (rPrime, gPrime, bPrime) = when ((h / 60.0f).toInt()) {
            0 -> Triple(c, x, 0.0f)
            1 -> Triple(x, c, 0.0f)
            2 -> Triple(0.0f, c, x)
            3 -> Triple(0.0f, x, c)
            4 -> Triple(x, 0.0f, c)
            else -> Triple(c, 0.0f, x)
        }

        return floatArrayOf(
            (rPrime + m).coerceIn(0.0f, 1.0f),
            (gPrime + m).coerceIn(0.0f, 1.0f),
            (bPrime + m).coerceIn(0.0f, 1.0f)
        )
    }
}
