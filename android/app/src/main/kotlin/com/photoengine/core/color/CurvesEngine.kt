package com.photoengine.core.color

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class CurveChannel {
    RGB_MASTER,
    RED,
    GREEN,
    BLUE
}

/**
 * Professional RGB Curves Engine with up to 14 control points per channel.
 * Precomputes a 256x1 4-channel FP16/RGBA8 1D LUT texture directly bound to Vulkan/OpenGL shaders.
 */
class CurvesEngine {
    private val interpolator = CubicSplineInterpolator()

    // 14 control points max per channel
    private val masterPoints = mutableListOf(CurvePoint(0.0f, 0.0f), CurvePoint(1.0f, 1.0f))
    private val redPoints = mutableListOf(CurvePoint(0.0f, 0.0f), CurvePoint(1.0f, 1.0f))
    private val greenPoints = mutableListOf(CurvePoint(0.0f, 0.0f), CurvePoint(1.0f, 1.0f))
    private val bluePoints = mutableListOf(CurvePoint(0.0f, 0.0f), CurvePoint(1.0f, 1.0f))

    private var isDirty = true
    private val cachedLutBuffer = ByteBuffer.allocateDirect(256 * 4).order(ByteOrder.nativeOrder())

    fun setPoints(channel: CurveChannel, points: List<CurvePoint>) {
        require(points.size <= 14) { "Maximum of 14 control points allowed per curve channel." }
        val target = when (channel) {
            CurveChannel.RGB_MASTER -> masterPoints
            CurveChannel.RED -> redPoints
            CurveChannel.GREEN -> greenPoints
            CurveChannel.BLUE -> bluePoints
        }
        target.clear()
        target.addAll(points)
        isDirty = true
    }

    fun getPoints(channel: CurveChannel): List<CurvePoint> = when (channel) {
        CurveChannel.RGB_MASTER -> masterPoints.toList()
        CurveChannel.RED -> redPoints.toList()
        CurveChannel.GREEN -> greenPoints.toList()
        CurveChannel.BLUE -> bluePoints.toList()
    }

    /**
     * Bakes the curves into a 256-pixel RGBA texture buffer for the GPU shader.
     * Master curve is composed with the individual R, G, B curves:
     * final_R(x) = MasterLut(RedLut(x))
     */
    fun getGpuLutTextureBuffer(): ByteBuffer {
        if (!isDirty) {
            cachedLutBuffer.rewind()
            return cachedLutBuffer
        }

        val masterLut = interpolator.generateLut256(masterPoints)
        val redLut = interpolator.generateLut256(redPoints)
        val greenLut = interpolator.generateLut256(greenPoints)
        val blueLut = interpolator.generateLut256(bluePoints)

        cachedLutBuffer.clear()
        for (i in 0..255) {
            // Apply individual channel curve then master tone curve
            val rVal = redLut[i]
            val gVal = greenLut[i]
            val bVal = blueLut[i]

            val rIdx = (rVal * 255.0f).toInt().coerceIn(0, 255)
            val gIdx = (gVal * 255.0f).toInt().coerceIn(0, 255)
            val bIdx = (bVal * 255.0f).toInt().coerceIn(0, 255)

            val finalR = (masterLut[rIdx] * 255.0f).toInt().coerceIn(0, 255).toByte()
            val finalG = (masterLut[gIdx] * 255.0f).toInt().coerceIn(0, 255).toByte()
            val finalB = (masterLut[bIdx] * 255.0f).toInt().coerceIn(0, 255).toByte()
            val finalA = 255.toByte()

            cachedLutBuffer.put(finalR)
            cachedLutBuffer.put(finalG)
            cachedLutBuffer.put(finalB)
            cachedLutBuffer.put(finalA)
        }

        cachedLutBuffer.flip()
        isDirty = false
        return cachedLutBuffer
    }

    fun reset() {
        val defaultPoints = listOf(CurvePoint(0.0f, 0.0f), CurvePoint(1.0f, 1.0f))
        masterPoints.clear(); masterPoints.addAll(defaultPoints)
        redPoints.clear(); redPoints.addAll(defaultPoints)
        greenPoints.clear(); greenPoints.addAll(defaultPoints)
        bluePoints.clear(); bluePoints.addAll(defaultPoints)
        isDirty = true
    }

    enum class CurvePreset {
        LINEAR,
        S_CURVE,
        HIGH_CONTRAST,
        FADE_MATTE,
        WARM_SHADOWS,
        COOL_HIGHLIGHTS,
        CROSS_PROCESS,
        NEGATIVE
    }

    fun applyPreset(preset: CurvePreset) {
        reset()
        when (preset) {
            CurvePreset.LINEAR -> { /* default */ }
            CurvePreset.S_CURVE -> {
                masterPoints.clear()
                masterPoints.addAll(listOf(
                    CurvePoint(0.0f, 0.0f),
                    CurvePoint(0.25f, 0.18f),
                    CurvePoint(0.50f, 0.50f),
                    CurvePoint(0.75f, 0.82f),
                    CurvePoint(1.0f, 1.0f)
                ))
            }
            CurvePreset.HIGH_CONTRAST -> {
                masterPoints.clear()
                masterPoints.addAll(listOf(
                    CurvePoint(0.0f, 0.0f),
                    CurvePoint(0.20f, 0.10f),
                    CurvePoint(0.50f, 0.50f),
                    CurvePoint(0.80f, 0.90f),
                    CurvePoint(1.0f, 1.0f)
                ))
            }
            CurvePreset.FADE_MATTE -> {
                masterPoints.clear()
                masterPoints.addAll(listOf(
                    CurvePoint(0.0f, 0.12f),
                    CurvePoint(0.30f, 0.28f),
                    CurvePoint(0.70f, 0.72f),
                    CurvePoint(1.0f, 0.92f)
                ))
            }
            CurvePreset.WARM_SHADOWS -> {
                redPoints.clear()
                redPoints.addAll(listOf(CurvePoint(0.0f, 0.08f), CurvePoint(0.5f, 0.52f), CurvePoint(1.0f, 1.0f)))
                bluePoints.clear()
                bluePoints.addAll(listOf(CurvePoint(0.0f, 0.0f), CurvePoint(0.5f, 0.46f), CurvePoint(1.0f, 0.95f)))
            }
            CurvePreset.COOL_HIGHLIGHTS -> {
                bluePoints.clear()
                bluePoints.addAll(listOf(CurvePoint(0.0f, 0.0f), CurvePoint(0.5f, 0.52f), CurvePoint(1.0f, 1.05f.coerceAtMost(1.0f))))
                redPoints.clear()
                redPoints.addAll(listOf(CurvePoint(0.0f, 0.0f), CurvePoint(0.5f, 0.48f), CurvePoint(1.0f, 0.92f)))
            }
            CurvePreset.CROSS_PROCESS -> {
                redPoints.clear()
                redPoints.addAll(listOf(CurvePoint(0.0f, 0.05f), CurvePoint(0.5f, 0.55f), CurvePoint(1.0f, 0.95f)))
                greenPoints.clear()
                greenPoints.addAll(listOf(CurvePoint(0.0f, 0.0f), CurvePoint(0.5f, 0.48f), CurvePoint(1.0f, 1.0f)))
                bluePoints.clear()
                bluePoints.addAll(listOf(CurvePoint(0.0f, 0.15f), CurvePoint(0.5f, 0.42f), CurvePoint(1.0f, 0.85f)))
            }
            CurvePreset.NEGATIVE -> {
                masterPoints.clear()
                masterPoints.addAll(listOf(CurvePoint(0.0f, 1.0f), CurvePoint(1.0f, 0.0f)))
            }
        }
        isDirty = true
    }
}
