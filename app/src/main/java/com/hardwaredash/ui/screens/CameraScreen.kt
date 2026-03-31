package com.hardwaredash.ui.screens

import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen() {
    val cameraPermState = rememberPermissionState(android.Manifest.permission.CAMERA)

    when {
        cameraPermState.status.isGranted -> CameraPreview()
        cameraPermState.status.shouldShowRationale -> {
            PermissionRationale("Camera access is needed to show the live preview and capture photos.") {
                cameraPermState.launchPermissionRequest()
            }
        }
        else -> {
            LaunchedEffect(Unit) { cameraPermState.launchPermissionRequest() }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun CameraPreview() {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing    by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var statusMsg     by remember { mutableStateOf("") }

    // Hold a reference to ImageCapture so we can trigger it from the button
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    Box(Modifier.fillMaxSize()) {

        // ── CameraX PreviewView ───────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val provider = ProcessCameraProvider.getInstance(ctx)
                    provider.addListener({
                        bindCamera(
                            provider        = provider.get(),
                            previewView     = previewView,
                            lifecycleOwner  = lifecycleOwner,
                            lensFacing      = lensFacing,
                            onCapture       = { imageCaptureRef = it },
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            update = { previewView ->
                // Re-bind when lens changes
                val provider = ProcessCameraProvider.getInstance(context)
                provider.addListener({
                    bindCamera(
                        provider       = provider.get(),
                        previewView    = previewView,
                        lifecycleOwner = lifecycleOwner,
                        lensFacing     = lensFacing,
                        onCapture      = { imageCaptureRef = it },
                    )
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // ── Controls overlay ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (statusMsg.isNotEmpty()) {
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    Text(statusMsg, modifier = Modifier.padding(8.dp))
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Flip camera
                FilledTonalIconButton(onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT
                    else
                        CameraSelector.LENS_FACING_BACK
                }) {
                    Icon(Icons.Default.FlipCameraAndroid, "Flip camera")
                }

                // Shutter
                FloatingActionButton(
                    onClick = {
                        val capture = imageCaptureRef ?: return@FloatingActionButton
                        val cv = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "HW_${System.currentTimeMillis()}.jpg")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        }
                        val output = ImageCapture.OutputFileOptions.Builder(
                            context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cv
                        ).build()
                        capture.takePicture(
                            output,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                    statusMsg = "✓ Saved to Gallery"
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    statusMsg = "Error: ${exc.message}"
                                    Log.e("Camera", "Capture failed", exc)
                                }
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier       = Modifier.size(72.dp),
                ) {
                    Icon(Icons.Default.CameraAlt, "Take photo", modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

/** Binds Preview + ImageCapture use-cases to the given lifecycle. */
private fun bindCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    lensFacing: Int,
    onCapture: (ImageCapture) -> Unit,
) {
    provider.unbindAll()

    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val preview  = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    try {
        provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        onCapture(imageCapture)
    } catch (e: Exception) {
        Log.e("CameraScreen", "Use case binding failed", e)
    }
}

@Composable
private fun PermissionRationale(text: String, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant Camera Permission") }
    }
}
