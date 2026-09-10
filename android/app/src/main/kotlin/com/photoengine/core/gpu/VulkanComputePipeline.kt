package com.photoengine.core.gpu

import android.content.Context
import android.hardware.HardwareBuffer
import android.util.Log

/**
 * Vulkan Compute Engine Pipeline Stub for Android.
 * In this stage, native Vulkan/NDK execution is disabled to ensure 100% reliable
 * CPU-based execution across all devices and standard CI environments.
 */
class VulkanComputePipeline(private val context: Context) {

    companion object {
        private const val TAG = "VulkanComputePipeline"
    }

    private var isVulkanSupported = false

    fun initialize(): Boolean {
        // Native Vulkan is disabled in favor of reliable CPU/Canvas Kotlin processing
        isVulkanSupported = false
        Log.i(TAG, "Vulkan compute is disabled. CPU processing pipeline engaged.")
        return false
    }

    fun dispatchComputePass(
        shaderName: String,
        inputBuffer: HardwareBuffer,
        outputBuffer: HardwareBuffer,
        uniformData: FloatArray,
        width: Int,
        height: Int
    ): Boolean {
        return false
    }

    fun release() {
        // No-op for CPU-only execution
    }
}
