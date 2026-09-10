package com.photoengine.core.face

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FaceLandmarks468(
    val allPoints: List<PointF>,
    val lipsIndices: IntArray,
    val leftEyeIndices: IntArray,
    val rightEyeIndices: IntArray,
    val leftEyebrowIndices: IntArray,
    val rightEyebrowIndices: IntArray,
    val jawlineIndices: IntArray,
    val leftCheekIndices: IntArray,
    val rightCheekIndices: IntArray,
    val noseIndices: IntArray
)

/**
 * 468-Landmark Face Mesh Processor powered by Google ML Kit.
 * Maps dense 3D facial topology for precise virtual makeup, bilateral skin masks,
 * and warp mesh slimming deformation.
 */
class FaceMeshEngine {

    private val detectorOptions = FaceMeshDetectorOptions.Builder()
        .setUseCase(FaceMeshDetectorOptions.FACE_MESH_RESOURCE_ALL)
        .build()

    private val detector = FaceMeshDetection.getClient(detectorOptions)

    // Canonical MediaPipe / ML Kit landmark topology indices
    companion object {
        val LIPS_OUTER = intArrayOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95)
        val LIPS_INNER = intArrayOf(78, 95, 88, 178, 87, 14, 317, 402, 318, 324, 308, 415, 310, 311, 312, 13, 82, 81, 80, 191)
        val LEFT_EYE_LASH = intArrayOf(263, 249, 390, 373, 374, 380, 381, 382, 362, 466, 388, 387, 386, 385, 384, 398)
        val RIGHT_EYE_LASH = intArrayOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 246, 161, 160, 159, 158, 157, 173)
        val LEFT_EYEBROW = intArrayOf(276, 283, 282, 295, 285, 300, 293, 334, 296, 336)
        val RIGHT_EYEBROW = intArrayOf(46, 53, 52, 65, 55, 70, 63, 105, 66, 107)
        val JAWLINE = intArrayOf(10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109)
        val LEFT_CHEEK = intArrayOf(234, 93, 132, 58, 172, 136, 150, 149, 176, 148, 152)
        val RIGHT_CHEEK = intArrayOf(454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152)
        val NOSE = intArrayOf(1, 2, 98, 327, 195, 5, 4, 19, 94, 2)
    }

    suspend fun detectMesh(bitmap: Bitmap): FaceLandmarks468? = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { meshes ->
                if (meshes.isEmpty()) {
                    cont.resume(null)
                    return@addOnSuccessListener
                }

                val primaryFace = meshes[0]
                val allPoints = primaryFace.allPoints.map { p ->
                    val pos = p.position
                    PointF(pos.x, pos.y)
                }

                val result = FaceLandmarks468(
                    allPoints = allPoints,
                    lipsIndices = LIPS_OUTER,
                    leftEyeIndices = LEFT_EYE_LASH,
                    rightEyeIndices = RIGHT_EYE_LASH,
                    leftEyebrowIndices = LEFT_EYEBROW,
                    rightEyebrowIndices = RIGHT_EYEBROW,
                    jawlineIndices = JAWLINE,
                    leftCheekIndices = LEFT_CHEEK,
                    rightCheekIndices = RIGHT_CHEEK,
                    noseIndices = NOSE
                )
                cont.resume(result)
            }
            .addOnFailureListener { ex ->
                cont.resumeWithException(ex)
            }
    }

    fun release() {
        detector.close()
    }
}
