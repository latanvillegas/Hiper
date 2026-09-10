package com.photoengine.core.mask

import android.graphics.*
import kotlin.math.*

data class LinearGradientConfig(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val feather: Float = 0.5f // Transition smoothness
)

data class RadialGradientConfig(
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float,
    val angleDegrees: Float = 0.0f,
    val feather: Float = 0.5f
)

/**
 * Geometric Gradient Mask Generator.
 * Produces Linear Gradients (Horizon/Sky adjustments) and Radial Gradients (Vignettes, Spotlights)
 * with continuous feather transitions.
 */
class GradientMaskEngine(
    private val width: Int,
    private val height: Int
) {
    val maskBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    private val canvas = Canvas(maskBitmap)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }

    fun renderLinearGradient(config: LinearGradientConfig) {
        maskBitmap.eraseColor(0)

        val innerStop = (1.0f - config.feather).coerceIn(0.05f, 0.95f) * 0.5f
        val gradient = LinearGradient(
            config.startX, config.startY, config.endX, config.endY,
            intArrayOf(
                Color.WHITE,
                Color.argb((255 * (1.0f - innerStop)).toInt(), 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.0f, innerStop, 1.0f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    fun renderRadialGradient(config: RadialGradientConfig) {
        maskBitmap.eraseColor(0)
        canvas.save()

        // Translate and rotate around center
        canvas.translate(config.centerX, config.centerY)
        canvas.rotate(config.angleDegrees)

        // Scale to handle non-circular ellipses (radiusX vs radiusY)
        val aspect = if (config.radiusY > 0) config.radiusX / config.radiusY else 1.0f
        canvas.scale(1.0f, 1.0f / aspect)

        val maxR = config.radiusX.coerceAtLeast(1.0f)
        val innerFeather = (1.0f - config.feather).coerceIn(0.0f, 0.9f)

        val gradient = RadialGradient(
            0f, 0f, maxR,
            intArrayOf(Color.WHITE, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0.0f, innerFeather, 1.0f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawCircle(0f, 0f, maxR, paint)
        canvas.restore()
    }
}
