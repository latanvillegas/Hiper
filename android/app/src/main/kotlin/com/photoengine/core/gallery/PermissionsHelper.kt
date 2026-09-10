package com.photoengine.core.gallery

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Robust permissions manager for Android 10+ (Scoped Storage) and Android 13+ (Granular Media).
 * 
 * Key Architectural Principle:
 * When launched via Intent (ACTION_EDIT, ACTION_VIEW, ACTION_SEND) by a gallery (Google Photos,
 * Samsung Gallery, MIUI, etc.), the caller grants a temporary URI permission
 * (FLAG_GRANT_READ_URI_PERMISSION). In this mode, storage permissions are NOT strictly required
 * to read and process the image via ContentResolver.
 * Storage permissions are only requested if the user chooses to browse the local device library.
 */
object PermissionsHelper {

    enum class StoragePermissionState {
        GRANTED_FULL,
        GRANTED_PARTIAL_ANDROID_14, // Selected photos only (Android 14+)
        DENIED,
        NOT_REQUIRED_FOR_INTENT_URI // Image accessed directly via Intent grant
    }

    /**
     * Gets the list of media permissions applicable for the current Android SDK version.
     */
    fun getRequiredMediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> { // Android 14+ (API 34)
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> { // Android 13 (API 33)
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            else -> { // Android 12 and below
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Checks current permission status for general media browsing.
     */
    fun checkMediaPermissionState(context: Context): StoragePermissionState {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                val fullImagesGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED

                val partialGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED

                return when {
                    fullImagesGranted -> StoragePermissionState.GRANTED_FULL
                    partialGranted -> StoragePermissionState.GRANTED_PARTIAL_ANDROID_14
                    else -> StoragePermissionState.DENIED
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                return if (granted) StoragePermissionState.GRANTED_FULL else StoragePermissionState.DENIED
            }
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                return if (granted) StoragePermissionState.GRANTED_FULL else StoragePermissionState.DENIED
            }
        }
    }

    /**
     * Determines whether an image URI can be opened without requesting runtime storage permissions.
     * Checks if ContentResolver can open an InputStream directly via temporary Intent grants.
     */
    fun canAccessUriDirectly(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { 
                true 
            } ?: false
        } catch (e: SecurityException) {
            GalleryAnalyticsLogger.logError(
                GalleryAnalyticsLogger.EventType.PERMISSION_STATUS_CHANGED,
                "SecurityException checking direct URI access for $uri",
                e
            )
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the activity should show UI with rationale before requesting permissions.
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        val permissions = getRequiredMediaPermissions()
        return permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }
}
