package com.photoengine.core.face

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.*

data class FaceSlimmingConfig(
    val jawSlimming: Float = 0.0f,     // 0.0 .. 1.0
    val cheekNarrowing: Float = 0.0f,  // 0.0 .. 1.0
    val chinSharpening: Float = 0.0f,  // 0.0 .. 1.0
    val noseSlimming: Float = 0.0f,    // 0.0 .. 1.0
    val eyeEnlargement: Float = 0.0f   // 0.0 .. 1.0
)

/**
 * 2D Warp Mesh Deformation Engine for Anatomical Face Slimming.
 * Uses Radial Basis Function (RBF) vector displacement fields derived from ML Kit 468 landmarks.
 * Modifies coordinates prior to texture sampling to provide 60 FPS real-time deformation.
 */
class FaceSlimmingWarpEngine {

    data class WarpVector(
        val center: PointF,
        val radius: Float,
        val direction: PointF,
        val intensity: Float
    )

    /**
     * Builds list of radial warp vectors corresponding to face landmarks and slimming parameters.
     */
    fun computeWarpVectors(landmarks: FaceLandmarks468, config: FaceSlimmingConfig): List<WarpVector> {
        val vectors = mutableListOf<WarpVector>()
        val pts = landmarks.allPoints
        if (pts.size < 468) return vectors

        // Chin tip index: 152, Nose tip index: 1, Left cheek index: 234, Right cheek index: 454
        val noseTip = pts[1]
        val chin = pts[152]
        val leftJaw = pts[234]
        val rightJaw = pts[454]

        val faceWidth = abs(rightJaw.x - leftJaw.x).coerceAtLeast(50f)
        val faceHeight = abs(chin.y - pts[10].y).coerceAtLeast(50f)

        // 1. Jawline & Cheek slimming vectors: push inwards towards central vertical axis
        if (config.jawSlimming > 0.01f) {
            val jawInward = faceWidth * 0.08f * config.jawSlimming
            // Left jaw points push right (+x)
            vectors.add(WarpVector(leftJaw, faceWidth * 0.35f, PointF(1.0f, -0.1f), jawInward))
            // Right jaw points push left (-x)
            vectors.add(WarpVector(rightJaw, faceWidth * 0.35f, PointF(-1.0f, -0.1f), jawInward))
        }

        // 2. Cheek narrowing: push cheekbones slightly inwards and upwards
        if (config.cheekNarrowing > 0.01f) {
            val cheekInward = faceWidth * 0.06f * config.cheekNarrowing
            val leftCheek = pts[116] // Left zygomatic arch
            val rightCheek = pts[345] // Right zygomatic arch
            vectors.add(WarpVector(leftCheek, faceWidth * 0.28f, PointF(1.0f, 0.0f), cheekInward))
            vectors.add(WarpVector(rightCheek, faceWidth * 0.28f, PointF(-1.0f, 0.0f), cheekInward))
        }

        // 3. Chin sharpening: push chin upward
        if (config.chinSharpening > 0.01f) {
            val chinLift = faceHeight * 0.05f * config.chinSharpening
            vectors.add(WarpVector(chin, faceWidth * 0.25f, PointF(0.0f, -1.0f), chinLift))
        }

        // 4. Nose slimming
        if (config.noseSlimming > 0.01f) {
            val nosePush = faceWidth * 0.035f * config.noseSlimming
            val leftAlar = pts[98]
            val rightAlar = pts[327]
            vectors.add(WarpVector(leftAlar, faceWidth * 0.12f, PointF(1.0f, 0.0f), nosePush))
            vectors.add(WarpVector(rightAlar, faceWidth * 0.12f, PointF(-1.0f, 0.0f), nosePush))
        }

        return vectors
    }

    /**
     * Calculates the deformed texture coordinate (u', v') from original (u, v).
     */
    fun evaluateDisplacement(x: Float, y: Float, warps: List<WarpVector>): PointF {
        var dx = 0.0f
        var dy = 0.0f

        for (w in warps) {
            val vx = x - w.center.x
            val vy = y - w.center.y
            val d2 = vx * vx + vy * vy
            val r2 = w.radius * w.radius

            if (d2 < r2) {
                // Smooth polynomial bell decay (1 - d^2/r^2)^2
                val factor = (1.0f - d2 / r2)
                val weight = factor * factor * w.intensity

                dx += w.direction.x * weight
                dy += w.direction.y * weight
            }
        }

        return PointF(x - dx, y - dy)
    }
}
