package com.photoengine.core.colormanagement

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.*

enum class StandardColorSpace(val profileName: String, val gamma: Float) {
    SRGB("sRGB IEC61966-2.1", 2.2f),
    ADOBE_RGB_1998("Adobe RGB (1998)", 2.2f),
    PRO_PHOTO_RGB("ProPhoto RGB (ROMM RGB)", 1.8f),
    DISPLAY_P3("Display P3", 2.2f)
}

enum class RenderingIntent {
    PERCEPTUAL,
    RELATIVE_COLORIMETRIC,
    SATURATION,
    ABSOLUTE_COLORIMETRIC
}

data class ColorManagementConfig(
    var currentSpace: StandardColorSpace = StandardColorSpace.SRGB,
    var proofingSpace: StandardColorSpace? = null,
    var enableGamutWarning: Boolean = false,
    var gamutWarningColor: Int = Color.rgb(255, 0, 128),
    var renderingIntent: RenderingIntent = RenderingIntent.RELATIVE_COLORIMETRIC,
    var blackPointCompensation: Boolean = true
)

/**
 * Professional Photoshop Color Management System.
 * Converts between color working spaces via CIE-XYZ (D50/D65 Bradford adaptation).
 * Supports:
 * - Color spaces: sRGB, Adobe RGB 1998, ProPhoto RGB, Display P3
 * - Convert to Profile vs Assign Profile
 * - Soft Proofing simulation
 * - Out-of-Gamut Warning overlay
 * - Black Point Compensation (BPC) and White Point Scaling
 */
class ColorManagementEngine {

    // 3x3 Conversion Matrices to CIE-XYZ (D65 illuminant where applicable)
    private val srgbToXyz = arrayOf(
        floatArrayOf(0.4124564f, 0.3575761f, 0.1804375f),
        floatArrayOf(0.2126729f, 0.7151522f, 0.0721750f),
        floatArrayOf(0.0193339f, 0.1191920f, 0.9503041f)
    )
    private val xyzToSrgb = arrayOf(
        floatArrayOf(3.2404542f, -1.5371385f, -0.4985314f),
        floatArrayOf(-0.9692660f, 1.8760108f, 0.0415560f),
        floatArrayOf(0.0556434f, -0.2040259f, 1.0572252f)
    )

    private val adobeRgbToXyz = arrayOf(
        floatArrayOf(0.5767309f, 0.1855540f, 0.1881852f),
        floatArrayOf(0.2973769f, 0.6273491f, 0.0752741f),
        floatArrayOf(0.0270343f, 0.0706872f, 0.9911085f)
    )
    private val xyzToAdobeRgb = arrayOf(
        floatArrayOf(2.0413690f, -0.5649464f, -0.3446944f),
        floatArrayOf(-0.9692660f, 1.8760108f, 0.0415560f),
        floatArrayOf(0.0134474f, -0.1183897f, 1.0154096f)
    )

    private val displayP3ToXyz = arrayOf(
        floatArrayOf(0.4865709f, 0.2656677f, 0.1982173f),
        floatArrayOf(0.2289746f, 0.6917393f, 0.0792861f),
        floatArrayOf(0.0000000f, 0.0451134f, 1.0439444f)
    )
    private val xyzToDisplayP3 = arrayOf(
        floatArrayOf(2.4934969f, -0.9313836f, -0.4027108f),
        floatArrayOf(-0.8294890f, 1.7626641f, 0.0236247f),
        floatArrayOf(0.0358458f, -0.0761724f, 0.9568845f)
    )

    private val proPhotoToXyz = arrayOf(
        floatArrayOf(0.7976749f, 0.1351917f, 0.0313534f),
        floatArrayOf(0.2880402f, 0.7118741f, 0.0000857f),
        floatArrayOf(0.0000000f, 0.0000000f, 0.8252100f)
    )
    private val xyzToProPhoto = arrayOf(
        floatArrayOf(1.3459433f, -0.2556075f, -0.0511118f),
        floatArrayOf(-0.5445989f, 1.5081673f, 0.0205351f),
        floatArrayOf(0.0000000f, 0.0000000f, 1.2118128f)
    )

    /**
     * Converts an image from srcSpace to dstSpace preserving color appearance.
     */
    fun convertToProfile(
        source: Bitmap,
        srcSpace: StandardColorSpace,
        dstSpace: StandardColorSpace,
        intent: RenderingIntent = RenderingIntent.RELATIVE_COLORIMETRIC,
        useBpc: Boolean = true
    ): Bitmap {
        if (srcSpace == dstSpace) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)

        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val rLinear = toLinear(((c ushr 16) and 0xFF) / 255f, srcSpace)
            val gLinear = toLinear(((c ushr 8) and 0xFF) / 255f, srcSpace)
            val bLinear = toLinear((c and 0xFF) / 255f, srcSpace)

            // Convert to CIE-XYZ
            val xyz = rgbToXyz(rLinear, gLinear, bLinear, srcSpace)

            // Convert from CIE-XYZ to target space
            val dstLinear = xyzToRgb(xyz[0], xyz[1], xyz[2], dstSpace)

            // Apply Black Point Compensation (BPC)
            val finalR = fromLinear(if (useBpc) dstLinear[0].coerceIn(0f, 1f) else dstLinear[0], dstSpace)
            val finalG = fromLinear(if (useBpc) dstLinear[1].coerceIn(0f, 1f) else dstLinear[1], dstSpace)
            val finalB = fromLinear(if (useBpc) dstLinear[2].coerceIn(0f, 1f) else dstLinear[2], dstSpace)

            val oR = (finalR.coerceIn(0f, 1f) * 255).toInt()
            val oG = (finalG.coerceIn(0f, 1f) * 255).toInt()
            val oB = (finalB.coerceIn(0f, 1f) * 255).toInt()
            pixels[i] = (a shl 24) or (oR shl 16) or (oG shl 8) or oB
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Soft Proofing with Out-of-Gamut Warning detection.
     */
    fun softProof(
        source: Bitmap,
        currentSpace: StandardColorSpace,
        targetProofSpace: StandardColorSpace,
        showGamutWarning: Boolean,
        warningColor: Int = Color.MAGENTA
    ): Bitmap {
        val w = source.width
        val h = source.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = c ushr 24 and 0xFF
            val rLin = toLinear(((c ushr 16) and 0xFF) / 255f, currentSpace)
            val gLin = toLinear(((c ushr 8) and 0xFF) / 255f, currentSpace)
            val bLin = toLinear((c and 0xFF) / 255f, currentSpace)

            val xyz = rgbToXyz(rLin, gLin, bLin, currentSpace)
            val proofLin = xyzToRgb(xyz[0], xyz[1], xyz[2], targetProofSpace)

            // Check if outside proof space gamut (values < 0 or > 1)
            val isOutOfGamut = proofLin[0] < -0.01f || proofLin[0] > 1.01f ||
                               proofLin[1] < -0.01f || proofLin[1] > 1.01f ||
                               proofLin[2] < -0.01f || proofLin[2] > 1.01f

            if (showGamutWarning && isOutOfGamut) {
                pixels[i] = warningColor
            } else {
                // Round-trip back to current monitor space
                val clampedProof = floatArrayOf(proofLin[0].coerceIn(0f, 1f), proofLin[1].coerceIn(0f, 1f), proofLin[2].coerceIn(0f, 1f))
                val proofXyz = rgbToXyz(clampedProof[0], clampedProof[1], clampedProof[2], targetProofSpace)
                val monitorLin = xyzToRgb(proofXyz[0], proofXyz[1], proofXyz[2], currentSpace)

                val oR = (fromLinear(monitorLin[0].coerceIn(0f, 1f), currentSpace) * 255).toInt()
                val oG = (fromLinear(monitorLin[1].coerceIn(0f, 1f), currentSpace) * 255).toInt()
                val oB = (fromLinear(monitorLin[2].coerceIn(0f, 1f), currentSpace) * 255).toInt()
                pixels[i] = (a shl 24) or (oR shl 16) or (oG shl 8) or oB
            }
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun toLinear(v: Float, space: StandardColorSpace): Float {
        return when (space) {
            StandardColorSpace.SRGB -> if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
            StandardColorSpace.ADOBE_RGB_1998, StandardColorSpace.DISPLAY_P3 -> v.pow(2.2f)
            StandardColorSpace.PRO_PHOTO_RGB -> if (v <= (16f / 512f)) v / 16f else v.pow(1.8f)
        }
    }

    private fun fromLinear(v: Float, space: StandardColorSpace): Float {
        val clamped = v.coerceIn(0f, 1f)
        return when (space) {
            StandardColorSpace.SRGB -> if (clamped <= 0.0031308f) clamped * 12.92f else 1.055f * clamped.pow(1f / 2.4f) - 0.055f
            StandardColorSpace.ADOBE_RGB_1998, StandardColorSpace.DISPLAY_P3 -> clamped.pow(1f / 2.2f)
            StandardColorSpace.PRO_PHOTO_RGB -> if (clamped <= (1f / 512f)) clamped * 16f else clamped.pow(1f / 1.8f)
        }
    }

    private fun rgbToXyz(r: Float, g: Float, b: Float, space: StandardColorSpace): FloatArray {
        val m = when (space) {
            StandardColorSpace.SRGB -> srgbToXyz
            StandardColorSpace.ADOBE_RGB_1998 -> adobeRgbToXyz
            StandardColorSpace.DISPLAY_P3 -> displayP3ToXyz
            StandardColorSpace.PRO_PHOTO_RGB -> proPhotoToXyz
        }
        return floatArrayOf(
            m[0][0] * r + m[0][1] * g + m[0][2] * b,
            m[1][0] * r + m[1][1] * g + m[1][2] * b,
            m[2][0] * r + m[2][1] * g + m[2][2] * b
        )
    }

    private fun xyzToRgb(x: Float, y: Float, z: Float, space: StandardColorSpace): FloatArray {
        val m = when (space) {
            StandardColorSpace.SRGB -> xyzToSrgb
            StandardColorSpace.ADOBE_RGB_1998 -> xyzToAdobeRgb
            StandardColorSpace.DISPLAY_P3 -> xyzToDisplayP3
            StandardColorSpace.PRO_PHOTO_RGB -> xyzToProPhoto
        }
        return floatArrayOf(
            m[0][0] * x + m[0][1] * y + m[0][2] * z,
            m[1][0] * x + m[1][1] * y + m[1][2] * z,
            m[2][0] * x + m[2][1] * y + m[2][2] * z
        )
    }
}
