package com.photoengine.core.effects

import kotlin.math.sin

data class FilmGrainConfig(
    val amount: Float = 0.25f,      // Grain intensity 0.0 .. 1.0
    val size: Float = 1.6f,         // Grain clump size 1.0 .. 4.0
    val roughness: Float = 0.5f,    // High frequency sharpness
    val shadowsFade: Float = 0.8f,  // Fade grain in deep blacks
    val highlightsFade: Float = 0.6f // Fade grain in blown whites
)

/**
 * Procedural Silver-Halide 35mm / 16mm Film Grain Synthesis.
 * Mathematically derived per pixel to eliminate repeating tiled textures.
 * Uses high-efficiency pseudo-random hashing suited for SIMD and GPU fragment/compute shaders.
 */
class FilmGrainEngine {

    fun getUniformPayload(config: FilmGrainConfig, frameSeed: Float): FloatArray {
        return floatArrayOf(
            config.amount,
            config.size,
            config.roughness,
            config.shadowsFade,
            config.highlightsFade,
            frameSeed,
            0.0f, 0.0f
        )
    }

    /**
     * Synthesizes organic grain modulation for a given pixel coordinate and luminance.
     */
    fun sampleFilmGrain(x: Float, y: Float, luma: Float, seed: Float, config: FilmGrainConfig): Float {
        // High quality pseudo-random spatial hash
        val scaledX = (x / config.size)
        val scaledY = (y / config.size)
        val n = sin(scaledX * 12.9898f + scaledY * 78.233f + seed * 43.123f) * 43758.5453f
        val rawNoise = (n - kotlin.math.floor(n)) * 2.0f - 1.0f

        // Natural film response: grain is most visible in midtones, fading in pure highlights and deep blacks
        val midtoneWeight = (1.0f - kotlin.math.abs(luma - 0.5f) * 2.0f).coerceIn(0.1f, 1.0f)

        return rawNoise * config.amount * midtoneWeight
    }
}
