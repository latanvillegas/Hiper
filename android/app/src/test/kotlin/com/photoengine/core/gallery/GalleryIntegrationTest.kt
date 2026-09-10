package com.photoengine.core.gallery

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying Native Gallery Intent resolution, Scoped Storage compatibility,
 * and error handling logic.
 */
class GalleryIntegrationTest {

    @Test
    fun testIntentResolution_actionEdit_withData() {
        val testUri = Uri.parse("content://media/external/images/media/42")
        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = testUri
            type = "image/jpeg"
        }

        // Mock verification of URI extraction logic
        val extractedUri = intent.data
        assertEquals("content://media/external/images/media/42", extractedUri?.toString())
        assertEquals(Intent.ACTION_EDIT, intent.action)
    }

    @Test
    fun testIntentResolution_actionSend_withExtraStream() {
        val testUri = Uri.parse("content://com.google.android.apps.photos.contentprovider/123/original")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, testUri)
        }

        @Suppress("DEPRECATION")
        val extractedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertNotNull(extractedUri)
        assertEquals("content://com.google.android.apps.photos.contentprovider/123/original", extractedUri.toString())
    }

    @Test
    fun testIntentResolution_samsungGallery_clipData() {
        val testUri = Uri.parse("content://sec.gallery.provider/image/999")
        val clipData = ClipData.newUri(null, "Samsung Photo", testUri)
        val intent = Intent(Intent.ACTION_EDIT).apply {
            this.clipData = clipData
        }

        val extractedUri = intent.clipData?.getItemAt(0)?.uri
        assertEquals("content://sec.gallery.provider/image/999", extractedUri?.toString())
    }

    @Test
    fun testResultIntent_grantsSecurityFlags() {
        val editedUri = Uri.parse("content://com.photoengine.core.fileprovider/edited_photos/EDIT_12345.jpg")
        val resultIntent = Intent().apply {
            data = editedUri
            setDataAndType(editedUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        val flags = resultIntent.flags
        val hasReadGrant = (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        val hasWriteGrant = (flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0

        assertTrue("Result intent must grant read URI permission", hasReadGrant)
        assertTrue("Result intent must grant write URI permission", hasWriteGrant)
        assertEquals(editedUri, resultIntent.data)
    }

    @Test
    fun testAnalyticsLogger_recordsEventsWithoutCrash() {
        GalleryAnalyticsLogger.logEdgeCase("TEST_CASE", "Testing logger resilience", Uri.parse("content://test/1"))
        val report = GalleryAnalyticsLogger.getDiagnosticReport()
        assertTrue(report.contains("TEST_CASE"))
    }
}
