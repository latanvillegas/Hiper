package com.photoengine.core.face

import android.graphics.*
import kotlin.math.*

data class VirtualMakeupConfig(
    // 1. Foundation: tone evening and shine/specular reduction
    val foundationIntensity: Float = 0.0f,
    val foundationToneR: Float = 0.94f,
    val foundationToneG: Float = 0.82f,
    val foundationToneB: Float = 0.73f,

    // 2. Contour: jawline & cheekbone shadow definition
    val contourIntensity: Float = 0.0f,

    // 3. Blush: cheek warmth
    val blushIntensity: Float = 0.0f,
    val blushColor: Int = Color.argb(200, 235, 95, 115),

    // 4. Lipstick: color + gloss specular highlight
    val lipstickIntensity: Float = 0.0f,
    val lipstickColor: Int = Color.argb(220, 205, 30, 60),
    val lipstickGloss: Float = 0.45f,

    // 5. Eyeshadow: eyelid gradient
    val eyeshadowIntensity: Float = 0.0f,
    val eyeshadowColor: Int = Color.argb(180, 140, 80, 100),

    // 6. Eyeliner: dark defined lash line
    val eyelinerIntensity: Float = 0.0f,

    // 7. Mascara: lash volume and length
    val mascaraIntensity: Float = 0.0f,

    // 8. Eyebrows: brow tint and density fill
    val eyebrowIntensity: Float = 0.0f,
    val eyebrowColor: Int = Color.argb(200, 45, 35, 30)
)

/**
 * Professional Virtual Makeup Compositing Suite.
 * Accurately projects cosmetic layers onto 3D face mesh polygons with
 * realistic specular lighting, diffuse blending, and edge feathering.
 */
class VirtualMakeupEngine {

    /**
     * Renders all makeup layers into a transparent ARGB overlay bitmap
     * to be blended with the photo using standard Soft Light and Multiply GPU blend modes.
     */
    fun renderMakeupOverlay(
        width: Int,
        height: Int,
        landmarks: FaceLandmarks468,
        config: VirtualMakeupConfig
    ): Bitmap {
        val overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlay)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isDither = true }

        val pts = landmarks.allPoints
        if (pts.size < 468) return overlay

        // 1. Foundation (Subtle skin tone unify pass across face contour)
        if (config.foundationIntensity > 0.01f) {
            renderFoundation(canvas, pts, landmarks.jawlineIndices, config)
        }

        // 2. Contouring (Jawline and cheek hollow shading)
        if (config.contourIntensity > 0.01f) {
            renderContour(canvas, pts, config)
        }

        // 3. Blush (Radial gradient on cheekbones)
        if (config.blushIntensity > 0.01f) {
            renderBlush(canvas, pts, config)
        }

        // 4. Eyeshadow (Upper eyelid soft gradient)
        if (config.eyeshadowIntensity > 0.01f) {
            renderEyeshadow(canvas, pts, config)
        }

        // 5. Eyeliner & Mascara (Upper and lower lash line)
        if (config.eyelinerIntensity > 0.01f || config.mascaraIntensity > 0.01f) {
            renderEyelinerAndMascara(canvas, pts, landmarks, config)
        }

        // 6. Eyebrows (Stroke filling and tint)
        if (config.eyebrowIntensity > 0.01f) {
            renderEyebrows(canvas, pts, landmarks, config)
        }

        // 7. Lipstick (Outer lips fill + gloss specular highlight)
        if (config.lipstickIntensity > 0.01f) {
            renderLipstick(canvas, pts, landmarks, config)
        }

        return overlay
    }

    private fun renderFoundation(canvas: Canvas, pts: List<PointF>, jawline: IntArray, config: VirtualMakeupConfig) {
        val path = Path()
        for (i in jawline.indices) {
            val p = pts[jawline[i]]
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                (config.foundationIntensity * 60).toInt().coerceIn(0, 255),
                (config.foundationToneR * 255).toInt(),
                (config.foundationToneG * 255).toInt(),
                (config.foundationToneB * 255).toInt()
            )
            maskFilter = BlurMaskFilter(25.0f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
    }

    private fun renderContour(canvas: Canvas, pts: List<PointF>, config: VirtualMakeupConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((config.contourIntensity * 70).toInt().coerceIn(0, 255), 65, 45, 35)
            maskFilter = BlurMaskFilter(20.0f, BlurMaskFilter.Blur.NORMAL)
        }
        // Left & Right cheek hollows
        val leftCheek = pts[116]
        val rightCheek = pts[345]
        canvas.drawCircle(leftCheek.x, leftCheek.y + 15f, 30f, paint)
        canvas.drawCircle(rightCheek.x, rightCheek.y + 15f, 30f, paint)
    }

    private fun renderBlush(canvas: Canvas, pts: List<PointF>, config: VirtualMakeupConfig) {
        val leftBlushCenter = pts[205] // Left cheek apple
        val rightBlushCenter = pts[425] // Right cheek apple
        val radius = abs(pts[454].x - pts[234].x) * 0.12f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.blushColor
            alpha = (config.blushIntensity * 160).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(radius * 0.6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(leftBlushCenter.x, leftBlushCenter.y, radius, paint)
        canvas.drawCircle(rightBlushCenter.x, rightBlushCenter.y, radius, paint)
    }

    private fun renderEyeshadow(canvas: Canvas, pts: List<PointF>, config: VirtualMakeupConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.eyeshadowColor
            alpha = (config.eyeshadowIntensity * 150).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(12.0f, BlurMaskFilter.Blur.NORMAL)
        }

        // Left eyelid crease
        val leftEyelid = pts[223]
        val rightEyelid = pts[443]
        val eyeWidth = abs(pts[263].x - pts[362].x) * 0.8f

        canvas.drawCircle(leftEyelid.x, leftEyelid.y, eyeWidth * 0.5f, paint)
        canvas.drawCircle(rightEyelid.x, rightEyelid.y, eyeWidth * 0.5f, paint)
    }

    private fun renderEyelinerAndMascara(
        canvas: Canvas,
        pts: List<PointF>,
        landmarks: FaceLandmarks468,
        config: VirtualMakeupConfig
    ) {
        val totalAlpha = ((config.eyelinerIntensity * 0.6f + config.mascaraIntensity * 0.4f) * 220).toInt().coerceIn(0, 255)
        val strokeW = 2.0f + config.eyelinerIntensity * 2.5f + config.mascaraIntensity * 1.5f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = totalAlpha
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap = Paint.Cap.ROUND
        }

        fun drawLashLine(indices: IntArray) {
            val path = Path()
            for (i in 0 until min(indices.size, 10)) {
                val p = pts[indices[i]]
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            canvas.drawPath(path, paint)
        }

        drawLashLine(landmarks.leftEyeIndices)
        drawLashLine(landmarks.rightEyeIndices)
    }

    private fun renderEyebrows(
        canvas: Canvas,
        pts: List<PointF>,
        landmarks: FaceLandmarks468,
        config: VirtualMakeupConfig
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.eyebrowColor
            alpha = (config.eyebrowIntensity * 160).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = 5.0f
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(4.0f, BlurMaskFilter.Blur.NORMAL)
        }

        fun drawBrow(indices: IntArray) {
            val path = Path()
            for (i in indices.indices) {
                val p = pts[indices[i]]
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            canvas.drawPath(path, paint)
        }

        drawBrow(landmarks.leftEyebrowIndices)
        drawBrow(landmarks.rightEyebrowIndices)
    }

    private fun renderLipstick(
        canvas: Canvas,
        pts: List<PointF>,
        landmarks: FaceLandmarks468,
        config: VirtualMakeupConfig
    ) {
        val path = Path()
        val indices = landmarks.lipsIndices
        for (i in indices.indices) {
            val p = pts[indices[i]]
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.lipstickColor
            alpha = (config.lipstickIntensity * 190).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(3.0f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)

        // Gloss highlight
        if (config.lipstickGloss > 0.05f) {
            val lowerLipCenter = pts[14]
            val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = (config.lipstickIntensity * config.lipstickGloss * 180).toInt().coerceIn(0, 255)
                maskFilter = BlurMaskFilter(5.0f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(lowerLipCenter.x, lowerLipCenter.y - 2f, 6.0f, glossPaint)
        }
    }
}
