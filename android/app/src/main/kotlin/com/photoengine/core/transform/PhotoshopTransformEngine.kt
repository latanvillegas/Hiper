package com.photoengine.core.transform

import android.graphics.*
import kotlin.math.*

enum class TransformMode {
    FREE_TRANSFORM,
    SCALE,
    ROTATE,
    SKEW,
    DISTORT,
    PERSPECTIVE,
    WARP
}

enum class AnchorPoint {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    CUSTOM
}

data class NumericalTransformParams(
    var translationX: Float = 0.0f,
    var translationY: Float = 0.0f,
    var scaleX: Float = 1.0f,
    var scaleY: Float = 1.0f,
    var rotationDegrees: Float = 0.0f,
    var skewHorizontalDeg: Float = 0.0f,
    var skewVerticalDeg: Float = 0.0f,
    var anchor: AnchorPoint = AnchorPoint.CENTER,
    var customAnchorX: Float = 0.0f,
    var customAnchorY: Float = 0.0f
)

/**
 * 3x3 or 4x4 Bézier Warp Grid for Photoshop Mesh Warp.
 */
data class WarpMesh(
    val rows: Int = 3,
    val cols: Int = 3,
    val points: Array<Array<PointF>>
) {
    companion object {
        fun createDefault(width: Float, height: Float, rows: Int = 3, cols: Int = 3): WarpMesh {
            val grid = Array(rows + 1) { r ->
                Array(cols + 1) { c ->
                    PointF((c.toFloat() / cols) * width, (r.toFloat() / rows) * height)
                }
            }
            return WarpMesh(rows, cols, grid)
        }
    }
}

/**
 * Photoshop Transformation Engine.
 * Supports:
 * - Free Transform (Affine Matrix + 4-point Projective Quad Homography)
 * - Scale, Rotate, Skew, Distort, Perspective
 * - Adjustable reference/pivot anchor point
 * - Numerical precision transformation
 * - "Transform Again" (Ctrl+Shift+T) cached repeatable operation
 * - Bicubic mesh warp rasterization
 */
class PhotoshopTransformEngine {

    var lastAppliedTransform: NumericalTransformParams? = null
        private set

    /**
     * Calculates pivot point in source bitmap coordinate space.
     */
    fun calculatePivot(width: Float, height: Float, params: NumericalTransformParams): PointF {
        return when (params.anchor) {
            AnchorPoint.TOP_LEFT -> PointF(0f, 0f)
            AnchorPoint.TOP_CENTER -> PointF(width * 0.5f, 0f)
            AnchorPoint.TOP_RIGHT -> PointF(width, 0f)
            AnchorPoint.CENTER_LEFT -> PointF(0f, height * 0.5f)
            AnchorPoint.CENTER -> PointF(width * 0.5f, height * 0.5f)
            AnchorPoint.CENTER_RIGHT -> PointF(width, height * 0.5f)
            AnchorPoint.BOTTOM_LEFT -> PointF(0f, height)
            AnchorPoint.BOTTOM_CENTER -> PointF(width * 0.5f, height)
            AnchorPoint.BOTTOM_RIGHT -> PointF(width, height)
            AnchorPoint.CUSTOM -> PointF(params.customAnchorX, params.customAnchorY)
        }
    }

    /**
     * Builds affine transformation matrix from precise numerical parameters.
     */
    fun buildTransformMatrix(sourceWidth: Float, sourceHeight: Float, params: NumericalTransformParams): Matrix {
        val pivot = calculatePivot(sourceWidth, sourceHeight, params)
        val matrix = Matrix()

        // Translate to origin
        matrix.preTranslate(-pivot.x, -pivot.y)

        // Skew
        val kx = tan(Math.toRadians(params.skewHorizontalDeg.toDouble())).toFloat()
        val ky = tan(Math.toRadians(params.skewVerticalDeg.toDouble())).toFloat()
        matrix.postSkew(kx, ky)

        // Scale
        matrix.postScale(params.scaleX, params.scaleY)

        // Rotate
        matrix.postRotate(params.rotationDegrees)

        // Translate back and apply global displacement
        matrix.postTranslate(pivot.x + params.translationX, pivot.y + params.translationY)

        return matrix
    }

    /**
     * Applies numerical transformation and updates "Transform Again" history cache.
     */
    fun applyTransform(
        source: Bitmap,
        params: NumericalTransformParams,
        targetWidth: Int = source.width,
        targetHeight: Int = source.height
    ): Bitmap {
        val matrix = buildTransformMatrix(source.width.toFloat(), source.height.toFloat(), params)
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        lastAppliedTransform = params.copy()
        return output
    }

    /**
     * Executes "Transform Again" (Ctrl+Shift+T / Cmd+Shift+T).
     */
    fun transformAgain(source: Bitmap): Bitmap? {
        val cached = lastAppliedTransform ?: return null
        return applyTransform(source, cached)
    }

    /**
     * 4-Corner Projective Quad Homography (Photoshop Distort & Perspective modes).
     */
    fun applyDistortPerspective(
        source: Bitmap,
        dstCorners: FloatArray, // [TLx, TLy, TRx, TRy, BRx, BRy, BLx, BLy]
        targetWidth: Int = source.width,
        targetHeight: Int = source.height
    ): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        val srcCorners = floatArrayOf(
            0f, 0f,
            w, 0f,
            w, h,
            0f, h
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }

    /**
     * Photoshop Mesh Warp (Deformar con rejilla Bézier 3x3).
     * Renders quadrilateral subdivision patches with piecewise bilinear/homographic mapping.
     */
    fun applyMeshWarp(
        source: Bitmap,
        mesh: WarpMesh,
        targetWidth: Int = source.width,
        targetHeight: Int = source.height
    ): Bitmap {
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val origW = source.width.toFloat()
        val origH = source.height.toFloat()
        val matrix = Matrix()

        for (r in 0 until mesh.rows) {
            for (c in 0 until mesh.cols) {
                val srcTL = floatArrayOf((c.toFloat() / mesh.cols) * origW, (r.toFloat() / mesh.rows) * origH)
                val srcTR = floatArrayOf(((c + 1).toFloat() / mesh.cols) * origW, (r.toFloat() / mesh.rows) * origH)
                val srcBR = floatArrayOf(((c + 1).toFloat() / mesh.cols) * origW, ((r + 1).toFloat() / mesh.rows) * origH)
                val srcBL = floatArrayOf((c.toFloat() / mesh.cols) * origW, ((r + 1).toFloat() / mesh.rows) * origH)

                val srcCorners = floatArrayOf(
                    srcTL[0], srcTL[1],
                    srcTR[0], srcTR[1],
                    srcBR[0], srcBR[1],
                    srcBL[0], srcBL[1]
                )

                val ptTL = mesh.points[r][c]
                val ptTR = mesh.points[r][c + 1]
                val ptBR = mesh.points[r + 1][c + 1]
                val ptBL = mesh.points[r + 1][c]

                val dstCorners = floatArrayOf(
                    ptTL.x, ptTL.y,
                    ptTR.x, ptTR.y,
                    ptBR.x, ptBR.y,
                    ptBL.x, ptBL.y
                )

                matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

                canvas.save()
                val clipPath = Path().apply {
                    moveTo(ptTL.x, ptTL.y)
                    lineTo(ptTR.x, ptTR.y)
                    lineTo(ptBR.x, ptBR.y)
                    lineTo(ptBL.x, ptBL.y)
                    close()
                }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(source, matrix, paint)
                canvas.restore()
            }
        }

        return output
    }
}
