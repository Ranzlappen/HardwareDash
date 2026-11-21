package dev.ranzlappen.gadget.feature.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/** Fixed structural sizes for the preview overlay (documented per-file Defaults exception). */
private object CameraPreviewDefaults {
    val ScanRectCornerRadius: Dp = 12.dp
    val CornerMarkerLength: Dp = 24.dp
    val CornerMarkerStrokeWidth: Dp = 3.dp
}

private const val PREVIEW_ASPECT = 3f / 4f
private const val ZOOM_MATCH_TOLERANCE = 0.05f

/**
 * The camera itself (v3 B9 — brings back v1's camera next to v2's
 * scanner). One live preview driven by CameraX's [LifecycleCameraController]
 * (tap-to-focus and pinch-zoom built in) in two modes:
 *
 * - **Photo** — lens picker (every camera CameraX exposes, labelled by focal
 *   length), zoom presets, exposure compensation, torch, and capture to
 *   `Pictures/Gadget` via MediaStore.
 * - **Scan** — the ML Kit barcode analyzer with the scan-frame overlay.
 */
@Composable
internal fun CameraPreviewCard(
    mode: CameraMode,
    onScanDetected: (BarcodeResult) -> Unit,
    onPhotoSaved: (Uri) -> Unit,
    onPhotoError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val latestOnScan by rememberUpdatedState(onScanDetected)

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(mode) {
        when (mode) {
            CameraMode.Photo -> {
                controller.clearImageAnalysisAnalyzer()
                controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            }
            CameraMode.Scan -> {
                controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                controller.setImageAnalysisAnalyzer(analysisExecutor, BarcodeScannerAnalyzer { latestOnScan(it) })
            }
        }
    }

    var lenses by remember { mutableStateOf(emptyList<LensOption>()) }
    LaunchedEffect(Unit) { lenses = loadLenses(context) }
    var selectedLensId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedLensId) {
        val id = selectedLensId ?: return@LaunchedEffect
        runCatching { controller.cameraSelector = selectorFor(id) }
    }

    val zoomState by controller.zoomState.observedState()
    val torchState by controller.torchState.observedState()
    val torchOn = torchState == TorchState.ON
    var exposureIndex by remember(selectedLensId) { mutableIntStateOf(0) }
    var capturing by remember { mutableStateOf(false) }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(
            if (mode == CameraMode.Photo) R.string.camera_card_photo_title else R.string.camera_card_scanner_title,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT),
            ) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).also { it.controller = controller } },
                    modifier = Modifier.fillMaxSize(),
                )
                if (mode == CameraMode.Scan) ScanOverlay(Modifier.fillMaxSize())
                IconButton(
                    onClick = { controller.enableTorch(!torchOn) },
                    enabled = controller.cameraInfo?.hasFlashUnit() != false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(spacing.micro),
                ) {
                    Icon(
                        imageVector = if (torchOn) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                        contentDescription = stringResource(R.string.camera_torch_toggle),
                        tint = Color.White,
                    )
                }
            }

            if (lenses.size > 1) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                ) {
                    lenses.forEach { lens ->
                        GadgetChip(
                            selected = lens.cameraId == (selectedLensId ?: lenses.firstOrNull()?.cameraId),
                            onClick = { selectedLensId = lens.cameraId },
                            label = lensLabel(lens),
                        )
                    }
                }
            }

            zoomState?.let { zoom -> ZoomRow(zoom = zoom, onZoom = { controller.setZoomRatio(it) }) }

            val exposure = controller.cameraInfo?.exposureState
            if (exposure != null && exposure.isExposureCompensationSupported) {
                val range = exposure.exposureCompensationRange
                val step = exposure.exposureCompensationStep.toFloat()
                GadgetSlider(
                    value = exposureIndex.toFloat(),
                    onValueChange = { value ->
                        val index = value.roundToInt().coerceIn(range.lower, range.upper)
                        if (index != exposureIndex) {
                            exposureIndex = index
                            controller.cameraControl?.setExposureCompensationIndex(index)
                        }
                    },
                    valueRange = range.lower.toFloat()..range.upper.toFloat(),
                    label = stringResource(R.string.camera_exposure),
                    valueFormatter = { formatEv(it.roundToInt(), step) },
                    steps = (range.upper - range.lower - 1).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (mode == CameraMode.Photo) {
                GadgetPrimaryButton(
                    onClick = {
                        capturing = true
                        takePhoto(
                            context = context,
                            controller = controller,
                            onSaved = { uri ->
                                capturing = false
                                onPhotoSaved(uri)
                            },
                            onFailed = { message ->
                                capturing = false
                                onPhotoError(message)
                            },
                        )
                    },
                    text = stringResource(if (capturing) R.string.camera_capturing else R.string.camera_take_photo),
                    leadingIcon = Icons.Outlined.PhotoCamera,
                    enabled = !capturing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ZoomRow(zoom: ZoomState, onZoom: (Float) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    val presets = zoomPresets(zoom.minZoomRatio, zoom.maxZoomRatio)
    if (presets.size < 2) return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { ratio ->
            GadgetChip(
                selected = abs(zoom.zoomRatio - ratio) < ZOOM_MATCH_TOLERANCE,
                onClick = { onZoom(ratio) },
                label = formatZoom(ratio),
            )
        }
        Text(
            text = stringResource(R.string.camera_zoom_current, formatZoom(zoom.zoomRatio)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun lensLabel(lens: LensOption): String = stringResource(
    when (lens.role) {
        LensRole.UltraWide -> R.string.camera_lens_ultra_wide
        LensRole.Main -> R.string.camera_lens_main
        LensRole.Tele -> R.string.camera_lens_tele
        LensRole.Front -> R.string.camera_lens_front
        LensRole.External -> R.string.camera_lens_external
    },
)

/** Dimmed frame with a clear rounded window and corner markers (scan mode). */
@Composable
private fun ScanOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scanRect = Rect(
            offset = Offset((size.width - size.width * 0.6f) / 2f, (size.height - size.height * 0.6f) / 2f),
            size = Size(size.width * 0.6f, size.height * 0.6f),
        )
        drawRect(color = Color.Black.copy(alpha = 0.4f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = scanRect.topLeft,
            size = scanRect.size,
            cornerRadius = CornerRadius(CameraPreviewDefaults.ScanRectCornerRadius.toPx()),
            blendMode = BlendMode.Clear,
        )
        val cornerLen = CameraPreviewDefaults.CornerMarkerLength.toPx()
        val strokeWidth = CameraPreviewDefaults.CornerMarkerStrokeWidth.toPx()
        val corners = listOf(
            scanRect.topLeft to Pair(Offset(cornerLen, 0f), Offset(0f, cornerLen)),
            Offset(scanRect.right, scanRect.top) to Pair(Offset(-cornerLen, 0f), Offset(0f, cornerLen)),
            Offset(scanRect.left, scanRect.bottom) to Pair(Offset(cornerLen, 0f), Offset(0f, -cornerLen)),
            scanRect.bottomRight to Pair(Offset(-cornerLen, 0f), Offset(0f, -cornerLen)),
        )
        corners.forEach { (corner, arms) ->
            drawLine(Color.White, corner, corner + arms.first, strokeWidth, StrokeCap.Round)
            drawLine(Color.White, corner, corner + arms.second, strokeWidth, StrokeCap.Round)
        }
    }
}

/** Observe a [LiveData] as Compose state for this composition's lifecycle owner. */
@Composable
private fun <T> LiveData<T>.observedState(): State<T?> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(this) { mutableStateOf(value) }
    DisposableEffect(this, lifecycleOwner) {
        val observer = Observer<T> { state.value = it }
        observe(lifecycleOwner, observer)
        onDispose { removeObserver(observer) }
    }
    return state
}

/** Every camera CameraX can open, with roles from focal length. */
@OptIn(ExperimentalCamera2Interop::class)
private suspend fun loadLenses(context: Context): List<LensOption> {
    val provider = suspendCancellableCoroutine<ProcessCameraProvider?> { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({ cont.resume(runCatching { future.get() }.getOrNull()) }, context.mainExecutor)
    } ?: return emptyList()
    val cameras = provider.availableCameraInfos.mapNotNull { info ->
        runCatching {
            val camera2 = Camera2CameraInfo.from(info)
            val focal = camera2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                ?.minOrNull()
            Triple(camera2.cameraId, info.facing(), focal)
        }.getOrNull()
    }
    return assignLensRoles(cameras)
}

private fun CameraInfo.facing(): LensFacing = when (lensFacing) {
    CameraSelector.LENS_FACING_FRONT -> LensFacing.Front
    CameraSelector.LENS_FACING_BACK -> LensFacing.Back
    else -> LensFacing.External
}

@OptIn(ExperimentalCamera2Interop::class)
private fun selectorFor(cameraId: String): CameraSelector = CameraSelector.Builder()
    .addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == cameraId } }
    .build()

private fun takePhoto(
    context: Context,
    controller: LifecycleCameraController,
    onSaved: (Uri) -> Unit,
    onFailed: (String) -> Unit,
) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, photoFileName(System.currentTimeMillis()))
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, PHOTO_RELATIVE_PATH)
    }
    val options = ImageCapture.OutputFileOptions
        .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        .build()
    controller.takePicture(
        options,
        context.mainExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                output.savedUri?.let(onSaved) ?: onFailed("No file was written")
            }

            override fun onError(exception: ImageCaptureException) {
                onFailed(exception.message ?: exception.javaClass.simpleName)
            }
        },
    )
}
