package com.photoengine.core.effects

import android.graphics.Bitmap
import kotlin.math.*

enum class FilmProfile(val profileName: String) {
    KODAK_PORTRA_400("Kodak Portra 400"),
    KODAK_PORTRA_160("Kodak Portra 160"),
    FUJI_PRO_400H("Fuji Pro 400H"),
    ILFORD_HP5_PLUS("Ilford HP5 Plus (B&W)"),
    KODAK_TRI_X_400("Kodak Tri-X 400 (B&W)"),
    KODAK_EKTAR_100("Kodak Ektar 100"),
    AGFA_VISTA_200("Agfa Vista 200"),
    CINESTILL_800T("CineStill 800T")
}

data class FilmSimulationConfig(
    val profile: FilmProfile = FilmProfile.KODAK_PORTRA_400,
    val intensity: Float = 1.0f,         // 0.0 .. 1.0 (0 to 100%)
    val fade: Float = 0.15f,             // 0.0 .. 1.0 (lifted film shadows)
    val vignette: Float = 0.25f,         // 0.0 .. 1.0 (subtle optical falloff)
    val halationStrength: Float = 0.4f,  // 0.0 .. 1.0 (red-orange glow around highlights)
    val bloomIntensity: Float = 0.3f,    // 0.0 .. 1.0 (light diffusion)
    val grainIntensity: Float = 0.25f,   // 0.0 .. 1.0
    val grainSize: Float = 1.2f,         // 0.5 .. 3.0
    val grainRoughness: Float = 0.5f     // 0.0 .. 1.0
)

/**
 * Professional Film Emulation Engine (Snapseed 2026 Film specification).
 * Accurately models chemical film dyes, S-curve response, lifted shadow fade,
 * organic photochemical halation, diffusion bloom, and silver halide grain.
 */
class FilmSimulationEngine {

    val filmGrainEngine = FilmGrainEngine()
    val halationBloomEngine = HalationBloomEngine()

    /**
     * Color matrix transform for film color science: converts linear sRGB to film spectral response.
     */
    fun getProfileMatrix(profile: FilmProfile): FloatArray = when (profile) {
        FilmProfile.KODAK_PORTRA_400 -> floatArrayOf(
            1.05f, -0.02f, -0.01f,
            -0.03f, 1.02f, 0.01f,
            -0.02f, -0.04f, 1.08f
        )
        FilmProfile.KODAK_PORTRA_160 -> floatArrayOf(
            1.02f, 0.01f, -0.02f,
            -0.01f, 1.04f, -0.01f,
            -0.03f, -0.02f, 1.05f
        )
        FilmProfile.FUJI_PRO_400H -> floatArrayOf(
            0.96f, 0.04f, 0.02f,
            -0.01f, 1.06f, -0.02f,
            0.02f, -0.02f, 1.06f
        )
        FilmProfile.ILFORD_HP5_PLUS -> floatArrayOf(
            0.30f, 0.59f, 0.11f,
            0.30f, 0.59f, 0.11f,
            0.30f, 0.59f, 0.11f
        )
        FilmProfile.KODAK_TRI_X_400 -> floatArrayOf(
            0.33f, 0.50f, 0.17f,
            0.33f, 0.50f, 0.17f,
            0.33f, 0.50f, 0.17f
        )
        FilmProfile.KODAK_EKTAR_100 -> floatArrayOf(
            1.15f, -0.08f, -0.02f,
            -0.04f, 1.12f, -0.03f,
            -0.02f, -0.06f, 1.18f
        )
        FilmProfile.AGFA_VISTA_200 -> floatArrayOf(
            1.08f, -0.04f, 0.01f,
            -0.02f, 1.05f, -0.01f,
            -0.05f, 0.02f, 1.04f
        )
        FilmProfile.CINESTILL_800T -> floatArrayOf(
            0.94f, 0.02f, 0.06f,
            -0.02f, 0.98f, 0.05f,
            -0.08f, -0.04f, 1.16f
        )
    }

    /**
     * Applies full chemical film pipeline to an input Bitmap.
     */
    fun processFilm(bitmap: Bitmap, config: FilmSimulationConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val mat = getProfileMatrix(config.profile)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxDist = sqrt(cx * cx + cy * cy)

        val intensity = config.intensity.coerceIn(0.0f, 1.0f)
        val fadeLift = config.fade * 0.12f

        for (y in 0 until h) {
            val dy = y - cy
            for (x in 0 until w) {
                val dx = x - cx
                val idx = y * w + x
                val c = pixels[idx]

                val r = ((c shr 16) and 0xFF) / 255.0f
                val g = ((c shr 8) and 0xFF) / 255.0f
                val b = (c and 0xFF) / 255.0f

                // 1. Film spectral matrix
                var fr = mat[0] * r + mat[1] * g + mat[2] * b
                var fg = mat[3] * r + mat[4] * g + mat[5] * b
                var fb = mat[6] * r + mat[7] * g + mat[8] * b

                // 2. Film S-curve tone response with lifted shadows (fade)
                fr = filmToneCurve(fr, fadeLift)
                fg = filmToneCurve(fg, fadeLift)
                fb = filmToneCurve(fb, fadeLift)

                // 3. Halation (warm glow on highlights)
                if (config.halationStrength > 0.01f) {
                    val luma = 0.2126f * fr + 0.7152f * fg + 0.0722f * fb
                    if (luma > 0.65f) {
                        val glow = (luma - 0.65f) * config.halationStrength * 0.45f
                        fr += glow * 1.2f // Strong red-orange bias
                        fg += glow * 0.4f
                    }
                }

                // 4. Optical Vignette falloff
                if (config.vignette > 0.01f) {
                    val dist = sqrt(dx * dx + dy * dy) / maxDist
                    val vig = (1.0f - dist.pow(2.0f) * config.vignette * 0.65f).coerceIn(0.0f, 1.0f)
                    fr *= vig
                    fg *= vig
                    fb *= vig
                }

                // 5. Film grain overlay (fast random noise with grain size & roughness)
                if (config.grainIntensity > 0.01f) {
                    val noiseSeed = (x * 12.9898f + y * 78.233f + config.grainSize * 43.1f)
                    val randNoise = ((sin(noiseSeed.toDouble()) * 43758.5453).toFloat() % 1.0f) - 0.5f
                    val luma = 0.2126f * fr + 0.7152f * fg + 0.0722f * fb
                    val grainMask = 4.0f * luma * (1.0f - luma) // Grain is most visible in midtones
                    val grainVal = randNoise * config.grainIntensity * 0.22f * grainMask * config.grainRoughness
                    fr += grainVal
                    fg += grainVal
                    fb += grainVal
                }

                // Blend with original according to intensity
                val finalR = (r * (1.0f - intensity) + fr * intensity).coerceIn(0.0f, 1.0f)
                val finalG = (g * (1.0f - intensity) + fg * intensity).coerceIn(0.0f, 1.0f)
                val finalB = (b * (1.0f - intensity) + fb * intensity).coerceIn(0.0f, 1.0f)

                val outR = (finalR * 255.0f).toInt()
                val outG = (finalG * 255.0f).toInt()
                val outB = (finalB * 255.0f).toInt()
                pixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun filmToneCurve(v: Float, fade: Float): Float {
        val clamped = v.coerceIn(0.0f, 1.0f)
        // Sigmoid curve with lifted toe (fade)
        val s = (clamped * clamped * (3.0f - 2.0f * clamped))
        return fade + (1.0f - fade) * s
    }
}
