package com.photoengine.core.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

data class BrushStrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

enum class BrushMode {
    EXPOSURE,       // -5.0 .. +5.0 EV
    SATURATION,     // -100 .. +100
    TEMPERATURE,    // -100 (cool) .. +100 (warm)
    CLARITY         // -100 .. +100 (structure)
}

data class BrushConfig(
    var mode: BrushMode = BrushMode.EXPOSURE,
    var value: Float = 1.0f,            // Parameter value depending on mode
    var size: Float = 40.0f,            // 1 .. 100 px
    var feather: Float = 50.0f,         // 0 .. 100% (feather/softness)
    var zoomFactor: Float = 1.0f        // 1.0 .. 10.0x
)

/**
 * Selective Brush Engine.
 * Supports dynamic radius, hardness (feathering), flow, opacity, eraser mode,
 * and high-precision stroke application for Exposure, Saturation, Temperature, and Clarity.
 */
class SelectiveBrushEngine(
    val width: Int,
    val height: Int
) {
    val maskBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    private val canvas = Canvas(maskBitmap)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }

    var config: BrushConfig = BrushConfig()
    var radius: Float
        get() = config.size
        set(value) { config.size = value }
    var hardness: Float
        get() = 1.0f - (config.feather / 100.0f)
        set(value) { config.feather = (1.0f - value) * 100.0f }
    var flow: Float = 0.8f     // Rate of application per stroke
    var opacity: Float = 1.0f  // Max density ceiling
    var isEraser: Boolean = false

    fun drawStamp(x: Float, y: Float, pressure: Float = 1.0f) {
        val effectiveRadius = (radius * pressure / config.zoomFactor).coerceAtLeast(1.0f)
        val effectiveAlpha = (255 * flow * opacity).toInt().coerceIn(1, 255)

        if (isEraser) {
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawCircle(x, y, effectiveRadius, paint)
            return
        }

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.ADD)

        if (hardness >= 0.98f) {
            paint.shader = null
            paint.alpha = effectiveAlpha
            canvas.drawCircle(x, y, effectiveRadius, paint)
        } else {
            val innerStop = hardness.coerceIn(0.0f, 0.95f)
            val gradient = RadialGradient(
                x, y, effectiveRadius,
                intArrayOf(
                    android.graphics.Color.argb(effectiveAlpha, 255, 255, 255),
                    android.graphics.Color.argb((effectiveAlpha * 0.5f).toInt(), 255, 255, 255),
                    android.graphics.Color.argb(0, 255, 255, 255)
                ),
                floatArrayOf(0.0f, innerStop, 1.0f),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawCircle(x, y, effectiveRadius, paint)
        }
    }

    fun clearMask() {
        maskBitmap.eraseColor(0)
    }

    fun fillMask() {
        maskBitmap.eraseColor(android.graphics.Color.WHITE)
    }

    fun invertMask() {
        val pixels = ByteArray(width * height)
        val buf = java.nio.ByteBuffer.wrap(pixels)
        maskBitmap.copyPixelsToBuffer(buf)
        for (i in pixels.indices) {
            val v = pixels[i].toInt() and 0xFF
            pixels[i] = (255 - v).toByte()
        }
        buf.rewind()
        maskBitmap.copyPixelsFromBuffer(buf)
    }

    /**
     * Applies the selected brush effect (Exposure, Saturation, Temperature, Clarity)
     * modulated by the brush's alpha mask onto the target bitmap.
     */
    fun applyBrushEffect(baseBitmap: Bitmap, mode: BrushMode = config.mode, value: Float = config.value): Bitmap {
        val w = baseBitmap.width
        val h = baseBitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(w * h)
        baseBitmap.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val maskBytes = ByteArray(w * h)
        val maskBuf = java.nio.ByteBuffer.wrap(maskBytes)
        maskBitmap.copyPixelsToBuffer(maskBuf)

        for (i in srcPixels.indices) {
            val maskAlpha = (maskBytes[i].toInt() and 0xFF) / 255.0f
            if (maskAlpha <= 0.001f) continue

            val c = srcPixels[i]
            var r = ((c shr 16) and 0xFF) / 255.0f
            var g = ((c shr 8) and 0xFF) / 255.0f
            var b = (c and 0xFF) / 255.0f

            when (mode) {
                BrushMode.EXPOSURE -> {
                    // value is EV (-5.0 .. +5.0)
                    val factor = 2.0.pow((value * maskAlpha).toDouble()).toFloat()
                    r *= factor
                    g *= factor
                    b *= factor
                }
                BrushMode.SATURATION -> {
                    // value is -100 .. +100
                    val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    val satFactor = 1.0f + (value / 100.0f) * maskAlpha
                    r = luma + (r - luma) * satFactor
                    g = luma + (g - luma) * satFactor
                    b = luma + (b - luma) * satFactor
                }
                BrushMode.TEMPERATURE -> {
                    // value is -100 (cool) .. +100 (warm)
                    val tShift = (value / 100.0f) * 0.35f * maskAlpha
                    r += tShift
                    b -= tShift
                }
                BrushMode.CLARITY -> {
                    // value is -100 .. +100 (structure/micro-contrast)
                    val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                    val midtoneMask = kotlin.math.sin((luma.coerceIn(0f, 1f) * Math.PI).toDouble()).toFloat()
                    val cShift = (value / 100.0f) * 0.3f * maskAlpha * midtoneMask * (luma - 0.5f)
                    r += cShift
                    g += cShift
                    b += cShift
                }
            }

            val outR = (r.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
            val outG = (g.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
            val outB = (b.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
            srcPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(srcPixels, 0, w, 0, 0, w, h)
        return output
    }
}
