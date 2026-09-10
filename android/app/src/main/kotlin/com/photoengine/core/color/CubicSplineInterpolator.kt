package com.photoengine.core.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Control point in a tonal/color curve normalized to [0.0, 1.0].
 */
data class CurvePoint(
    val x: Float,
    val y: Float
) : Comparable<CurvePoint> {
    override fun compareTo(other: CurvePoint): Int = this.x.compareTo(other.x)
}

/**
 * High-performance Monotone Cubic Hermite Spline (Fritsch-Carlson) & Natural Spline Interpolator.
 * Designed specifically for photographic RGB curves with up to 14 control points.
 * Guarantees zero Runge overshoot, strict monotonicity when points are monotonic,
 * and seamless C1 continuity.
 */
class CubicSplineInterpolator {

    /**
     * Evaluates a curve defined by up to 14 control points and writes a 256-element 1D LUT.
     * LUT values are strictly clamped to [0, 255] (or normalized [0.0f, 1.0f]).
     */
    fun generateLut256(points: List<CurvePoint>): FloatArray {
        val lut = FloatArray(256)
        if (points.isEmpty()) {
            for (i in 0..255) lut[i] = i / 255.0f
            return lut
        }

        // Clean, sort, and ensure boundary anchor points at x=0.0 and x=1.0
        val sortedPoints = points.toMutableList().apply {
            sort()
            if (first().x > 0.0f) add(0, CurvePoint(0.0f, first().y))
            if (last().x < 1.0f) add(CurvePoint(1.0f, last().y))
        }

        // Deduplicate points with identical or near-identical X coordinates
        val cleanPoints = mutableListOf<CurvePoint>()
        for (p in sortedPoints) {
            if (cleanPoints.isEmpty() || abs(p.x - cleanPoints.last().x) >= 0.001f) {
                cleanPoints.add(p)
            }
        }

        val n = cleanPoints.size
        if (n == 1) {
            val v = cleanPoints[0].y.coerceIn(0.0f, 1.0f)
            lut.fill(v)
            return lut
        }

        val x = FloatArray(n) { cleanPoints[it].x }
        val y = FloatArray(n) { cleanPoints[it].y.coerceIn(0.0f, 1.0f) }

        // Fritsch-Carlson algorithm for monotonic cubic spline
        val delta = FloatArray(n - 1)
        val m = FloatArray(n)

        for (i in 0 until n - 1) {
            val h = x[i + 1] - x[i]
            delta[i] = if (h != 0.0f) (y[i + 1] - y[i]) / h else 0.0f
        }

        // Initial tangents
        m[0] = delta[0]
        for (i in 1 until n - 1) {
            m[i] = (delta[i - 1] + delta[i]) * 0.5f
        }
        m[n - 1] = delta[n - 2]

        // Monotonicity enforcement
        for (i in 0 until n - 1) {
            if (delta[i] == 0.0f) {
                m[i] = 0.0f
                m[i + 1] = 0.0f
            } else {
                val alpha = m[i] / delta[i]
                val beta = m[i + 1] / delta[i]
                val s = alpha * alpha + beta * beta
                if (s > 9.0f) {
                    val tau = 3.0f / sqrt(s)
                    m[i] = tau * alpha * delta[i]
                    m[i + 1] = tau * beta * delta[i]
                }
            }
        }

        // Interpolate across all 256 discrete bins
        var segIndex = 0
        for (i in 0..255) {
            val tX = i / 255.0f
            while (segIndex < n - 2 && tX > x[segIndex + 1]) {
                segIndex++
            }

            val h = x[segIndex + 1] - x[segIndex]
            if (h <= 0.00001f) {
                lut[i] = y[segIndex]
                continue
            }

            val t = (tX - x[segIndex]) / h
            val t2 = t * t
            val t3 = t2 * t

            // Hermite basis functions
            val h00 = 2 * t3 - 3 * t2 + 1
            val h10 = t3 - 2 * t2 + t
            val h01 = -2 * t3 + 3 * t2
            val h11 = t3 - t2

            val interpolated = h00 * y[segIndex] +
                    h10 * h * m[segIndex] +
                    h01 * y[segIndex + 1] +
                    h11 * h * m[segIndex + 1]

            lut[i] = interpolated.coerceIn(0.0f, 1.0f)
        }

        return lut
    }
}
