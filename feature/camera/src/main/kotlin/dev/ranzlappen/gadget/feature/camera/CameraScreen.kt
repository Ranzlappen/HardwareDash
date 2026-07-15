package dev.ranzlappen.gadget.feature.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootConfirmActionRow
import dev.ranzlappen.gadget.core.ui.module.RootToolsSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val rootTools by viewModel.rootTools.collectAsStateWithLifecycle()
    var rootToolsExpanded by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val isRootedFlavor = viewModel.isRootedFlavor

    LaunchedEffect(cameraPermission.status.isGranted) {
        viewModel.onPermissionResult(cameraPermission.status.isGranted)
    }

    CameraScreenContent(
        state = state,
        history = history,
        moduleInfo = cameraModuleInfo(permissionGranted = state.permissionGranted, isRootedFlavor = isRootedFlavor),
        onRequestPermission = { cameraPermission.launchPermissionRequest() },
        onScanDetected = viewModel::onScanDetected,
        onToggleTorch = viewModel::toggleTorch,
        onCopy = { result -> viewModel.copyToClipboard(context, result) },
        onShare = { result -> shareText(context, result.rawValue) },
        onClearHistory = viewModel::clearHistory,
        modifier = modifier,
        rootTools = {
            RootToolsSection(
                title = stringResource(R.string.camera_root_tools_title),
                available = isRootedFlavor,
                unavailableMessage = stringResource(R.string.camera_root_tools_unavailable),
                expanded = rootToolsExpanded,
                onExpandedChange = { rootToolsExpanded = it },
            ) {
                RootConfirmActionRow(
                    label = stringResource(R.string.camera_root_hal_bypass_label),
                    description = stringResource(R.string.camera_root_hal_bypass_detail),
                    runLabel = stringResource(R.string.camera_root_run),
                    confirmTitle = stringResource(R.string.camera_root_hal_bypass_confirm_title),
                    confirmMessage = stringResource(R.string.camera_root_hal_bypass_confirm_message),
                    confirmLabel = stringResource(R.string.camera_root_hal_bypass_confirm_label),
                    cancelLabel = stringResource(R.string.camera_root_cancel),
                    onConfirm = viewModel::onHalBypassFrame,
                    enabled = !rootTools.halBypass.running,
                    statusMessage = rootTools.halBypass.message,
                    statusKind = rootTools.halBypass.statusKind,
                )
            }
            if (isRootedFlavor) {
                CameraToolsCard(
                    enabled = true,
                    rootTools = rootTools,
                    onHighFps = viewModel::onHighFps,
                    onManualExposure = viewModel::onManualExposure,
                    onRawCapture = viewModel::onRawCapture,
                    onMultiCamera = viewModel::onMultiCamera,
                    onShutterSound = viewModel::onShutterSound,
                )
            }
        },
    )
}

@Composable
private fun cameraModuleInfo(permissionGranted: Boolean, isRootedFlavor: Boolean): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 21),
    permissions = listOf(
        ModulePermission(
            permission = Manifest.permission.CAMERA,
            label = stringResource(R.string.camera_perm_label),
            rationale = stringResource(R.string.camera_perm_rationale),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.camera_cap_scanner_name),
            detail = stringResource(R.string.camera_cap_scanner_detail),
            status = {
                if (permissionGranted) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.camera_cap_scanner_ok),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.camera_cap_scanner_denied),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.camera_cap_high_fps_name),
            detail = stringResource(R.string.camera_cap_high_fps_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.camera_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.camera_cap_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.camera_cap_manual_name),
            detail = stringResource(R.string.camera_cap_manual_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.camera_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.camera_cap_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.camera_cap_hal_bypass_name),
            detail = stringResource(R.string.camera_cap_hal_bypass_detail),
            status = {
                if (isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.camera_cap_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.camera_cap_rooted_required),
                    )
                }
            },
        ),
    ),
)

@Composable
internal fun CameraScreenContent(
    state: CameraState,
    history: List<BarcodeResult>,
    moduleInfo: ModuleInfo?,
    onRequestPermission: () -> Unit,
    onScanDetected: (BarcodeResult) -> Unit,
    onToggleTorch: (CameraControl) -> Unit,
    onCopy: (BarcodeResult) -> Unit,
    onShare: (BarcodeResult) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    rootTools: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.camera_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            if (!state.permissionGranted) {
                CameraPermissionCard(onRequestPermission = onRequestPermission)
            } else {
                BarcodeScannerCard(
                    isTorchOn = state.isTorchOn,
                    onScanDetected = onScanDetected,
                    onToggleTorch = onToggleTorch,
                )
            }
            state.latestScan?.let { scan ->
                BarcodeResultCard(
                    result = scan,
                    onCopy = { onCopy(scan) },
                    onShare = { onShare(scan) },
                )
            }
            ScanHistoryCard(
                history = history,
                onCopy = onCopy,
                onClearHistory = onClearHistory,
            )
            rootTools()
        },
    )
}

@Composable
private fun CameraPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.camera_perm_card_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(R.string.camera_perm_card_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            GadgetPrimaryButton(
                onClick = onRequestPermission,
                text = stringResource(R.string.camera_perm_grant_btn),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BarcodeScannerCard(
    isTorchOn: Boolean,
    onScanDetected: (BarcodeResult) -> Unit,
    onToggleTorch: (CameraControl) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.camera_card_scanner_title),
    ) {
        Box {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(executor, BarcodeScannerAnalyzer(onScanDetected))
                                }
                            runCatching {
                                provider.unbindAll()
                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analyzer,
                                )
                                cameraControl = camera.cameraControl
                            }
                        }, context.mainExecutor)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(CameraScreenDefaults.PreviewHeight),
            )

            // Scanning overlay
            Canvas(modifier = Modifier.fillMaxWidth().height(CameraScreenDefaults.PreviewHeight)) {
                val scanRect = Rect(
                    offset = Offset((size.width - size.width * 0.6f) / 2f, (size.height - size.height * 0.6f) / 2f),
                    size = Size(size.width * 0.6f, size.height * 0.6f),
                )
                drawRect(color = Color.Black.copy(alpha = 0.4f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = scanRect.topLeft,
                    size = scanRect.size,
                    cornerRadius = CornerRadius(CameraScreenDefaults.ScanRectCornerRadius.toPx()),
                    blendMode = BlendMode.Clear,
                )
                // Corner markers
                val cornerLen = CameraScreenDefaults.CornerMarkerLength.toPx()
                val strokeWidth = CameraScreenDefaults.CornerMarkerStrokeWidth.toPx()
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

            // Torch toggle
            IconButton(
                onClick = { cameraControl?.let(onToggleTorch) },
                modifier = Modifier.align(Alignment.TopEnd).padding(spacing.micro),
            ) {
                Icon(
                    if (isTorchOn) Icons.Outlined.FlashlightOff else Icons.Outlined.FlashlightOn,
                    contentDescription = stringResource(R.string.camera_torch_toggle),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun BarcodeResultCard(
    result: BarcodeResult,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val context = LocalContext.current
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.camera_card_result_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            SuggestionChip(
                onClick = {},
                label = { Text(result.format) },
            )
            Text(
                text = result.rawValue,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.camera_btn_copy))
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.camera_btn_share))
                }
            }
            AnimatedVisibility(visible = result.isUrl()) {
                GadgetPrimaryButton(
                    onClick = { openUrl(context, result.rawValue) },
                    text = stringResource(R.string.camera_btn_open_url),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(visible = result.isWifi()) {
                result.parsedWifi()?.let { wifi ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        Text(
                            text = stringResource(R.string.camera_wifi_ssid, wifi.ssid),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (wifi.password.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.camera_wifi_password, wifi.password),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = stringResource(R.string.camera_wifi_type, wifi.type.uppercase()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryCard(
    history: List<BarcodeResult>,
    onCopy: (BarcodeResult) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.camera_card_history_title),
    ) {
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.camera_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                history.forEach { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.rawValue.take(48) + if (result.rawValue.length > 48) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${result.format} · ${dateFormat.format(Date(result.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { onCopy(result) },
                            modifier = Modifier.size(CameraScreenDefaults.HistoryCopyIconButtonSize),
                        ) {
                            Icon(
                                Icons.Outlined.ContentCopy,
                                contentDescription = stringResource(R.string.camera_btn_copy),
                            )
                        }
                    }
                }
                GadgetSecondaryButton(
                    onClick = onClearHistory,
                    text = stringResource(R.string.camera_btn_clear_history),
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.tiny),
                )
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private object CameraScreenDefaults {
    val PreviewHeight: Dp = 300.dp
    val ScanRectCornerRadius: Dp = 12.dp
    val CornerMarkerLength: Dp = 24.dp
    val CornerMarkerStrokeWidth: Dp = 3.dp
    val HistoryCopyIconButtonSize: Dp = 36.dp
}
