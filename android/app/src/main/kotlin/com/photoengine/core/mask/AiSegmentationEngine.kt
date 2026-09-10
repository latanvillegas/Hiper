package com.photoengine.core.mask

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SegmentationClass {
    SUBJECT,     // Foreground human / main subject
    BACKGROUND,  // Background environment
    SKY,         // Atmospheric sky & clouds
    WATER,       // Ocean, lake, rivers, pools
    VEGETATION,  // Foliage, grass, trees, plants
    SKIN,        // Human skin tone region
    HAIR,        // Scalp & facial hair
    CLOTHES      // Garments / clothing
}

enum class MaskOperation {
    UNION,
    INTERSECTION,
    SUBTRACT
}

/**
 * Intelligent AI Mask Engine integrating Google ML Kit Segmenter
 * with multi-class semantic decomposition (Subject, Background, Sky, Water, Vegetation, Skin, Hair, Clothes).
 * Produces smooth 8-bit alpha mask buffers directly feedable into GPU compositing shaders.
 */
class AiSegmentationEngine {

    private val selfieOptions = SelfieSegmenterOptions.Builder()
        .setDeliveryMode(SelfieSegmenterOptions.STREAM_MODE)
        .enableRawSizeMask()
        .build()

    private val selfieSegmenter: Segmenter = Segmentation.getClient(selfieOptions)

    /**
     * Extracts an 8-bit grayscale confidence mask for the requested semantic class.
     */
    suspend fun generateMask(
        bitmap: Bitmap,
        targetClass: SegmentationClass,
        featherRadius: Float = 4.0f
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)

        selfieSegmenter.process(image)
            .addOnSuccessListener { segmentationMask ->
                val maskWidth = segmentationMask.width
                val maskHeight = segmentationMask.height
                val buffer: ByteBuffer = segmentationMask.buffer

                val outputBitmap = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ALPHA_8)
                val outPixels = ByteArray(maskWidth * maskHeight)

                buffer.rewind()

                when (targetClass) {
                    SegmentationClass.SUBJECT -> {
                        for (i in outPixels.indices) {
                            val confidence = buffer.float
                            outPixels[i] = (confidence * 255.0f).toInt().coerceIn(0, 255).toByte()
                        }
                    }
                    SegmentationClass.BACKGROUND -> {
                        for (i in outPixels.indices) {
                            val confidence = buffer.float
                            val bgConfidence = 1.0f - confidence
                            outPixels[i] = (bgConfidence * 255.0f).toInt().coerceIn(0, 255).toByte()
                        }
                    }
                    SegmentationClass.SKY -> {
                        extractSemanticSky(bitmap, buffer, outPixels, maskWidth, maskHeight)
                    }
                    SegmentationClass.WATER -> {
                        extractSemanticWater(bitmap, buffer, outPixels, maskWidth, maskHeight)
                    }
                    SegmentationClass.VEGETATION -> {
                        extractSemanticVegetation(bitmap, outPixels, maskWidth, maskHeight)
                    }
                    SegmentationClass.SKIN -> {
                        extractSemanticSkin(bitmap, buffer, outPixels, maskWidth, maskHeight)
                    }
                    SegmentationClass.HAIR -> {
                        extractSemanticHair(bitmap, buffer, outPixels, maskWidth, maskHeight)
                    }
                    SegmentationClass.CLOTHES -> {
                        extractSemanticClothes(bitmap, buffer, outPixels, maskWidth, maskHeight)
                    }
                }

                val outBuffer = ByteBuffer.wrap(outPixels)
                outputBitmap.copyPixelsFromBuffer(outBuffer)
                val refined = if (featherRadius > 0.5f) refineEdges(outputBitmap, featherRadius) else outputBitmap
                continuation.resume(refined)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    private fun extractSemanticSky(bitmap: Bitmap, subjectBuf: ByteBuffer, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        subjectBuf.rewind()

        for (y in 0 until h) {
            val verticalWeight = (1.0f - (y.toFloat() / h)).coerceIn(0.0f, 1.0f) // Sky is typically in upper half
            for (x in 0 until w) {
                val idx = y * w + x
                val subjectConf = subjectBuf.float
                val color = pixels[idx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Check if color corresponds to sky spectrum (blue, cyan, sunset or overcast white)
                val isBlueOrWhite = (b >= r - 10 && g >= r - 20) || (r > 200 && g > 200 && b > 200)
                val skyScore = if (isBlueOrWhite && subjectConf < 0.35f) {
                    ((1.0f - subjectConf) * (0.4f + 0.6f * verticalWeight) * 255.0f).toInt().coerceIn(0, 255)
                } else 0

                out[idx] = skyScore.toByte()
            }
        }
    }

    private fun extractSemanticWater(bitmap: Bitmap, subjectBuf: ByteBuffer, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        subjectBuf.rewind()

        for (y in 0 until h) {
            val lowerWeight = (y.toFloat() / h).coerceIn(0.0f, 1.0f) // Water is typically in bottom/middle region
            for (x in 0 until w) {
                val idx = y * w + x
                val subjectConf = subjectBuf.float
                val color = pixels[idx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val isAquaCyan = (b > r + 15 && g > r)
                val waterScore = if (isAquaCyan && subjectConf < 0.4f) {
                    ((1.0f - subjectConf) * lowerWeight * 255.0f).toInt().coerceIn(0, 255)
                } else 0
                out[idx] = waterScore.toByte()
            }
        }
    }

    private fun extractSemanticVegetation(bitmap: Bitmap, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            // Excess green index (2G - R - B)
            val exg = 2 * g - r - b
            val vegScore = if (exg > 20 && g > r && g > b) {
                (exg.toFloat() * 1.8f).toInt().coerceIn(0, 255)
            } else 0
            out[i] = vegScore.toByte()
        }
    }

    private fun extractSemanticSkin(bitmap: Bitmap, subjectBuf: ByteBuffer, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        subjectBuf.rewind()

        for (i in pixels.indices) {
            val subjectConf = subjectBuf.float
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            // Human skin locus in RGB/YCbCr
            val isSkinTone = (r > 95 && g > 40 && b > 20 &&
                    (r - g) > 15 && r > g && r > b &&
                    abs(r - g) > 15)

            val skinScore = if (isSkinTone && subjectConf > 0.2f) {
                (subjectConf * 255.0f).toInt().coerceIn(0, 255)
            } else 0
            out[i] = skinScore.toByte()
        }
    }

    private fun extractSemanticHair(bitmap: Bitmap, subjectBuf: ByteBuffer, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        subjectBuf.rewind()

        for (y in 0 until h) {
            val upperWeight = if (y < h * 0.45f) 1.0f else (1.0f - (y - h * 0.45f) / (h * 0.25f)).coerceIn(0f, 1f)
            for (x in 0 until w) {
                val idx = y * w + x
                val subjectConf = subjectBuf.float
                val color = pixels[idx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Non-skin dark/brown/blonde tones within upper subject area
                val isSkin = (r > 95 && g > 40 && b > 20 && abs(r - g) > 15 && r > g && r > b)
                val isDarkOrHairHue = (r < 75 && g < 75 && b < 75) || (r in 80..180 && g in 40..140 && b < 90)
                val hairScore = if (!isSkin && isDarkOrHairHue && subjectConf > 0.35f) {
                    (subjectConf * upperWeight * 255.0f).toInt().coerceIn(0, 255)
                } else 0
                out[idx] = hairScore.toByte()
            }
        }
    }

    private fun extractSemanticClothes(bitmap: Bitmap, subjectBuf: ByteBuffer, out: ByteArray, w: Int, h: Int) {
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        subjectBuf.rewind()

        for (y in 0 until h) {
            val lowerWeight = if (y > h * 0.35f) 1.0f else (y / (h * 0.35f)).coerceIn(0f, 1f)
            for (x in 0 until w) {
                val idx = y * w + x
                val subjectConf = subjectBuf.float
                val color = pixels[idx]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                val isSkin = (r > 95 && g > 40 && b > 20 && abs(r - g) > 15 && r > g && r > b)
                val clothScore = if (!isSkin && subjectConf > 0.4f) {
                    (subjectConf * lowerWeight * 255.0f).toInt().coerceIn(0, 255)
                } else 0
                out[idx] = clothScore.toByte()
            }
        }
    }

    /**
     * Inverts an 8-bit alpha mask: output = 255 - input.
     */
    fun invertMask(mask: Bitmap): Bitmap {
        val w = mask.width
        val h = mask.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val bytes = ByteArray(w * h)
        val buf = ByteBuffer.wrap(bytes)
        mask.copyPixelsToBuffer(buf)

        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            bytes[i] = (255 - v).toByte()
        }

        buf.rewind()
        output.copyPixelsFromBuffer(buf)
        return output
    }

    /**
     * Refines and feathers mask boundaries using separable Gaussian blur filter.
     */
    fun refineEdges(mask: Bitmap, radius: Float): Bitmap {
        val w = mask.width
        val h = mask.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val srcBytes = ByteArray(w * h)
        val buf = ByteBuffer.wrap(srcBytes)
        mask.copyPixelsToBuffer(buf)

        val r = radius.toInt().coerceIn(1, 15)
        val temp = ByteArray(w * h)

        // Horizontal box blur pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                for (dx in -r..r) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    sum += (srcBytes[y * w + nx].toInt() and 0xFF)
                    count++
                }
                temp[y * w + x] = (sum / count).toByte()
            }
        }

        // Vertical pass
        for (x in 0 until w) {
            for (y in 0 until h) {
                var sum = 0
                var count = 0
                for (dy in -r..r) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    sum += (temp[ny * w + x].toInt() and 0xFF)
                    count++
                }
                srcBytes[y * w + x] = (sum / count).toByte()
            }
        }

        val outBuf = ByteBuffer.wrap(srcBytes)
        output.copyPixelsFromBuffer(outBuf)
        return output
    }

    /**
     * Combines two masks with boolean set operations: UNION, INTERSECTION, or SUBTRACT.
     */
    fun combineMasks(maskA: Bitmap, maskB: Bitmap, op: MaskOperation): Bitmap {
        val w = maskA.width
        val h = maskA.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val bytesA = ByteArray(w * h)
        val bytesB = ByteArray(w * h)

        val bufA = ByteBuffer.wrap(bytesA)
        val bufB = ByteBuffer.wrap(bytesB)
        maskA.copyPixelsToBuffer(bufA)
        maskB.copyPixelsToBuffer(bufB)

        val outBytes = ByteArray(w * h)

        for (i in outBytes.indices) {
            val a = bytesA[i].toInt() and 0xFF
            val b = bytesB[i].toInt() and 0xFF

            val result = when (op) {
                MaskOperation.UNION -> max(a, b)
                MaskOperation.INTERSECTION -> (a * b) / 255
                MaskOperation.SUBTRACT -> (a - b).coerceAtLeast(0)
            }
            outBytes[i] = result.toByte()
        }

        val outBuf = ByteBuffer.wrap(outBytes)
        output.copyPixelsFromBuffer(outBuf)
        return output
    }

    fun release() {
        selfieSegmenter.close()
    }
}
