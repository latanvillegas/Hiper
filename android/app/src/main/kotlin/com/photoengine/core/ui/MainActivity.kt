package com.photoengine.core.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.photoengine.core.color.HistogramData
import com.photoengine.core.gallery.GalleryAnalyticsLogger
import com.photoengine.core.gallery.GalleryIntegrationManager
import com.photoengine.core.gallery.PermissionsHelper
import kotlinx.coroutines.launch

/**
 * MainActivity supporting Native Android Gallery Integration.
 * Acts as the default editor target for Google Photos, Samsung Gallery,
 * Xiaomi Gallery, OnePlus Gallery, and Motorola Gallery via ACTION_EDIT.
 */
class MainActivity : ComponentActivity() {

    private lateinit var galleryManager: GalleryIntegrationManager
    private var sourceIntent: Intent? = null
    private var currentImageUri by mutableStateOf<Uri?>(null)
    private var currentBitmap by mutableStateOf<Bitmap?>(null)
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var isFromGalleryEditor by mutableStateOf(false)
    private var imageMetadata by mutableStateOf<String?>(null)

    // Modern photo picker contract for standalone app launcher mode
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            loadImageFromUri(uri)
        }
    }

    // Modern runtime permissions launcher for Android 13+ (READ_MEDIA_IMAGES) and legacy storage
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        GalleryAnalyticsLogger.logError(
            GalleryAnalyticsLogger.EventType.PERMISSION_STATUS_CHANGED,
            "Permissions result: $permissions"
        )
        if (anyGranted) {
            photoPickerLauncher.launch("image/*")
        } else {
            Toast.makeText(
                this,
                "Se requieren permisos para explorar las fotos locales del dispositivo.",
                Toast.LENGTH_LONG
            ).show()
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
     * Parses the incoming intent from Samsung Gallery, Google Photos, Xiaomi, etc.
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
     * ContentResolver reads directly via Intent grant (no storage permissions needed for Intent URIs).
     */
    private fun loadImageFromUri(uri: Uri) {
        isLoading = true
        errorMessage = null

        lifecycleScope.launch {
            when (val result = galleryManager.loadMaxResolutionImage(uri)) {
                is GalleryIntegrationManager.LoadResult.Success -> {
                    currentBitmap = result.bitmap
                    currentImageUri = result.sourceUri
                    imageMetadata = "${result.originalWidth} x ${result.originalHeight} px • ${result.mimeType}"
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
     * Saves the edited image to temporary cache and returns RESULT_OK to the calling gallery.
     */
    private fun saveAndReturnToGallery() {
        val bitmap = currentBitmap
        if (bitmap == null) {
            Toast.makeText(this, "No hay imagen para guardar", Toast.LENGTH_SHORT).show()
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

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error al guardar el resultado: ${e.localizedMessage}"
                GalleryAnalyticsLogger.logError(
                    GalleryAnalyticsLogger.EventType.IMAGE_EXPORT_ERROR,
                    "Failed to return result to caller gallery",
                    e
                )
            }
        }
    }

    /**
     * Optional: saves a permanent copy directly into Scoped Storage (Pictures/PhotoEngine).
     */
    private fun saveToDeviceLibrary() {
        val bitmap = currentBitmap ?: return
        isLoading = true
        lifecycleScope.launch {
            val savedUri = galleryManager.saveToMediaStore(bitmap)
            isLoading = false
            if (savedUri != null) {
                Toast.makeText(
                    this@MainActivity,
                    "Guardado en Galería (Pictures/PhotoEngine)",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Error al guardar en el almacenamiento del dispositivo",
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

    private fun requestLocalPhotosBrowse() {
        val requiredPermissions = PermissionsHelper.getRequiredMediaPermissions()
        permissionsLauncher.launch(requiredPermissions)
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
                            // Save to Scoped Storage (device library)
                            IconButton(onClick = { saveToDeviceLibrary() }) {
                                Icon(Icons.Default.Save, contentDescription = "Guardar en Dispositivo", tint = Color(0xFF38BDF8))
                            }

                            // Done / Return to Caller Gallery (Google Photos, Samsung, etc.)
                            Button(
                                onClick = { saveAndReturnToGallery() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Listo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                "Cargando imagen en máxima resolución...",
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
                                text = "Aviso de Integración",
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
                                    onClick = { requestLocalPhotosBrowse() },
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
                                        contentDescription = "Foto editada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                // Native Gallery integration badge overlay
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
                                            text = "✓ Conectado a Galería Externa (Sin permisos de disco)",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Quick adjustment bar or tool launcher
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
                                    Text(
                                        text = "Listo para aplicar filtros de curvas, HSL y Face Mesh",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = { saveAndReturnToGallery() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Devolver a Galería", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Empty state when opened from app launcher
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Abre cualquier foto desde Google Fotos, Samsung Gallery, Xiaomi o pulsa aquí para editar.",
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { requestLocalPhotosBrowse() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir Foto del Dispositivo")
                            }
                        }
                    }
                }
            }
        }
    }
}
