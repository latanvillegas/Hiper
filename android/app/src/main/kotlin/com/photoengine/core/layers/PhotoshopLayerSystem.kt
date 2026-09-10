package com.photoengine.core.layers

import android.graphics.*
import java.util.*
import kotlin.math.*

/**
 * Full Photoshop Blend Mode Enumeration (25 modes covering all 6 standard groups:
 * Normal, Darken, Lighten, Contrast, Inversion/Cancellation, Component/HSL).
 */
enum class PhotoshopBlendMode(val displayName: String, val category: String) {
    // Normal Group
    NORMAL("Normal", "Normal"),
    DISSOLVE("Dissolve", "Normal"),

    // Darken Group
    DARKEN("Darken", "Darken"),
    MULTIPLY("Multiply", "Darken"),
    COLOR_BURN("Color Burn", "Darken"),
    LINEAR_BURN("Linear Burn", "Darken"),

    // Lighten Group
    LIGHTEN("Lighten", "Lighten"),
    SCREEN("Screen", "Lighten"),
    COLOR_DODGE("Color Dodge", "Lighten"),
    LINEAR_DODGE_ADD("Linear Dodge (Add)", "Lighten"),

    // Contrast Group
    OVERLAY("Overlay", "Contrast"),
    SOFT_LIGHT("Soft Light", "Contrast"),
    HARD_LIGHT("Hard Light", "Contrast"),
    VIVID_LIGHT("Vivid Light", "Contrast"),
    LINEAR_LIGHT("Linear Light", "Contrast"),
    PIN_LIGHT("Pin Light", "Contrast"),
    HARD_MIX("Hard Mix", "Contrast"),

    // Inversion / Cancellation Group
    DIFFERENCE("Difference", "Inversion"),
    EXCLUSION("Exclusion", "Inversion"),
    SUBTRACT("Subtract", "Inversion"),
    DIVIDE("Divide", "Inversion"),

    // Component / HSL Group
    HUE("Hue", "Component"),
    SATURATION("Saturation", "Component"),
    COLOR("Color", "Component"),
    LUMINOSITY("Luminosity", "Component")
}

data class LayerLocks(
    var lockTransparency: Boolean = false,
    var lockPosition: Boolean = false,
    var lockAll: Boolean = false
)

enum class LayerType {
    PIXEL,
    ADJUSTMENT,
    VECTOR_SHAPE,
    TEXT,
    GROUP
}

/**
 * Photoshop Layer representation with full layer stack capabilities:
 * - Unlimited layers
 * - Clipping mask support
 * - Raster layer mask (White reveals, Black conceals) with link/unlink toggle
 * - Vector path mask
 * - Opacity (0-100%) and Fill Opacity (0-100%)
 * - Layer locking flags
 */
data class PhotoshopLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var type: LayerType = LayerType.PIXEL,
    var bitmap: Bitmap,
    var opacity: Float = 1.0f,            // 0.0 .. 1.0 (Master layer opacity)
    var fillOpacity: Float = 1.0f,        // 0.0 .. 1.0 (Content fill opacity, preserves layer styles)
    var blendMode: PhotoshopBlendMode = PhotoshopBlendMode.NORMAL,
    var isVisible: Boolean = true,
    var locks: LayerLocks = LayerLocks(),
    var isClippingMask: Boolean = false,  // Clips to the base layer immediately below
    var maskBitmap: Bitmap? = null,       // ALPHA_8 bitmap (255 = reveal, 0 = conceal)
    var isMaskEnabled: Boolean = true,
    var isMaskLinked: Boolean = true,     // When false, layer can move without moving mask
    var vectorMaskPath: Path? = null,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
) {
    fun duplicate(): PhotoshopLayer {
        val dupBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val dupMask = maskBitmap?.copy(Bitmap.Config.ALPHA_8, true)
        val dupPath = vectorMaskPath?.let { Path(it) }
        return PhotoshopLayer(
            name = "$name copy",
            type = type,
            bitmap = dupBitmap,
            opacity = opacity,
            fillOpacity = fillOpacity,
            blendMode = blendMode,
            isVisible = isVisible,
            locks = locks.copy(),
            isClippingMask = isClippingMask,
            maskBitmap = dupMask,
            isMaskEnabled = isMaskEnabled,
            isMaskLinked = isMaskLinked,
            vectorMaskPath = dupPath,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    /**
     * Inverts the raster layer mask: 255 - pixel.
     */
    fun invertMask() {
        val mask = maskBitmap ?: return
        val w = mask.width
        val h = mask.height
        val pixels = IntArray(w * h)
        mask.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val a = (pixels[i] ushr 24) and 0xFF
            val inv = 255 - a
            pixels[i] = (inv shl 24) or (inv shl 16) or (inv shl 8) or inv
        }
        mask.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    /**
     * Fills the mask completely with white (reveal all) or black (hide all).
     */
    fun fillMask(colorValue: Int) {
        val mask = maskBitmap ?: return
        val canvas = Canvas(mask)
        canvas.drawColor(colorValue, PorterDuff.Mode.SRC)
    }
}

/**
 * Production-ready Photoshop Layer Engine.
 * Manages layer stack operations, reordering, duplicate, delete, merge down,
 * and high-performance multi-layer pixel compositing with clipping masks.
 */
class PhotoshopLayerSystem(val canvasWidth: Int, val canvasHeight: Int) {

    private val _layers = mutableListOf<PhotoshopLayer>()
    val layers: List<PhotoshopLayer> get() = _layers

    var activeLayerIndex: Int = 0
        set(value) {
            field = value.coerceIn(0, (_layers.size - 1).coerceAtLeast(0))
        }

    val activeLayer: PhotoshopLayer?
        get() = if (_layers.isNotEmpty() && activeLayerIndex in _layers.indices) _layers[activeLayerIndex] else null

    init {
        // Initialize with default background layer
        val bgBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bgBitmap)
        canvas.drawColor(Color.WHITE)
        val bgLayer = PhotoshopLayer(
            name = "Background",
            bitmap = bgBitmap,
            locks = LayerLocks(lockPosition = true)
        )
        _layers.add(bgLayer)
    }

    // --- Stack Manipulation ---

    fun addLayer(layer: PhotoshopLayer, atIndex: Int = activeLayerIndex + 1) {
        val index = atIndex.coerceIn(0, _layers.size)
        _layers.add(index, layer)
        activeLayerIndex = index
    }

    fun createEmptyPixelLayer(name: String = "Layer ${_layers.size + 1}"): PhotoshopLayer {
        val bmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val layer = PhotoshopLayer(name = name, bitmap = bmp)
        addLayer(layer)
        return layer
    }

    fun duplicateLayer(index: Int = activeLayerIndex): PhotoshopLayer? {
        if (index !in _layers.indices) return null
        val original = _layers[index]
        val duplicate = original.duplicate()
        _layers.add(index + 1, duplicate)
        activeLayerIndex = index + 1
        return duplicate
    }

    fun deleteLayer(index: Int = activeLayerIndex): Boolean {
        if (_layers.size <= 1 || index !in _layers.indices) return false
        val removed = _layers.removeAt(index)
        removed.bitmap.recycle()
        removed.maskBitmap?.recycle()
        activeLayerIndex = (index - 1).coerceAtLeast(0)
        return true
    }

    fun moveLayerUp(index: Int = activeLayerIndex) {
        if (index < _layers.size - 1) {
            Collections.swap(_layers, index, index + 1)
            activeLayerIndex = index + 1
        }
    }

    fun moveLayerDown(index: Int = activeLayerIndex) {
        if (index > 0) {
            Collections.swap(_layers, index, index - 1)
            activeLayerIndex = index - 1
        }
    }

    fun bringLayerToFront(index: Int = activeLayerIndex) {
        if (index in 0 until _layers.size - 1) {
            val layer = _layers.removeAt(index)
            _layers.add(layer)
            activeLayerIndex = _layers.size - 1
        }
    }

    fun sendLayerToBack(index: Int = activeLayerIndex) {
        if (index > 0 && index < _layers.size) {
            val layer = _layers.removeAt(index)
            _layers.add(0, layer)
            activeLayerIndex = 0
        }
    }

    /**
     * Merge current layer down into the layer beneath it.
     */
    fun mergeDown(index: Int = activeLayerIndex): Boolean {
        if (index <= 0 || index !in _layers.indices) return false
        val upperLayer = _layers[index]
        val lowerLayer = _layers[index - 1]

        if (lowerLayer.locks.lockAll) return false

        val merged = compositeTwoLayers(lowerLayer, upperLayer)
        lowerLayer.bitmap.recycle()
        lowerLayer.bitmap = merged

        _layers.removeAt(index)
        upperLayer.bitmap.recycle()
        upperLayer.maskBitmap?.recycle()
        activeLayerIndex = index - 1
        return true
    }

    /**
     * Flatten entire image into a single Background layer.
     */
    fun flattenImage(): PhotoshopLayer {
        val composite = compositeAllLayers()
        // Recycle old layer bitmaps
        for (layer in _layers) {
            layer.bitmap.recycle()
            layer.maskBitmap?.recycle()
        }
        _layers.clear()

        val flattened = PhotoshopLayer(
            name = "Background",
            bitmap = composite,
            locks = LayerLocks(lockPosition = true)
        )
        _layers.add(flattened)
        activeLayerIndex = 0
        return flattened
    }

    // --- Blending & Compositing Engine ---

    /**
     * Composites all visible layers from bottom to top respecting:
     * - Visibility
     * - Opacity and Fill
     * - Blend modes
     * - Layer masks (raster + vector)
     * - Clipping masks
     */
    fun compositeAllLayers(): Bitmap {
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        var i = 0
        while (i < _layers.size) {
            val baseLayer = _layers[i]
            if (!baseLayer.isVisible) {
                i++
                continue
            }

            // Check if there are clipping masks on top of this base layer
            val clippingStack = mutableListOf<PhotoshopLayer>()
            var j = i + 1
            while (j < _layers.size && _layers[j].isClippingMask) {
                if (_layers[j].isVisible) {
                    clippingStack.add(_layers[j])
                }
                j++
            }

            // Render base layer with its clipping masks
            val baseRendered = renderSingleLayerWithMasks(baseLayer)
            if (clippingStack.isNotEmpty()) {
                val clipCanvas = Canvas(baseRendered)
                val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                for (clipLayer in clippingStack) {
                    val clipRendered = renderSingleLayerWithMasks(clipLayer)
                    // Blend clip layer into base
                    val blendedClip = blendTwoBitmaps(baseRendered, clipRendered, clipLayer.blendMode, clipLayer.opacity * clipLayer.fillOpacity)
                    clipCanvas.drawBitmap(blendedClip, 0f, 0f, clipPaint)
                    clipRendered.recycle()
                    blendedClip.recycle()
                }
            }

            // Blend baseRendered onto main accumulation canvas
            val blendedAccum = blendTwoBitmaps(result, baseRendered, baseLayer.blendMode, baseLayer.opacity * baseLayer.fillOpacity)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(blendedAccum, 0f, 0f, null)
            baseRendered.recycle()
            blendedAccum.recycle()

            i = j
        }

        return result
    }

    private fun compositeTwoLayers(lower: PhotoshopLayer, upper: PhotoshopLayer): Bitmap {
        val lowerBmp = renderSingleLayerWithMasks(lower)
        val upperBmp = renderSingleLayerWithMasks(upper)
        val blended = blendTwoBitmaps(lowerBmp, upperBmp, upper.blendMode, upper.opacity * upper.fillOpacity)
        lowerBmp.recycle()
        upperBmp.recycle()
        return blended
    }

    private fun renderSingleLayerWithMasks(layer: PhotoshopLayer): Bitmap {
        val out = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Draw pixel bitmap with offset
        canvas.drawBitmap(layer.bitmap, layer.offsetX, layer.offsetY, null)

        // Apply raster mask if present & enabled
        val mask = layer.maskBitmap
        if (mask != null && layer.isMaskEnabled) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.drawBitmap(mask, if (layer.isMaskLinked) layer.offsetX else 0f, if (layer.isMaskLinked) layer.offsetY else 0f, maskPaint)
        }

        // Apply vector mask if present
        layer.vectorMaskPath?.let { path ->
            val vectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                color = Color.BLACK
            }
            val vectorMaskBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ALPHA_8)
            val vectorCanvas = Canvas(vectorMaskBmp)
            vectorCanvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL })
            canvas.drawBitmap(vectorMaskBmp, 0f, 0f, vectorPaint)
            vectorMaskBmp.recycle()
        }

        return out
    }

    /**
     * Exact pixel-by-pixel Photoshop blending algorithm implementation.
     */
    fun blendTwoBitmaps(base: Bitmap, blend: Bitmap, mode: PhotoshopBlendMode, alpha: Float): Bitmap {
        val w = canvasWidth
        val h = canvasHeight
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val basePixels = IntArray(w * h)
        val blendPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)

        base.getPixels(basePixels, 0, w, 0, 0, w, h)
        blend.getPixels(blendPixels, 0, w, 0, 0, w, h)

        val rng = Random(42)

        for (i in 0 until (w * h)) {
            val bColor = basePixels[i]
            val sColor = blendPixels[i]

            val bA = ((bColor ushr 24) and 0xFF) / 255.0f
            val sA = (((sColor ushr 24) and 0xFF) / 255.0f) * alpha

            if (sA <= 0.0001f) {
                outPixels[i] = bColor
                continue
            }
            if (bA <= 0.0001f) {
                val outAlpha = (sA * 255.0f).toInt().coerceIn(0, 255)
                outPixels[i] = (outAlpha shl 24) or (sColor and 0x00FFFFFF)
                continue
            }

            val bR = ((bColor ushr 16) and 0xFF) / 255.0f
            val bG = ((bColor ushr 8) and 0xFF) / 255.0f
            val bB = (bColor and 0xFF) / 255.0f

            val sR = ((sColor ushr 16) and 0xFF) / 255.0f
            val sG = ((sColor ushr 8) and 0xFF) / 255.0f
            val sB = (sColor and 0xFF) / 255.0f

            var rR: Float
            var rG: Float
            var rB: Float

            when (mode) {
                PhotoshopBlendMode.NORMAL -> {
                    rR = sR; rG = sG; rB = sB
                }
                PhotoshopBlendMode.DISSOLVE -> {
                    val threshold = rng.nextFloat()
                    if (sA >= threshold) {
                        rR = sR; rG = sG; rB = sB
                    } else {
                        rR = bR; rG = bG; rB = bB
                    }
                }
                PhotoshopBlendMode.DARKEN -> {
                    rR = min(bR, sR); rG = min(bG, sG); rB = min(bB, sB)
                }
                PhotoshopBlendMode.MULTIPLY -> {
                    rR = bR * sR; rG = bG * sG; rB = bB * sB
                }
                PhotoshopBlendMode.COLOR_BURN -> {
                    rR = if (sR <= 0f) 0f else (1f - (1f - bR) / sR).coerceIn(0f, 1f)
                    rG = if (sG <= 0f) 0f else (1f - (1f - bG) / sG).coerceIn(0f, 1f)
                    rB = if (sB <= 0f) 0f else (1f - (1f - bB) / sB).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.LINEAR_BURN -> {
                    rR = (bR + sR - 1f).coerceIn(0f, 1f)
                    rG = (bG + sG - 1f).coerceIn(0f, 1f)
                    rB = (bB + sB - 1f).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.LIGHTEN -> {
                    rR = max(bR, sR); rG = max(bG, sG); rB = max(bB, sB)
                }
                PhotoshopBlendMode.SCREEN -> {
                    rR = 1f - (1f - bR) * (1f - sR)
                    rG = 1f - (1f - bG) * (1f - sG)
                    rB = 1f - (1f - bB) * (1f - sB)
                }
                PhotoshopBlendMode.COLOR_DODGE -> {
                    rR = if (sR >= 1f) 1f else (bR / (1f - sR)).coerceIn(0f, 1f)
                    rG = if (sG >= 1f) 1f else (bG / (1f - sG)).coerceIn(0f, 1f)
                    rB = if (sB >= 1f) 1f else (bB / (1f - sB)).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.LINEAR_DODGE_ADD -> {
                    rR = (bR + sR).coerceIn(0f, 1f)
                    rG = (bG + sG).coerceIn(0f, 1f)
                    rB = (bB + sB).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.OVERLAY -> {
                    rR = if (bR < 0.5f) 2f * bR * sR else 1f - 2f * (1f - bR) * (1f - sR)
                    rG = if (bG < 0.5f) 2f * bG * sG else 1f - 2f * (1f - bG) * (1f - sG)
                    rB = if (bB < 0.5f) 2f * bB * sB else 1f - 2f * (1f - bB) * (1f - sB)
                }
                PhotoshopBlendMode.SOFT_LIGHT -> {
                    fun sl(b: Float, s: Float): Float {
                        val d = if (b <= 0.25f) ((16f * b - 12f) * b + 4f) * b else sqrt(b)
                        return if (s <= 0.5f) b - (1f - 2f * s) * b * (1f - b) else b + (2f * s - 1f) * (d - b)
                    }
                    rR = sl(bR, sR); rG = sl(bG, sG); rB = sl(bB, sB)
                }
                PhotoshopBlendMode.HARD_LIGHT -> {
                    rR = if (sR < 0.5f) 2f * bR * sR else 1f - 2f * (1f - bR) * (1f - sR)
                    rG = if (sG < 0.5f) 2f * bG * sG else 1f - 2f * (1f - bG) * (1f - sG)
                    rB = if (sB < 0.5f) 2f * bB * sB else 1f - 2f * (1f - bB) * (1f - sB)
                }
                PhotoshopBlendMode.VIVID_LIGHT -> {
                    fun vl(b: Float, s: Float): Float {
                        return if (s <= 0.5f) {
                            if (s == 0f) 0f else (1f - (1f - b) / (2f * s)).coerceIn(0f, 1f)
                        } else {
                            if (s == 1f) 1f else (b / (2f * (1f - s))).coerceIn(0f, 1f)
                        }
                    }
                    rR = vl(bR, sR); rG = vl(bG, sG); rB = vl(bB, sB)
                }
                PhotoshopBlendMode.LINEAR_LIGHT -> {
                    rR = (bR + 2f * sR - 1f).coerceIn(0f, 1f)
                    rG = (bG + 2f * sG - 1f).coerceIn(0f, 1f)
                    rB = (bB + 2f * sB - 1f).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.PIN_LIGHT -> {
                    fun pl(b: Float, s: Float): Float {
                        return if (s > 0.5f) max(b, 2f * (s - 0.5f)) else min(b, 2f * s)
                    }
                    rR = pl(bR, sR); rG = pl(bG, sG); rB = pl(bB, sB)
                }
                PhotoshopBlendMode.HARD_MIX -> {
                    rR = if (bR + sR >= 1f) 1f else 0f
                    rG = if (bG + sG >= 1f) 1f else 0f
                    rB = if (bB + sB >= 1f) 1f else 0f
                }
                PhotoshopBlendMode.DIFFERENCE -> {
                    rR = abs(bR - sR); rG = abs(bG - sG); rB = abs(bB - sB)
                }
                PhotoshopBlendMode.EXCLUSION -> {
                    rR = bR + sR - 2f * bR * sR
                    rG = bG + sG - 2f * bG * sG
                    rB = bB + sB - 2f * bB * sB
                }
                PhotoshopBlendMode.SUBTRACT -> {
                    rR = (bR - sR).coerceIn(0f, 1f)
                    rG = (bG - sG).coerceIn(0f, 1f)
                    rB = (bB - sB).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.DIVIDE -> {
                    rR = if (sR <= 0.0001f) 1f else (bR / sR).coerceIn(0f, 1f)
                    rG = if (sG <= 0.0001f) 1f else (bG / sG).coerceIn(0f, 1f)
                    rB = if (sB <= 0.0001f) 1f else (bB / sB).coerceIn(0f, 1f)
                }
                PhotoshopBlendMode.HUE, PhotoshopBlendMode.SATURATION, PhotoshopBlendMode.COLOR, PhotoshopBlendMode.LUMINOSITY -> {
                    val hslOut = blendHslComponents(bR, bG, bB, sR, sG, sB, mode)
                    rR = hslOut[0]; rG = hslOut[1]; rB = hslOut[2]
                }
            }

            // Alpha compositing Porter-Duff: OutAlpha = sA + bA * (1 - sA)
            val finalAlpha = sA + bA * (1f - sA)
            val finalR = (sA * rR + bA * bR * (1f - sA)) / finalAlpha
            val finalG = (sA * rG + bA * bG * (1f - sA)) / finalAlpha
            val finalB = (sA * rB + bA * bB * (1f - sA)) / finalAlpha

            val outAByte = (finalAlpha * 255f).toInt().coerceIn(0, 255)
            val outRByte = (finalR.coerceIn(0f, 1f) * 255f).toInt()
            val outGByte = (finalG.coerceIn(0f, 1f) * 255f).toInt()
            val outBByte = (finalB.coerceIn(0f, 1f) * 255f).toInt()

            outPixels[i] = (outAByte shl 24) or (outRByte shl 16) or (outGByte shl 8) or outBByte
        }

        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun blendHslComponents(
        bR: Float, bG: Float, bB: Float,
        sR: Float, sG: Float, sB: Float,
        mode: PhotoshopBlendMode
    ): FloatArray {
        val bHsl = FloatArray(3)
        val sHsl = FloatArray(3)
        Color.RGBToHSV((bR * 255).toInt(), (bG * 255).toInt(), (bB * 255).toInt(), bHsl)
        Color.RGBToHSV((sR * 255).toInt(), (sG * 255).toInt(), (sB * 255).toInt(), sHsl)

        val outHsl = FloatArray(3)
        when (mode) {
            PhotoshopBlendMode.HUE -> {
                outHsl[0] = sHsl[0]; outHsl[1] = bHsl[1]; outHsl[2] = bHsl[2]
            }
            PhotoshopBlendMode.SATURATION -> {
                outHsl[0] = bHsl[0]; outHsl[1] = sHsl[1]; outHsl[2] = bHsl[2]
            }
            PhotoshopBlendMode.COLOR -> {
                outHsl[0] = sHsl[0]; outHsl[1] = sHsl[1]; outHsl[2] = bHsl[2]
            }
            PhotoshopBlendMode.LUMINOSITY -> {
                outHsl[0] = bHsl[0]; outHsl[1] = bHsl[1]; outHsl[2] = sHsl[2]
            }
            else -> {
                outHsl[0] = sHsl[0]; outHsl[1] = sHsl[1]; outHsl[2] = sHsl[2]
            }
        }

        val rgbColor = Color.HSVToColor(outHsl)
        return floatArrayOf(
            ((rgbColor ushr 16) and 0xFF) / 255.0f,
            ((rgbColor ushr 8) and 0xFF) / 255.0f,
            (rgbColor and 0xFF) / 255.0f
        )
    }
}
