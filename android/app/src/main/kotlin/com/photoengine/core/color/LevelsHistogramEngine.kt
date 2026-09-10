package com.photoengine.core.color

import java.nio.IntBuffer
import kotlin.math.ln
import kotlin.math.pow

enum class LevelChannel {
    RGB_MASTER,
    RED,
    GREEN,
    BLUE
}

data class LevelsConfig(
    val inputBlack: Float = 0.0f,     // 0.0 .. 1.0 (typical 0)
    val inputMidGamma: Float = 1.0f,  // 0.1 .. 10.0 (typical 1.0)
    val inputWhite: Float = 1.0f,     // 0.0 .. 1.0 (typical 1.0)
    val outputBlack: Float = 0.0f,    // 0.0 .. 1.0 (typical 0)
    val outputWhite: Float = 1.0f     // 0.0 .. 1.0 (typical 1.0)
)

data class HistogramData(
    val redBins: IntArray = IntArray(256),
    val greenBins: IntArray = IntArray(256),
    val blueBins: IntArray = IntArray(256),
    val lumaBins: IntArray = IntArray(256),
    val maxCount: Int = 1
)

/**
 * Real-time Histogram Generator and Levels Adjustment Module.
 * Designed for 30+ FPS low-latency feedback.
 * Supports individual adjustments for RGB Master, Red, Green, and Blue channels.
 */
class LevelsHistogramEngine {

    var config: LevelsConfig
        get() = channelConfigs[LevelChannel.RGB_MASTER] ?: LevelsConfig()
        set(value) {
            channelConfigs[LevelChannel.RGB_MASTER] = value
        }

    val channelConfigs = mutableMapOf(
        LevelChannel.RGB_MASTER to LevelsConfig(),
        LevelChannel.RED to LevelsConfig(),
        LevelChannel.GREEN to LevelsConfig(),
        LevelChannel.BLUE to LevelsConfig()
    )

    fun setChannelConfig(channel: LevelChannel, cfg: LevelsConfig) {
        channelConfigs[channel] = cfg
    }

    fun getChannelConfig(channel: LevelChannel): LevelsConfig {
        return channelConfigs[channel] ?: LevelsConfig()
    }

    /**
     * Maps an input normalized pixel intensity through the Levels transfer function.
     * transfer(x) = outputBlack + (outputWhite - outputBlack) * (((x - inputBlack)/(inputWhite - inputBlack)) ^ (1/gamma))
     */
    fun evaluateLevel(x: Float, cfg: LevelsConfig = config): Float {
        val range = (cfg.inputWhite - cfg.inputBlack).coerceAtLeast(0.001f)
        val normalized = ((x - cfg.inputBlack) / range).coerceIn(0.0f, 1.0f)
        val gammaCorrected = normalized.pow(1.0f / cfg.inputMidGamma.coerceAtLeast(0.01f))
        return cfg.outputBlack + (cfg.outputWhite - cfg.outputBlack) * gammaCorrected
    }

    /**
     * Applies channel-specific levels followed by RGB Master levels.
     */
    fun processRgb(r: Float, g: Float, b: Float): FloatArray {
        val rLvl = evaluateLevel(r, channelConfigs[LevelChannel.RED] ?: LevelsConfig())
        val gLvl = evaluateLevel(g, channelConfigs[LevelChannel.GREEN] ?: LevelsConfig())
        val bLvl = evaluateLevel(b, channelConfigs[LevelChannel.BLUE] ?: LevelsConfig())

        val master = channelConfigs[LevelChannel.RGB_MASTER] ?: LevelsConfig()
        return floatArrayOf(
            evaluateLevel(rLvl, master).coerceIn(0.0f, 1.0f),
            evaluateLevel(gLvl, master).coerceIn(0.0f, 1.0f),
            evaluateLevel(bLvl, master).coerceIn(0.0f, 1.0f)
        )
    }

    /**
     * Calculates 256-bin histogram for CPU preview or fallback mode.
     * On GPU, this calculation runs inside the `levels_histogram.comp` compute shader in < 0.8ms.
     */
    fun calculateHistogramFromPixels(pixels: IntArray, width: Int, height: Int): HistogramData {
        val r = IntArray(256)
        val g = IntArray(256)
        val b = IntArray(256)
        val luma = IntArray(256)
        var maxCount = 1

        val step = if (pixels.size > 2_000_000) 4 else 1 // Subsample large preview frames for 60fps responsiveness
        for (i in pixels.indices step step) {
            val color = pixels[i]
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            val y = (0.2126f * red + 0.7152f * green + 0.0722f * blue).toInt().coerceIn(0, 255)

            r[red]++
            g[green]++
            b[blue]++
            luma[y]++

            if (luma[y] > maxCount) maxCount = luma[y]
        }

        return HistogramData(r, g, b, luma, maxCount)
    }
}
