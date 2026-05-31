package dev.ranzlappen.gadget.feature.torch.standard

import dev.ranzlappen.gadget.feature.torch.TorchRootAvailability
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.TorchRootResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor no-op for the modular Torch root seam. The rooted
 * capabilities never exist without the rooted build, so availability is
 * always [TorchRootAvailability.Unavailable] and every action reports
 * [TorchRootResult.Unsupported]. Shared UI hides the root controls and the
 * per-function badges render red ("requires the rooted app version").
 *
 * Brightness is binary on standard: the ceiling is a constant 100% and there's
 * no boost, so [maxBrightnessPercentFlow] never leaves 100 and
 * [commandedBrightnessPercent] stays a constant 0 (the metric falls back to
 * its Camera2 on/off reading).
 *
 * **Namespace:** this no-op lives in `:feature:torch-standard` under
 * `dev.ranzlappen.gadget.feature.torch.standard`, wired into the standard
 * variant via `standardImplementation` and bound by `StandardTorchModule`. It
 * mirrors `:feature:torch-rooted`'s `RootedTorchRootCapabilities`, so neither
 * flavor's Torch impls live in `:app`. The root-safety framework it would
 * otherwise lean on lives in `:core:root`.
 */
@Singleton
class StandardTorchRootCapabilities @Inject constructor() : TorchRootCapabilities {
    override val isRootedFlavor: Boolean = false
    override val maxBrightnessPercentFlow: StateFlow<Int> = MutableStateFlow(100).asStateFlow()
    override val commandedBrightnessPercent: StateFlow<Int> = MutableStateFlow(0)
    override fun hasRootAccess(): Boolean = false
    override suspend fun probe(): TorchRootAvailability = TorchRootAvailability.Unavailable
    override suspend fun boostBrightness(percent: Int): TorchRootResult = TorchRootResult.Unsupported
    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = TorchRootResult.Unsupported
    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchRootResult = TorchRootResult.Unsupported
    override suspend fun thermalOverrideStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = TorchRootResult.Unsupported
}
