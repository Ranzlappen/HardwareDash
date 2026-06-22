package dev.ranzlappen.gadget.feature.motion

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Hilt entry point for the Motion feature screen: collects live state,
 * builds the [ModuleInfo] (per-sensor capability rows), and supplies
 * the monitor containers as a slot — keeping [MotionScreenContent] (and
 * its previews) Hilt-free, per the module blueprint.
 */
@Composable
fun MotionScreen(
    modifier: Modifier = Modifier,
    viewModel: MotionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Refresh permission state each time the screen resumes (user may have
    // gone to Settings and granted the permission).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MotionScreenContent(
        state = state,
        onPermissionResult = viewModel::onPermissionResult,
        modifier = modifier,
        moduleInfo = motionModuleInfo(state),
        liveMonitors = {
            if (state.hasGyroscope) {
                LiveMonitorContainer(
                    metricKey = RotationRateMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_live_monitor_title,
                        stringResource(R.string.motion_gyroscope_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_live_monitor_${RotationRateMetricSource.METRIC_KEY}",
                )
            }
            if (state.hasStepCounter) {
                LiveMonitorContainer(
                    metricKey = StepCounterMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_live_monitor_title,
                        stringResource(R.string.motion_step_counter_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_live_monitor_${StepCounterMetricSource.METRIC_KEY}",
                )
            }
            if (state.hasMotionDetect) {
                LiveMonitorContainer(
                    metricKey = MotionDetectedMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_live_monitor_title,
                        stringResource(R.string.motion_detect_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_live_monitor_${MotionDetectedMetricSource.METRIC_KEY}",
                )
            }
        },
        monitors = {
            if (state.hasGyroscope) {
                MonitorContainer(
                    metricKey = RotationRateMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_monitor_title,
                        stringResource(R.string.motion_gyroscope_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_monitor_${RotationRateMetricSource.METRIC_KEY}",
                )
            }
            if (state.hasStepCounter) {
                MonitorContainer(
                    metricKey = StepCounterMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_monitor_title,
                        stringResource(R.string.motion_step_counter_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_monitor_${StepCounterMetricSource.METRIC_KEY}",
                )
            }
            if (state.hasMotionDetect) {
                MonitorContainer(
                    metricKey = MotionDetectedMetricSource.METRIC_KEY,
                    title = stringResource(
                        R.string.motion_monitor_title,
                        stringResource(R.string.motion_detect_card_title),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "motion_monitor_${MotionDetectedMetricSource.METRIC_KEY}",
                )
            }
        },
    )
}

@Composable
private fun motionModuleInfo(state: MotionState): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 9),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.motion_capability_gyroscope),
            detail = stringResource(R.string.motion_capability_gyroscope_detail),
            status = {
                if (state.hasGyroscope) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_present),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.motion_capability_not_present),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.motion_capability_step_counter),
            detail = stringResource(R.string.motion_capability_step_counter_detail),
            status = {
                if (state.hasStepCounter) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_present),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.motion_capability_not_present),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.motion_capability_motion_detect),
            detail = stringResource(R.string.motion_capability_motion_detect_detail),
            status = {
                if (state.hasMotionDetect) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_present),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Error,
                        message = stringResource(R.string.motion_capability_not_present),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.motion_capability_high_polling),
            detail = stringResource(R.string.motion_capability_high_polling_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.motion_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.motion_capability_raw_unfiltered),
            detail = stringResource(R.string.motion_capability_raw_unfiltered_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.motion_capability_rooted_required),
                    )
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.motion_capability_sysfs_read),
            detail = stringResource(R.string.motion_capability_sysfs_read_detail),
            status = {
                if (state.isRootedFlavor) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.motion_capability_rooted_active),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.motion_capability_rooted_required),
                    )
                }
            },
        ),
    ),
)

/**
 * Stateless Motion screen content: capability card, optional permission
 * card, per-sensor readout cards, and monitor slots.
 */
@Composable
internal fun MotionScreenContent(
    state: MotionState,
    onPermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    moduleInfo: ModuleInfo? = null,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.motion_capability_card_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            MotionCapabilityCard(state = state)
            if (state.hasStepCounter && !state.activityPermissionGranted) {
                ActivityPermissionCard(onPermissionResult = onPermissionResult)
            }
            if (state.hasGyroscope) {
                GyroscopeCard(state = state)
            }
            if (state.hasStepCounter) {
                StepCounterCard(state = state)
            }
            if (state.hasMotionDetect) {
                MotionDetectCard(state = state)
            }
            liveMonitors()
            monitors()
        },
    )
}

@Composable
private fun MotionCapabilityCard(state: MotionState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.motion_capability_card_title),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GadgetChip(
                selected = state.hasGyroscope,
                onClick = {},
                enabled = false,
                label = stringResource(R.string.motion_capability_gyroscope),
            )
            GadgetChip(
                selected = state.hasStepCounter,
                onClick = {},
                enabled = false,
                label = stringResource(R.string.motion_capability_step_counter),
            )
            GadgetChip(
                selected = state.hasMotionDetect,
                onClick = {},
                enabled = false,
                label = stringResource(R.string.motion_capability_motion_detect),
            )
        }
    }
}

@Composable
private fun ActivityPermissionCard(
    onPermissionResult: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult,
    )
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.motion_permission_title),
    ) {
        Text(
            text = stringResource(R.string.motion_permission_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        GadgetPrimaryButton(
            onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
            text = stringResource(R.string.motion_permission_grant),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GyroscopeCard(state: MotionState, modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.motion_gyroscope_card_title),
    ) {
        Text(
            text = "%.2f rad/s".format(state.rotationRate),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.motion_gyroscope_value),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StepCounterCard(state: MotionState, modifier: Modifier = Modifier) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.motion_step_counter_card_title),
    ) {
        Text(
            text = state.stepCount.toInt().toString(),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.motion_steps_label),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MotionDetectCard(state: MotionState, modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.motion_detect_card_title),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GadgetChip(
                selected = state.motionDetected,
                onClick = {},
                enabled = false,
                label = if (state.motionDetected) {
                    stringResource(R.string.motion_detected)
                } else {
                    stringResource(R.string.motion_idle)
                },
            )
        }
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun MotionScreenPreview() = GadgetThemedPreview {
    MotionScreenContent(
        state = MotionState(
            hasGyroscope = true,
            hasStepCounter = true,
            hasMotionDetect = false,
            rotationRate = 1.23f,
            stepCount = 4567f,
            motionDetected = false,
            activityPermissionGranted = true,
        ),
        onPermissionResult = {},
        moduleInfo = null,
    )
}
