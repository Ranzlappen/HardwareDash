// CHANGE: Expanded camera with multi-lens selection, zoom, exposure compensation, focus mode
// REASON: Expose all available camera lenses and Camera2/CameraX hardware controls
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*

// ─── Lens descriptor ─────────────────────────────────────────────────────────
private data class LensInfo(
    val cameraId: String,
    val lensFacing: Int,
    val label: String,
    val focalLength: Float,
)

private fun enumerateLenses(context: Context): List<LensInfo> {
    val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val lenses = mutableListOf<LensInfo>()
    for (id in cm.cameraIdList) {
        val chars = cm.getCameraCharacteristics(id)
        val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) ?: floatArrayOf(0f)
        val fl = focalLengths.firstOrNull() ?: 0f
        lenses.add(LensInfo(id, facing, "", fl))
    }

    // Label rear lenses by focal length
    val rearLenses = lenses.filter { it.lensFacing == CameraCharacteristics.LENS_FACING_BACK }
        .sortedBy { it.focalLength }
    val labeled = mutableListOf<LensInfo>()

    rearLenses.forEachIndexed { idx, lens ->
        val label = when {
            rearLenses.size == 1 -> "Rear"
            idx == 0 -> "Ultrawide"
            idx == rearLenses.size - 1 && rearLenses.size > 2 -> "Telephoto"
            idx == 1 && rearLenses.size == 2 -> "Rear Main"
            else -> "Rear Main"
        }
        labeled.add(lens.copy(label = label))
    }
    // Front cameras
    lenses.filter { it.lensFacing == CameraCharacteristics.LENS_FACING_FRONT }
        .forEachIndexed { idx, lens ->
            labeled.add(lens.copy(label = if (idx == 0) "Front" else "Front ${idx + 1}"))
        }
    return labeled
}

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
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Available lenses
    val lenses = remember { enumerateLenses(context) }
    var selectedLensIdx by remember { mutableIntStateOf(
        // Default to first rear lens
        lenses.indexOfFirst { it.lensFacing == CameraCharacteristics.LENS_FACING_BACK }.coerceAtLeast(0)
    ) }

    var statusMsg by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(false) }

    // Camera references
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    // Zoom state
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom   by remember { mutableFloatStateOf(1f) }
    var maxZoom   by remember { mutableFloatStateOf(1f) }

    // Exposure compensation
    var exposureIdx  by remember { mutableIntStateOf(0) }
    var exposureMin  by remember { mutableIntStateOf(0) }
    var exposureMax  by remember { mutableIntStateOf(0) }
    var exposureStep by remember { mutableFloatStateOf(0f) }

    // Focus mode
    var tapToFocusEnabled by remember { mutableStateOf(false) }

    // Observe zoom state from CameraInfo
    LaunchedEffect(cameraRef) {
        val cam = cameraRef ?: return@LaunchedEffect
        cam.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
            zoomRatio = state.zoomRatio
            minZoom   = state.minZoomRatio
            maxZoom   = state.maxZoomRatio
        }
        val expState = cam.cameraInfo.exposureState
        exposureMin  = expState.exposureCompensationRange.lower
        exposureMax  = expState.exposureCompensationRange.upper
        exposureStep = expState.exposureCompensationStep.toFloat()
        exposureIdx  = expState.exposureCompensationIndex
    }

    Box(Modifier.fillMaxSize()) {

        // ── CameraX PreviewView ───────────────────────────────────────────
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tapToFocusEnabled, cameraRef) {
                    if (!tapToFocusEnabled) return@pointerInput
                    detectTapGestures { offset ->
                        val cam = cameraRef ?: return@detectTapGestures
                        val factory = SurfaceOrientedMeteringPointFactory(
                            size.width.toFloat(), size.height.toFloat()
                        )
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        cam.cameraControl.startFocusAndMetering(action)
                        statusMsg = "Focus point set"
                    }
                },
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val provider = ProcessCameraProvider.getInstance(ctx)
                    provider.addListener({
                        val cam = bindCamera(
                            provider       = provider.get(),
                            previewView    = previewView,
                            lifecycleOwner = lifecycleOwner,
                            lensFacing     = lenses.getOrNull(selectedLensIdx)?.lensFacing
                                ?: CameraSelector.LENS_FACING_BACK,
                        )
                        imageCaptureRef = cam?.second
                        cameraRef = cam?.first
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            update = { previewView ->
                // Re-bind when lens changes
                val provider = ProcessCameraProvider.getInstance(context)
                provider.addListener({
                    val cam = bindCamera(
                        provider       = provider.get(),
                        previewView    = previewView,
                        lifecycleOwner = lifecycleOwner,
                        lensFacing     = lenses.getOrNull(selectedLensIdx)?.lensFacing
                            ?: CameraSelector.LENS_FACING_BACK,
                    )
                    imageCaptureRef = cam?.second
                    cameraRef = cam?.first
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // ── Controls overlay ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (statusMsg.isNotEmpty()) {
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer,
                    shape  = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Text(statusMsg, modifier = Modifier.padding(8.dp))
                }
            }

            // ── Expandable controls panel ────────────────────────────────
            AnimatedVisibility(visible = showControls) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Lens selector
                        if (lenses.size > 1) {
                            Text("Lens", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                lenses.forEachIndexed { idx, lens ->
                                    FilterChip(
                                        selected = idx == selectedLensIdx,
                                        onClick  = { selectedLensIdx = idx },
                                        label    = { Text(lens.label, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }

                        // Zoom slider
                        if (maxZoom > minZoom) {
                            Text(
                                "Zoom: ${"%.1f".format(zoomRatio)}x",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Slider(
                                value = zoomRatio,
                                onValueChange = { ratio ->
                                    zoomRatio = ratio
                                    cameraRef?.cameraControl?.setZoomRatio(ratio)
                                },
                                valueRange = minZoom..maxZoom,
                            )
                        }

                        // Exposure compensation slider
                        if (exposureMax > exposureMin) {
                            val evValue = exposureIdx * exposureStep
                            Text(
                                "Exposure: ${"%.1f".format(evValue)} EV",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Slider(
                                value = exposureIdx.toFloat(),
                                onValueChange = { v ->
                                    val idx = v.toInt()
                                    exposureIdx = idx
                                    cameraRef?.cameraControl?.setExposureCompensationIndex(idx)
                                },
                                valueRange = exposureMin.toFloat()..exposureMax.toFloat(),
                                steps = (exposureMax - exposureMin - 1).coerceAtLeast(0),
                            )
                        }

                        // Focus mode toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Tap-to-Focus",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = tapToFocusEnabled,
                                onCheckedChange = { tapToFocusEnabled = it },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Toggle controls panel
                FilledTonalIconButton(onClick = { showControls = !showControls }) {
                    Icon(
                        if (showControls) Icons.Default.ExpandMore else Icons.Default.Tune,
                        "Camera controls"
                    )
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

                // Quick flip (front/back)
                FilledTonalIconButton(onClick = {
                    val current = lenses.getOrNull(selectedLensIdx) ?: return@FilledTonalIconButton
                    val targetFacing = if (current.lensFacing == CameraCharacteristics.LENS_FACING_BACK)
                        CameraCharacteristics.LENS_FACING_FRONT
                    else
                        CameraCharacteristics.LENS_FACING_BACK
                    val idx = lenses.indexOfFirst { it.lensFacing == targetFacing }
                    if (idx >= 0) selectedLensIdx = idx
                }) {
                    Icon(Icons.Default.FlipCameraAndroid, "Flip camera")
                }
            }
        }
    }
}

/** Binds Preview + ImageCapture use-cases to the given lifecycle. Returns Camera + ImageCapture. */
private fun bindCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    lensFacing: Int,
): Pair<Camera, ImageCapture>? {
    provider.unbindAll()

    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val preview  = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    return try {
        val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        Pair(camera, imageCapture)
    } catch (e: Exception) {
        Log.e("CameraScreen", "Use case binding failed", e)
        null
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
