package com.photoengine.core.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.photoengine.core.gallery.GalleryAnalyticsLogger
import com.photoengine.core.gallery.GalleryIntegrationManager
import kotlinx.coroutines.launch

/**
 * MainActivity supporting Native Android Gallery Integration and Photo Picker.
 * Handles:
 * - Standalone App launch with modern Android Photo Picker
 * - System-wide ACTION_VIEW, ACTION_EDIT, and ACTION_SEND for image/*
 * - Preview of the selected image
 * - Export button (returns result to calling app or shares)
 * - Save copy to MediaStore (Pictures/PhotoEngine) via Scoped Storage
 */
class MainActivity : ComponentActivity() {

    private lateinit var galleryManager: GalleryIntegrationManager
    private var sourceIntent: Intent? = null
    private var currentImageUri by mutableStateOf<Uri?>(null)
    private var originalBitmap by mutableStateOf<Bitmap?>(null)
    private var currentBitmap by mutableStateOf<Bitmap?>(null)
    private var brightness by mutableFloatStateOf(0f)
    private var contrast by mutableFloatStateOf(1f)
    private var rotationDegrees by mutableFloatStateOf(0f)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var isFromGalleryEditor by mutableStateOf(false)
    private var imageMetadata by mutableStateOf<String?>(null)

    // Modern Android Photo Picker (Backported to Android 4.4+ via Play Services, native in Android 13+)
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            loadImageFromUri(uri)
        }
    }

    // Fallback file picker contract
    private val getContentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            loadImageFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryManager = GalleryIntegrationManager(applicationContext)
        galleryManager.pruneOldCacheFiles()

        handleIncomingIntent(intent)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    GalleryEditorScaffold()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Parses incoming intent from Google Photos, Samsung Gallery, Xiaomi, etc.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        sourceIntent = intent
        GalleryAnalyticsLogger.logIntentReceived(intent, callingActivity?.packageName)

        if (intent == null) return

        val action = intent.action
        isFromGalleryEditor = action == Intent.ACTION_EDIT ||
                action == Intent.ACTION_VIEW ||
                action == Intent.ACTION_SEND

        val extractedUri = galleryManager.extractSourceUri(intent)
        if (extractedUri != null) {
            currentImageUri = extractedUri
            loadImageFromUri(extractedUri)
        } else if (isFromGalleryEditor) {
            errorMessage = "No se recibió un URI de imagen válido desde la galería de origen."
            GalleryAnalyticsLogger.logEdgeCase("NULL_URI_FROM_GALLERY", "Action: $action")
        }
    }

    /**
     * Loads the target image in maximum available resolution.
     */
    private fun loadImageFromUri(uri: Uri) {
        isLoading = true
        errorMessage = null

        lifecycleScope.launch {
            when (val result = galleryManager.loadMaxResolutionImage(uri)) {
                is GalleryIntegrationManager.LoadResult.Success -> {
                    originalBitmap = result.bitmap
                    currentBitmap = result.bitmap
                    brightness = 0f
                    contrast = 1f
                    rotationDegrees = 0f
                    currentImageUri = result.sourceUri
                    imageMetadata = "${result.originalWidth} × ${result.originalHeight} px • ${result.mimeType}"
                    isLoading = false
                }
                is GalleryIntegrationManager.LoadResult.Failure.ImageDeleted -> {
                    isLoading = false
                    errorMessage = "La imagen seleccionada fue eliminada o movida por otra aplicación."
                }
                is GalleryIntegrationManager.LoadResult.Failure.SecurityDenied -> {
                    isLoading = false
                    errorMessage = "Permiso de lectura denegado por el sistema Android para este URI."
                }
                is GalleryIntegrationManager.LoadResult.Failure.OutOfMemory -> {
                    isLoading = false
                    errorMessage = "Memoria insuficiente para decodificar la imagen en alta resolución."
                }
                is GalleryIntegrationManager.LoadResult.Failure.DecodeError -> {
                    isLoading = false
                    errorMessage = "Error al decodificar la imagen: ${result.message}"
                }
                is GalleryIntegrationManager.LoadResult.Failure.UriNotFound -> {
                    isLoading = false
                    errorMessage = "URI no encontrado."
                }
            }
        }
    }

    /**
     * Pure CPU image processing using Android Bitmap, Canvas, and ColorMatrix.
     * Guaranteed 100% NDK-free and RenderScript-free.
     */
    private fun applyCpuAdjustments(newBrightness: Float, newContrast: Float, newRotation: Float) {
        val src = originalBitmap ?: return
        brightness = newBrightness
        contrast = newContrast
        rotationDegrees = newRotation

        val colorMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        }

        val rot = ((rotationDegrees % 360) + 360) % 360
        val isSwapped = rot == 90f || rot == 270f
        val outW = if (isSwapped) src.height else src.width
        val outH = if (isSwapped) src.width else src.height

        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        val matrix = android.graphics.Matrix().apply {
            postTranslate(-src.width / 2f, -src.height / 2f)
            postRotate(rot)
            postTranslate(outW / 2f, outH / 2f)
        }
        canvas.drawBitmap(src, matrix, paint)
        currentBitmap = result
    }

    /**
     * Saves the edited image to temporary cache and delivers result back to calling gallery.
     */
    private fun exportImage() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "No hay imagen para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        lifecycleScope.launch {
            try {
                val exportResult = galleryManager.saveToTemporaryCache(bitmap)
                val resultIntent = galleryManager.buildResultIntent(sourceIntent, exportResult)

                GalleryAnalyticsLogger.logResultDelivered(
                    Activity.RESULT_OK,
                    exportResult.contentUri,
                    exportResult.durationMs
                )

                if (isFromGalleryEditor && sourceIntent != null) {
                    setResult(Activity.RESULT_OK, resultIntent)
                    Toast.makeText(this@MainActivity, "Imagen exportada y devuelta a la galería", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Standalone mode: launch share intent
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = exportResult.mimeType
                        putExtra(Intent.EXTRA_STREAM, exportResult.contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Exportar imagen"))
                    isLoading = false
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error al exportar la imagen: ${e.localizedMessage}"
                GalleryAnalyticsLogger.logError(
                    GalleryAnalyticsLogger.EventType.IMAGE_EXPORT_ERROR,
                    "Failed to export image",
                    e
                )
            }
        }
    }

    /**
     * Saves a permanent copy directly into Scoped Storage (Pictures/PhotoEngine).
     */
    private fun saveToMediaStoreLibrary() {
        val bitmap = currentBitmap ?: return
        isLoading = true
        lifecycleScope.launch {
            val savedUri = galleryManager.saveToMediaStore(bitmap)
            isLoading = false
            if (savedUri != null) {
                Toast.makeText(
                    this@MainActivity,
                    "Copia guardada en Galería (Pictures/PhotoEngine)",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Error al guardar copia en MediaStore",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun cancelAndExit() {
        GalleryAnalyticsLogger.logResultDelivered(Activity.RESULT_CANCELED, null, 0)
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun launchPhotoPicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: Exception) {
            getContentLauncher.launch("image/*")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GalleryEditorScaffold() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isFromGalleryEditor) "Editor de Galería Nativa" else "PhotoEngine Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            imageMetadata?.let {
                                Text(
                                    text = it,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF141416)
                    ),
                    navigationIcon = {
                        IconButton(onClick = { cancelAndExit() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    },
                    actions = {
                        if (currentBitmap != null) {
                            // Save copy to MediaStore (Pictures/PhotoEngine)
                            IconButton(onClick = { saveToMediaStoreLibrary() }) {
                                Icon(Icons.Default.Save, contentDescription = "Guardar en MediaStore", tint = Color(0xFF38BDF8))
                            }

                            // Export button
                            Button(
                                onClick = { exportImage() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                )
            },
            containerColor = Color(0xFF0F172A)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Procesando imagen...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aviso del Editor",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFCBD5E1),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { cancelAndExit() }) {
                                    Text("Cerrar", color = Color.White)
                                }
                                Button(
                                    onClick = { launchPhotoPicker() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("Seleccionar otra foto")
                                }
                            }
                        }
                    }

                    currentBitmap != null -> {
                        // Display the high-res bitmap preview
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                currentBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Vista previa de la imagen",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                // Status badge
                                if (isFromGalleryEditor) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xCC000000))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "✓ Conectado a Galería Externa",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // CPU Basic Adjustments (Brightness, Contrast, Rotate 90°)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF0F172A)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Brillo: ${brightness.toInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                        Slider(
                                            value = brightness,
                                            onValueChange = { applyCpuAdjustments(it, contrast, rotationDegrees) },
                                            valueRange = -100f..100f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Contraste: ${"%.1f".format(contrast)}x", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(70.dp))
                                        Slider(
                                            value = contrast,
                                            onValueChange = { applyCpuAdjustments(brightness, it, rotationDegrees) },
                                            valueRange = 0.5f..2.0f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalButton(
                                            onClick = { applyCpuAdjustments(brightness, contrast, rotationDegrees + 90f) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Girar 90°", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { applyCpuAdjustments(0f, 1f, 0f) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restablecer", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Bottom actions bar
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1E293B)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { launchPhotoPicker() }
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cambiar Foto", fontSize = 12.sp, color = Color.White)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { saveToMediaStoreLibrary() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1))
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Guardar en MediaStore", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = { exportImage() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Exportar", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Empty state: Modern Photo Picker Entry
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "PhotoEngine Pro",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Selecciona una fotografía para comenzar a editar o compártela desde cualquier galería.",
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { launchPhotoPicker() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar Imagen (Photo Picker)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
