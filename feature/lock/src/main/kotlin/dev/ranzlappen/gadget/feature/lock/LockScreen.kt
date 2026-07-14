package dev.ranzlappen.gadget.feature.lock

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.lock.LockStateMetricSource.Companion.METRIC_KEY

@Composable
fun LockScreen(
    modifier: Modifier = Modifier,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lockAction by viewModel.lockAction.collectAsState()
    LockScreenContent(
        state = state,
        moduleInfo = lockModuleInfo(state),
        modifier = modifier,
        deviceAdmin = {
            DeviceAdminCard(
                lockAction = lockAction,
                onLockNow = viewModel::onLockNow,
                isAdminActive = viewModel::isDeviceAdminActive,
                activationIntentFor = viewModel::adminActivationIntent,
            )
        },
        liveMonitors = {
            LiveMonitorContainer(metricKey = METRIC_KEY, title = stringResource(R.string.lock_live_monitor_state))
        },
        monitors = {
            MonitorContainer(metricKey = METRIC_KEY, title = stringResource(R.string.lock_monitor_state))
        },
    )
}

@Composable
private fun DeviceAdminCard(
    lockAction: RootActionState,
    onLockNow: () -> Unit,
    isAdminActive: () -> Boolean,
    activationIntentFor: (String) -> Intent,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var adminActive by remember { mutableStateOf(isAdminActive()) }
    // Re-check admin state on resume (the user may have just activated it in
    // the system prompt and navigated back) — the AccessibilityCard idiom.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) adminActive = isAdminActive()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var showConfirm by remember { mutableStateOf(false) }
    val explanation = stringResource(R.string.lock_admin_explanation)

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.lock_admin_card_title),
        icon = Icons.Filled.Lock,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = stringResource(
                    if (adminActive) R.string.lock_admin_active_subtitle
                    else R.string.lock_admin_inactive_subtitle,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (adminActive) {
                GadgetSecondaryButton(
                    onClick = { showConfirm = true },
                    text = stringResource(R.string.lock_now_action),
                    enabled = !lockAction.running,
                    loading = lockAction.running,
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = lockAction.message
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = lockAction.statusKind.color(),
                    )
                }
            } else {
                GadgetSecondaryButton(
                    onClick = { ctx.startActivity(activationIntentFor(explanation)) },
                    text = stringResource(R.string.lock_admin_enable),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showConfirm) {
        GadgetDialog(
            onDismissRequest = { showConfirm = false },
            title = stringResource(R.string.lock_now_confirm_title),
            text = stringResource(R.string.lock_now_confirm_message),
            icon = Icons.Outlined.WarningAmber,
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = {
                        showConfirm = false
                        onLockNow()
                    },
                    text = stringResource(R.string.lock_now_action),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { showConfirm = false },
                    text = stringResource(R.string.lock_cancel),
                )
            },
        )
    }
}

@Composable
private fun lockModuleInfo(state: LockState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 1),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.lock_cap_keyguard_name),
            detail = stringResource(R.string.lock_cap_keyguard_detail),
            status = {
                if (state.isSecure) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.lock_cap_keyguard_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.lock_cap_keyguard_inactive),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.lock_cap_biometric_name),
            detail = stringResource(R.string.lock_cap_biometric_detail),
            status = {
                if (state.hasBiometric) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.lock_cap_biometric_available),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.lock_cap_biometric_unavailable),
                )
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.lock_cap_overlay_name),
            detail = stringResource(R.string.lock_cap_overlay_detail),
            status = {
                if (state.isRootedFlavor) CapabilityStatus(
                    kind = GadgetStatusKind.Success,
                    message = stringResource(R.string.lock_cap_rooted_active),
                ) else CapabilityStatus(
                    kind = GadgetStatusKind.Warning,
                    message = stringResource(R.string.lock_cap_rooted_required),
                )
            },
        ),
    ),
)

@Composable
internal fun LockScreenContent(
    state: LockState,
    moduleInfo: ModuleInfo?,
    modifier: Modifier = Modifier,
    deviceAdmin: @Composable () -> Unit = {},
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.lock_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            LockStatusCard(state = state)
            deviceAdmin()
            liveMonitors()
            monitors()
        },
    )
}

@Composable
private fun LockStatusCard(
    state: LockState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.lock_card_title),
        icon = Icons.Filled.Lock,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            GadgetChip(
                selected = state.isLocked,
                onClick = {},
                label = stringResource(
                    if (state.isLocked) R.string.lock_chip_locked else R.string.lock_chip_unlocked,
                ),
                enabled = false,
            )
            GadgetChip(
                selected = state.isSecure,
                onClick = {},
                label = stringResource(
                    if (state.isSecure) R.string.lock_chip_secure else R.string.lock_chip_insecure,
                ),
                enabled = false,
            )
            GadgetChip(
                selected = state.hasBiometric,
                onClick = {},
                label = stringResource(
                    if (state.hasBiometric) R.string.lock_chip_biometric else R.string.lock_chip_no_biometric,
                ),
                enabled = false,
            )
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun LockScreenLockedPreview() = GadgetThemedPreview {
    LockScreenContent(
        state = LockState(isLocked = true, isSecure = true, hasBiometric = true),
        moduleInfo = null,
    )
}

@GadgetPreviewLightDark
@Composable
private fun LockScreenUnlockedPreview() = GadgetThemedPreview {
    LockScreenContent(
        state = LockState(isLocked = false, isSecure = false, hasBiometric = false),
        moduleInfo = null,
    )
}
