package com.photoengine.core.effects

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class HalationBloomConfig(
    val halationIntensity: Float = 0.4f,   // 0.0 .. 1.0
    val halationRadius: Float = 12.0f,     // Spread radius in pixels
    val halationThreshold: Float = 0.75f,  // Brightness trigger
    val bloomIntensity: Float = 0.35f,     // 0.0 .. 1.0
    val bloomThreshold: Float = 0.82f,     // Highlight cutoff
    val bloomRadius: Float = 24.0f
)

/**
 * Optical Halation and Bloom Generator.
 * Simulates vintage film halation (red-orange scattering in anti-halation layer)
 * and optical glass bloom (anamorphic / spherical lens flare glow).
 */
class HalationBloomEngine {

    /**
     * GPU shader uniform payload for halation and bloom passes.
     */
    fun getUniformPayload(config: HalationBloomConfig): FloatArray {
        return floatArrayOf(
            config.halationIntensity,
            config.halationRadius,
            config.halationThreshold,
            config.bloomIntensity,
            config.bloomThreshold,
            config.bloomRadius,
            0.0f, 0.0f // 16-byte alignment
        )
    }

    /**
     * Evaluates halation spectral bleed on a pixel based on thresholded specular highlights.
     * Film halation scatters predominantly red and warm wavelengths.
     */
    fun computeHalationColor(r: Float, g: Float, b: Float, intensity: Float): FloatArray {
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        // Characteristic Kodak 2383 / Vision3 red-orange fringe: RGB (1.0, 0.22, 0.05)
        val bleedR = intensity * 1.0f
        val bleedG = intensity * 0.22f
        val bleedB = intensity * 0.04f

        return floatArrayOf(
            (r + bleedR).coerceIn(0.0f, 1.0f),
            (g + bleedG).coerceIn(0.0f, 1.0f),
            (b + bleedB).coerceIn(0.0f, 1.0f)
        )
    }
}
