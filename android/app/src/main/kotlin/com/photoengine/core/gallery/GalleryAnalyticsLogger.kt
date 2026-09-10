package com.photoengine.core.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Structured telemetry and error logger for Android native gallery integration.
 * Captures intent payloads, caller OEM variations (Samsung, Xiaomi, Google, OnePlus, Moto),
 * image decode latencies, memory pressure, and edge case errors.
 */
object GalleryAnalyticsLogger {

    private const val TAG = "PhotoEngine_Gallery"

    enum class EventType {
        INTENT_RECEIVED,
        URI_RESOLVED,
        IMAGE_DECODING_STARTED,
        IMAGE_DECODING_SUCCESS,
        IMAGE_DECODING_ERROR,
        IMAGE_EXPORT_STARTED,
        IMAGE_EXPORT_SUCCESS,
        IMAGE_EXPORT_ERROR,
        RESULT_DELIVERED,
        PERMISSION_STATUS_CHANGED,
        EDGE_CASE_DETECTED
    }

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val type: EventType,
        val message: String,
        val details: Map<String, Any?> = emptyMap()
    )

    private val eventHistory = ConcurrentLinkedQueue<LogEntry>()
    private const val MAX_HISTORY_SIZE = 100

    /**
     * Logs an intent received from an external gallery app.
     */
    fun logIntentReceived(intent: Intent?, callingPackage: String?) {
        if (intent == null) {
            log(EventType.INTENT_RECEIVED, "Null intent received")
            return
        }

        val details = mutableMapOf<String, Any?>(
            "action" to intent.action,
            "dataUri" to intent.data?.toString(),
            "type" to intent.type,
            "callingPackage" to (callingPackage ?: "Unknown"),
            "deviceManufacturer" to Build.MANUFACTURER,
            "deviceModel" to Build.MODEL,
            "androidVersion" to Build.VERSION.SDK_INT,
            "hasClipData" to (intent.clipData != null),
            "clipDataCount" to (intent.clipData?.itemCount ?: 0),
            "hasExtraStream" to intent.hasExtra(Intent.EXTRA_STREAM),
            "hasExtraOutput" to intent.hasExtra(android.provider.MediaStore.EXTRA_OUTPUT)
        )

        log(EventType.INTENT_RECEIVED, "Gallery Intent received: action=${intent.action}", details)
    }

    /**
     * Logs successful decode metrics.
     */
    fun logImageDecoded(
        uri: Uri,
        originalWidth: Int,
        originalHeight: Int,
        sampleSize: Int,
        finalWidth: Int,
        finalHeight: Int,
        durationMs: Long,
        orientationDegrees: Int
    ) {
        val details = mapOf(
            "uri" to uri.toString(),
            "originalResolution" to "${originalWidth}x${originalHeight}",
            "sampleSize" to sampleSize,
            "finalResolution" to "${finalWidth}x${finalHeight}",
            "durationMs" to durationMs,
            "orientation" to orientationDegrees,
            "megapixels" to String.format("%.2f MP", (finalWidth * finalHeight) / 1_000_000.0)
        )
        log(EventType.IMAGE_DECODING_SUCCESS, "Decoded image at maximum available resolution", details)
    }

    /**
     * Logs an error during image loading or processing.
     */
    fun logError(type: EventType, message: String, throwable: Throwable? = null, extra: Map<String, Any?> = emptyMap()) {
        val details = extra.toMutableMap()
        if (throwable != null) {
            details["exceptionClass"] = throwable.javaClass.simpleName
            details["exceptionMessage"] = throwable.message ?: "No message"
            details["stackTraceTop"] = throwable.stackTrace.firstOrNull()?.toString()
        }

        log(type, message, details)
        if (throwable != null) {
            Log.e(TAG, "$message: ${throwable.message}", throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    /**
     * Records an edge case (e.g. image deleted while editing, missing grants).
     */
    fun logEdgeCase(edgeCaseName: String, description: String, uri: Uri? = null) {
        val details = mapOf(
            "edgeCase" to edgeCaseName,
            "uri" to (uri?.toString() ?: "None"),
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND
        )
        log(EventType.EDGE_CASE_DETECTED, "Edge case: $edgeCaseName - $description", details)
        Log.w(TAG, "Edge case encountered: $edgeCaseName -> $description")
    }

    /**
     * Logs result returned to caller gallery.
     */
    fun logResultDelivered(resultCode: Int, returnedUri: Uri?, durationTotalMs: Long) {
        val details = mapOf(
            "resultCode" to if (resultCode == -1) "RESULT_OK" else "RESULT_CANCELED ($resultCode)",
            "returnedUri" to (returnedUri?.toString() ?: "None"),
            "durationTotalMs" to durationTotalMs
        )
        log(EventType.RESULT_DELIVERED, "Delivered result back to caller gallery", details)
    }

    private fun log(type: EventType, message: String, details: Map<String, Any?> = emptyMap()) {
        val entry = LogEntry(type = type, message = message, details = details)
        eventHistory.add(entry)
        while (eventHistory.size > MAX_HISTORY_SIZE) {
            eventHistory.poll()
        }
        Log.d(TAG, "[$type] $message | $details")
    }

    /**
     * Exports full debug diagnostic log report for debugging integration failures.
     */
    fun getDiagnosticReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== PhotoEngine Gallery Integration Diagnostic Report ===")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        sb.appendLine("Total Events Logged: ${eventHistory.size}")
        sb.appendLine("---------------------------------------------------------")
        for (entry in eventHistory) {
            sb.appendLine("[${entry.timestamp}] [${entry.type}] ${entry.message}")
            if (entry.details.isNotEmpty()) {
                entry.details.forEach { (k, v) ->
                    sb.appendLine("    $k: $v")
                }
            }
        }
        return sb.toString()
    }
}
