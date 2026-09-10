package com.photoengine.core.effects

import android.graphics.Bitmap
import kotlin.math.*

enum class ApertureShape(val bladeCount: Int) {
    CIRCULAR(0),
    HEXAGON(6),
    STAR(5),
    HEART(0),
    PENTAGON(5),
    OCTAGON(8)
}

enum class DepthMapMode {
    MANUAL_RADIAL,
    MANUAL_TILT_SHIFT,
    AUTO_PORTRAIT_DEPTH
}

data class LensBlurConfig(
    val fNumber: Float = 2.8f,               // f/1.4 .. f/22.0
    val blurRadius: Float = 24.0f,           // Bokeh disc radius in px (calculated from fNumber)
    val apertureShape: ApertureShape = ApertureShape.CIRCULAR,
    val bladeCurvature: Float = 0.5f,        // 0.0 straight polygon, 1.0 rounded
    val specularThreshold: Float = 0.70f,    // Highlights threshold for bright bokeh discs
    val specularBoost: Float = 2.0f,         // Glow amplification
    val chromaticAberration: Float = 1.5f,   // Axial color fringe offset
    val depthMode: DepthMapMode = DepthMapMode.MANUAL_RADIAL,
    val focalDistance: Float = 0.5f,         // 0.0 .. 1.0 (focus plane in depth map)
    val focusTransitionWidth: Float = 0.25f  // Depth of field thickness
)

/**
 * Optical Lens Blur Engine with Physically-Based Bokeh Discs.
 * Simulates real camera lens apertures with polygonal blades, specular boost,
 * custom bokeh shapes (circle, hexagon, star, heart), and depth-of-field control.
 */
class LensBlurBokehEngine {

    /**
     * Converts f-stop aperture (e.g. f/1.4, f/2.8, f/5.6, f/22) to Circle of Confusion blur radius.
     */
    fun calculateCoCRadius(fNumber: Float, maxBlurRadius: Float = 40.0f): Float {
        val clampedF = fNumber.coerceIn(1.4f, 22.0f)
        // Inverse linear relationship: larger aperture (lower f/) yields larger bokeh circle
        val factor = ((22.0f - clampedF) / (22.0f - 1.4f)).pow(1.5f)
        return (factor * maxBlurRadius).coerceAtLeast(1.0f)
    }

    fun getUniformPayload(config: LensBlurConfig): FloatArray {
        val effectiveRadius = calculateCoCRadius(config.fNumber, config.blurRadius)
        return floatArrayOf(
            effectiveRadius,
            config.apertureShape.bladeCount.toFloat(),
            config.bladeCurvature,
            config.specularThreshold,
            config.specularBoost,
            config.chromaticAberration,
            config.focalDistance,
            config.focusTransitionWidth
        )
    }

    /**
     * Evaluates bokeh transmission shape mask at normalized offset (dx, dy) inside [-1, 1].
     */
    fun isInsideAperture(dx: Float, dy: Float, shape: ApertureShape, curvature: Float): Float {
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 1.0f) return 0.0f

        when (shape) {
            ApertureShape.CIRCULAR -> {
                return 1.0f - smoothstep(0.92f, 1.0f, dist)
            }
            ApertureShape.HEART -> {
                // Normalized cardioid/heart equation: (x^2 + y^2 - 1)^3 - x^2 * y^3 <= 0
                val hx = dx * 1.3f
                val hy = -dy * 1.3f + 0.3f // Invert Y and shift center
                val a = hx * hx + hy * hy - 0.75f
                val heartEq = a * a * a - hx * hx * hy * hy * hy
                return if (heartEq <= 0.0f) 1.0f else 0.0f
            }
            ApertureShape.STAR -> {
                // 5-pointed star modulation
                val angle = atan2(dy, dx)
                val starMod = 0.55f + 0.45f * cos(5.0f * angle).coerceAtLeast(0.0f)
                return if (dist <= starMod) 1.0f else 0.0f
            }
            else -> {
                // Polygonal blade aperture (Hexagon, Pentagon, Octagon)
                val angle = atan2(dy, dx) + Math.PI.toFloat()
                val sector = (2.0f * Math.PI.toFloat()) / shape.bladeCount
                val localAngle = (angle % sector) - (sector * 0.5f)
                val polyR = cos(sector * 0.5f) / cos(localAngle)

                val blendedR = (1.0f - curvature) * polyR + curvature * 1.0f
                return if (dist <= blendedR) 1.0f else 0.0f
            }
        }
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0.0f, 1.0f)
        return t * t * (3.0f - 2.0f * t)
    }
}
