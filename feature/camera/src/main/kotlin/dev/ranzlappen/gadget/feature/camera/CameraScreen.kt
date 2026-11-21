package dev.ranzlappen.gadget.feature.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
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
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        moduleInfo = cameraModuleInfo(permissionGranted = state.permissionGranted),
        onRequestPermission = { cameraPermission.launchPermissionRequest() },
        onModeChange = viewModel::setMode,
        onScanDetected = viewModel::onScanDetected,
        onPhotoSaved = viewModel::onPhotoSaved,
        onPhotoError = viewModel::onPhotoError,
        onOpenPhoto = { uri -> openPhoto(context, uri) },
        onSharePhoto = { uri -> sharePhoto(context, uri) },
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
private fun cameraModuleInfo(permissionGranted: Boolean): ModuleInfo = ModuleInfo(
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
    ),
)

@Composable
internal fun CameraScreenContent(
    state: CameraState,
    history: List<BarcodeResult>,
    moduleInfo: ModuleInfo?,
    onRequestPermission: () -> Unit,
    onModeChange: (CameraMode) -> Unit,
    onScanDetected: (BarcodeResult) -> Unit,
    onPhotoSaved: (Uri) -> Unit,
    onPhotoError: (String) -> Unit,
    onOpenPhoto: (Uri) -> Unit,
    onSharePhoto: (Uri) -> Unit,
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
            CameraModeChips(mode = state.mode, onModeChange = onModeChange)
            if (!state.permissionGranted) {
                CameraPermissionCard(onRequestPermission = onRequestPermission)
            } else {
                CameraPreviewCard(
                    mode = state.mode,
                    onScanDetected = onScanDetected,
                    onPhotoSaved = onPhotoSaved,
                    onPhotoError = onPhotoError,
                )
            }
            when (state.mode) {
                CameraMode.Photo -> LastPhotoCard(
                    uri = state.lastPhotoUri,
                    error = state.photoError,
                    onOpen = onOpenPhoto,
                    onShare = onSharePhoto,
                )
                CameraMode.Scan -> {
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
                }
            }
            rootTools()
        },
    )
}

@Composable
private fun CameraModeChips(mode: CameraMode, onModeChange: (CameraMode) -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
        GadgetChip(
            selected = mode == CameraMode.Photo,
            onClick = { onModeChange(CameraMode.Photo) },
            label = stringResource(R.string.camera_mode_photo),
            leadingIcon = Icons.Outlined.PhotoCamera,
        )
        GadgetChip(
            selected = mode == CameraMode.Scan,
            onClick = { onModeChange(CameraMode.Scan) },
            label = stringResource(R.string.camera_mode_scan),
            leadingIcon = Icons.Outlined.QrCodeScanner,
        )
    }
}

/** Result of the last capture: where it went, plus open / share — or the error. */
@Composable
private fun LastPhotoCard(
    uri: Uri?,
    error: String?,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
) {
    if (uri == null && error == null) return
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(modifier = Modifier.fillMaxWidth(), title = stringResource(R.string.camera_card_last_photo_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (error != null) {
                Text(
                    text = stringResource(R.string.camera_photo_error, error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (uri != null) {
                Text(
                    text = stringResource(R.string.camera_photo_saved, PHOTO_RELATIVE_PATH),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                    GadgetSecondaryButton(
                        onClick = { onOpen(uri) },
                        text = stringResource(R.string.camera_photo_open),
                        leadingIcon = Icons.Outlined.Image,
                    )
                    GadgetSecondaryButton(
                        onClick = { onShare(uri) },
                        text = stringResource(R.string.camera_btn_share),
                        leadingIcon = Icons.Outlined.Share,
                    )
                }
            }
        }
    }
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
            GadgetEmptyState(
                title = stringResource(R.string.camera_history_empty),
                icon = Icons.Outlined.HistoryToggleOff,
                modifier = Modifier.fillMaxWidth(),
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

private fun openPhoto(context: Context, uri: Uri) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun sharePhoto(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/jpeg")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching {
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
    val HistoryCopyIconButtonSize: Dp = 36.dp
}

@GadgetPreviewLightDark
@Composable
private fun CameraScreenPreview() = GadgetThemedPreview {
    CameraScreenContent(
        state = CameraState(permissionGranted = false, mode = CameraMode.Photo, photoError = "Camera is busy"),
        history = emptyList(),
        moduleInfo = null,
        onRequestPermission = {},
        onModeChange = {},
        onScanDetected = {},
        onPhotoSaved = {},
        onPhotoError = {},
        onOpenPhoto = {},
        onSharePhoto = {},
        onCopy = {},
        onShare = {},
        onClearHistory = {},
    )
}
