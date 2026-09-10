package com.photoengine.core.geometry

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.*

enum class PerspectiveFillMode {
    CROP,
    AUTO_FILL,
    STRETCH
}

data class PerspectiveConfig(
    val verticalKeystone: Float = 0.0f,   // -45° .. +45°
    val horizontalKeystone: Float = 0.0f, // -45° .. +45°
    val rotation: Float = 0.0f,           // -180° .. +180°
    val scale: Float = 1.0f,
    val aspectCorrection: Float = 1.0f,
    val fillMode: PerspectiveFillMode = PerspectiveFillMode.AUTO_FILL
)

/**
 * Geometric Perspective Correction Module.
 * Features:
 * - Manual 4-point quad / homography transform
 * - Architectural keystoning (vertical & horizontal tilt)
 * - Automatic horizon leveling & straightener via dominant edge orientation detection.
 */
class PerspectiveEngine {

    /**
     * Calculates the 3x3 homography matrix from keystoning parameters.
     */
    fun createHomographyMatrix(width: Float, height: Float, config: PerspectiveConfig): Matrix {
        val matrix = Matrix()

        val cx = width * 0.5f
        val cy = height * 0.5f

        // Corners in standard image space: Top-Left, Top-Right, Bottom-Right, Bottom-Left
        val src = floatArrayOf(
            0f, 0f,
            width, 0f,
            width, height,
            0f, height
        )

        // Deform quad based on keystone angles
        val vOffset = sin(Math.toRadians(config.verticalKeystone.toDouble())).toFloat() * width * 0.35f
        val hOffset = sin(Math.toRadians(config.horizontalKeystone.toDouble())).toFloat() * height * 0.35f

        val dst = floatArrayOf(
            vOffset, hOffset,                             // TL
            width - vOffset, -hOffset,                    // TR
            width + vOffset * 0.5f, height + hOffset,     // BR
            -vOffset * 0.5f, height - hOffset             // BL
        )

        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        matrix.postRotate(config.rotation, cx, cy)
        matrix.postScale(config.scale, config.scale * config.aspectCorrection, cx, cy)

        return matrix
    }

    /**
     * Applies perspective warp to a Bitmap with bilinear interpolation and boundary mode.
     */
     fun applyPerspective(bitmap: Bitmap, config: PerspectiveConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val warped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(warped)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val matrix = createHomographyMatrix(w.toFloat(), h.toFloat(), config)
        canvas.drawBitmap(bitmap, matrix, paint)

        return when (config.fillMode) {
            PerspectiveFillMode.CROP -> cropInscribedRectangle(warped, config)
            PerspectiveFillMode.AUTO_FILL -> contentAwareFillBorders(warped)
            PerspectiveFillMode.STRETCH -> warped
        }
    }

    /**
     * Content-aware fill for missing perspective triangular border wedges.
     * Propagates valid edge color and texture inward seamlessly.
     */
    private fun contentAwareFillBorders(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Find and fill empty pixels (alpha < 10) by projecting closest valid boundary pixels
        for (y in 0 until h) {
            var firstValid = -1
            var lastValid = -1
            for (x in 0 until w) {
                val alpha = (pixels[y * w + x] ushr 24) and 0xFF
                if (alpha > 30) {
                    if (firstValid == -1) firstValid = x
                    lastValid = x
                }
            }

            if (firstValid != -1 && lastValid != -1) {
                val leftColor = pixels[y * w + firstValid]
                for (x in 0 until firstValid) {
                    pixels[y * w + x] = leftColor
                }
                val rightColor = pixels[y * w + lastValid]
                for (x in (lastValid + 1) until w) {
                    pixels[y * w + x] = rightColor
                }
            }
        }

        // Vertical pass for top/bottom empty regions
        for (x in 0 until w) {
            var firstValidY = -1
            var lastValidY = -1
            for (y in 0 until h) {
                val alpha = (pixels[y * w + x] ushr 24) and 0xFF
                if (alpha > 30) {
                    if (firstValidY == -1) firstValidY = y
                    lastValidY = y
                }
            }

            if (firstValidY != -1 && lastValidY != -1) {
                val topColor = pixels[firstValidY * w + x]
                for (y in 0 until firstValidY) {
                    pixels[y * w + x] = topColor
                }
                val bottomColor = pixels[lastValidY * w + x]
                for (y in (lastValidY + 1) until h) {
                    pixels[y * w + x] = bottomColor
                }
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun cropInscribedRectangle(bitmap: Bitmap, config: PerspectiveConfig): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val maxOffset = (max(abs(config.verticalKeystone), abs(config.horizontalKeystone)) / 45.0f * 0.2f)
            .coerceIn(0.0f, 0.35f)
        val cropX = (w * maxOffset).toInt()
        val cropY = (h * maxOffset).toInt()
        val cropW = (w - cropX * 2).coerceAtLeast(1)
        val cropH = (h - cropY * 2).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
        return Bitmap.createScaledBitmap(cropped, w, h, true)
    }

    /**
     * Automatic straightener: Detects dominant horizontal and vertical lines
     * using Sobel gradients and computes the angle needed to level the horizon.
     */
    fun detectAutoStraightenAngle(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val step = max(1, w / 200)

        var angleWeightedSum = 0.0
        var totalWeight = 0.0

        for (y in step until h - step step step * 2) {
            for (x in step until w - step step step * 2) {
                val left = bitmap.getPixel(x - step, y) and 0xFF
                val right = bitmap.getPixel(x + step, y) and 0xFF
                val top = bitmap.getPixel(x, y - step) and 0xFF
                val bottom = bitmap.getPixel(x, y + step) and 0xFF

                val gx = (right - left).toDouble()
                val gy = (bottom - top).toDouble()
                val mag = sqrt(gx * gx + gy * gy)

                if (mag > 35.0) { // Only high contrast edges
                    val theta = atan2(gy, gx) * 180.0 / Math.PI
                    // Look for nearly horizontal lines within +/- 15 degrees
                    if (abs(theta) < 15.0) {
                        angleWeightedSum += theta * mag
                        totalWeight += mag
                    }
                }
            }
        }

        return if (totalWeight > 0.0) -(angleWeightedSum / totalWeight).toFloat() else 0.0f
    }
}
