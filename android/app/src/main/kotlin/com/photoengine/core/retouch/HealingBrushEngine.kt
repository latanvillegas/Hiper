package com.photoengine.core.retouch

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.*

/**
 * Intelligent Healing Brush with Automatic Donor Sampling.
 * Searches in concentric rings around the defect for optimal donor patches
 * matching local luminance and low gradient variance, then performs seamless blending.
 */
class HealingBrushEngine {

    data class HealingResult(
        val donorX: Float,
        val donorY: Float,
        val confidence: Float
    )

    /**
     * Automatically discovers the best source patch for the target coordinate.
     * Evaluates color variance and edge gradients to avoid sampling across high-contrast edges.
     */
    fun findAutoDonorPatch(
        bitmap: Bitmap,
        targetX: Int,
        targetY: Int,
        radius: Int
    ): HealingResult {
        val w = bitmap.width
        val h = bitmap.height

        val targetMean = computePatchMean(bitmap, targetX, targetY, radius)

        var bestDonorX = targetX + radius * 2
        var bestDonorY = targetY
        var bestScore = Float.MAX_VALUE

        // Search in 16 radial directions at distances of 1.5x, 2.5x, and 3.5x radius
        val distances = listOf(radius * 1.6f, radius * 2.5f, radius * 3.5f)
        val angles = 16

        for (dist in distances) {
            for (i in 0 until angles) {
                val theta = (i * 2.0 * Math.PI / angles).toFloat()
                val candidateX = (targetX + dist * cos(theta)).toInt()
                val candidateY = (targetY + dist * sin(theta)).toInt()

                if (candidateX - radius < 0 || candidateX + radius >= w ||
                    candidateY - radius < 0 || candidateY + radius >= h) {
                    continue
                }

                val candMean = computePatchMean(bitmap, candidateX, candidateY, radius)
                val candVariance = computePatchVariance(bitmap, candidateX, candidateY, radius, candMean)

                // Cost function: difference in luminance + penalty for high variance (edges/moles)
                val lumaDiff = abs(candMean - targetMean)
                val score = lumaDiff * 1.5f + candVariance * 0.8f

                if (score < bestScore) {
                    bestScore = score
                    bestDonorX = candidateX
                    bestDonorY = candidateY
                }
            }
        }

        return HealingResult(
            donorX = bestDonorX.toFloat(),
            donorY = bestDonorY.toFloat(),
            confidence = 1.0f / (1.0f + bestScore * 0.05f)
        )
    }

    /**
     * Blends the donor patch into the target area using gradient-preserving feathering.
     */
    fun applyHeal(
        targetBitmap: Bitmap,
        targetX: Int,
        targetY: Int,
        donorX: Int,
        donorY: Int,
        radius: Int
    ) {
        val w = targetBitmap.width
        val h = targetBitmap.height

        val r2 = radius * radius

        for (dy in -radius..radius) {
            val ty = targetY + dy
            val sy = donorY + dy
            if (ty < 0 || ty >= h || sy < 0 || sy >= h) continue

            for (dx in -radius..radius) {
                val tx = targetX + dx
                val sx = donorX + dx
                if (tx < 0 || tx >= w || sx < 0 || sx >= w) continue

                val dist2 = dx * dx + dy * dy
                if (dist2 > r2) continue

                val dist = sqrt(dist2.toFloat()) / radius
                // Smooth cosine feathering
                val weight = (cos(dist * Math.PI.toFloat()) * 0.5f + 0.5f).coerceIn(0.0f, 1.0f)

                val targetColor = targetBitmap.getPixel(tx, ty)
                val donorColor = targetBitmap.getPixel(sx, sy)

                val tr = (targetColor shr 16) and 0xFF
                val tg = (targetColor shr 8) and 0xFF
                val tb = targetColor and 0xFF

                val sr = (donorColor shr 16) and 0xFF
                val sg = (donorColor shr 8) and 0xFF
                val sb = donorColor and 0xFF

                val outR = (tr * (1.0f - weight) + sr * weight).toInt().coerceIn(0, 255)
                val outG = (tg * (1.0f - weight) + sg * weight).toInt().coerceIn(0, 255)
                val outB = (tb * (1.0f - weight) + sb * weight).toInt().coerceIn(0, 255)

                targetBitmap.setPixel(tx, ty, (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB)
            }
        }
    }

    /**
     * Non-destructive real-time preview of healing for instant UI feedback.
     */
    fun previewHeal(
        sourceBitmap: Bitmap,
        targetX: Int,
        targetY: Int,
        donorX: Int,
        donorY: Int,
        radius: Int
    ): Bitmap {
        val preview = sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        applyHeal(preview, targetX, targetY, donorX, donorY, radius)
        return preview
    }

    /**
     * Executes healing with either automatic donor sampling or explicit manual donor coordinates.
     */
    fun healSpot(
        bitmap: Bitmap,
        targetX: Int,
        targetY: Int,
        radius: Int,
        manualDonorX: Int? = null,
        manualDonorY: Int? = null
    ): HealingResult {
        val donor = if (manualDonorX != null && manualDonorY != null) {
            HealingResult(manualDonorX.toFloat(), manualDonorY.toFloat(), 1.0f)
        } else {
            findAutoDonorPatch(bitmap, targetX, targetY, radius)
        }
        applyHeal(bitmap, targetX, targetY, donor.donorX.toInt(), donor.donorY.toInt(), radius)
        return donor
    }

    private fun computePatchMean(bitmap: Bitmap, cx: Int, cy: Int, r: Int): Float {
        var sum = 0L
        var count = 0
        for (y in max(0, cy - r)..min(bitmap.height - 1, cy + r)) {
            for (x in max(0, cx - r)..min(bitmap.width - 1, cx + r)) {
                val color = bitmap.getPixel(x, y)
                val lum = ((color shr 16 and 0xFF) * 299 + (color shr 8 and 0xFF) * 587 + (color and 0xFF) * 114) / 1000
                sum += lum
                count++
            }
        }
        return if (count > 0) sum.toFloat() / count else 128.0f
    }

    private fun computePatchVariance(bitmap: Bitmap, cx: Int, cy: Int, r: Int, mean: Float): Float {
        var sumSq = 0.0
        var count = 0
        for (y in max(0, cy - r)..min(bitmap.height - 1, cy + r)) {
            for (x in max(0, cx - r)..min(bitmap.width - 1, cx + r)) {
                val color = bitmap.getPixel(x, y)
                val lum = ((color shr 16 and 0xFF) * 299 + (color shr 8 and 0xFF) * 587 + (color and 0xFF) * 114) / 1000
                val diff = lum - mean
                sumSq += diff * diff
                count++
            }
        }
        return if (count > 0) (sumSq / count).toFloat() else 0.0f
    }
}
