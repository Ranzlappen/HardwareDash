package dev.ranzlappen.gadget.feature.torch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig

/**
 * Stateless view-state container consumed by [TorchScreenContent].
 *
 * Produced by [TorchViewModel.state] from the four reactive sources
 * the screen depends on:
 * - [TorchController.state] (live torch hardware snapshot)
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }]
 * - [TorchWidgetConfigRepository.all] (saved widget configs)
 * - [StrobeRuntime.running] (live strobe-running signal, no polling)
 *
 * `@Immutable` so Compose skips recompositions when the structural
 * value is unchanged across emissions.
 */
@Immutable
data class TorchScreenState(
    val torch: TorchState,
    val defaultStrobeRateHz: Float,
    val widgets: List<SavedTorchWidget>,
    val strobeRunning: Boolean = false,
    val morseText: String = "",
    val rootAvailability: TorchRootAvailability = TorchRootAvailability.Unavailable,
    /** Persisted expanded/collapsed state per [TorchSectionId]; missing =
     *  expanded. */
    val expandedSections: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        /** First-emission placeholder used before the flows emit. */
        val Initial = TorchScreenState(
            torch = TorchState(),
            defaultStrobeRateHz = TorchWidgetConfig.DEFAULT_RATE_HZ,
            widgets = emptyList(),
        )
    }
}

/** A single persisted widget — `appWidgetId` keyed [TorchWidgetConfig]. */
@Immutable
data class SavedTorchWidget(
    val appWidgetId: Int,
    val config: TorchWidgetConfig,
)

@Composable
internal fun TorchState.statusMessage(): String = when {
    !isAvailable && error == TorchError.NoFlashUnit ->
        stringResource(R.string.torch_status_no_flash)
    error == TorchError.HardwareError ->
        stringResource(R.string.torch_status_hardware_error)
    error == TorchError.PermissionDenied ->
        stringResource(R.string.torch_status_permission_denied)
    isOn -> stringResource(R.string.torch_status_on)
    else -> stringResource(R.string.torch_status_off)
}
