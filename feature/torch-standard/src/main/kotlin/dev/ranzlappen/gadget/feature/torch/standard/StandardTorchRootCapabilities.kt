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
 * **Namespace:** this no-op lives under `dev.ranzlappen.gadget.feature.torch
 * .standard` (not the legacy `com.gadget.torch`) so the blueprint's flavor
 * seam matches the modular package convention. The root-safety framework
 * extracted to `:core:root` in refactor-2026 D1; the follow-up E2 moves this
 * no-op into `:feature:torch-rooted` (and the rooted impl alongside it). It
 * still physically resides in `app/src/standard/` until that lands.
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
