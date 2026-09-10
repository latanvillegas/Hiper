package com.photoengine.core.gpu

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

/**
 * CPU-based Image Processing Driver (pure Kotlin / Bitmap / Canvas).
 * Eliminates obsolete Android RenderScript dependencies and NDK requirements
 * while keeping 100% API compatibility with downstream image modules.
 */
class RenderScriptFallback(private val context: Context) {

    fun initialize(): Boolean {
        Log.i("RenderScriptFallback", "CPU processing fallback driver initialized successfully (Pure Kotlin / Canvas).")
        return true
    }

    /**
     * Applies a 256-level RGB LUT using pure Kotlin CPU processing.
     */
    fun applyLut(input: Bitmap, output: Bitmap, lutTableRgba: ByteArray) {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        val rLut = IntArray(256)
        val gLut = IntArray(256)
        val bLut = IntArray(256)

        for (i in 0..255) {
            rLut[i] = lutTableRgba[i * 4 + 0].toInt() and 0xFF
            gLut[i] = lutTableRgba[i * 4 + 1].toInt() and 0xFF
            bLut[i] = lutTableRgba[i * 4 + 2].toInt() and 0xFF
        }

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            val newR = rLut[r]
            val newG = gLut[g]
            val newB = bLut[b]

            pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Executes fast Gaussian / Box blur for halation / bloom with configurable radius on CPU.
     */
    fun applyFastBlur(input: Bitmap, output: Bitmap, radius: Float) {
        val rad = radius.toInt().coerceIn(1, 25)
        val width = input.width
        val height = input.height
        val inPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        input.getPixels(inPixels, 0, width, 0, 0, width, height)

        // Fast horizontal box blur pass
        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                val startX = (x - rad).coerceAtLeast(0)
                val endX = (x + rad).coerceAtMost(width - 1)

                for (kx in startX..endX) {
                    val p = inPixels[yOffset + kx]
                    rSum += (p shr 16) and 0xFF
                    gSum += (p shr 8) and 0xFF
                    bSum += p and 0xFF
                    count++
                }

                val orig = inPixels[yOffset + x]
                val a = (orig shr 24) and 0xFF
                outPixels[yOffset + x] = (a shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
    }

    fun release() {
        // No-op for pure CPU driver
    }
}
