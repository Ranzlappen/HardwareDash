package com.gadget.torch

import dev.ranzlappen.gadget.feature.torch.TorchRootAvailability
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.TorchRootResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor no-op for the modular Torch root seam. The rooted
 * capabilities never exist without the rooted build, so availability is
 * always [TorchRootAvailability.Unavailable] and every action reports
 * [TorchRootResult.Unsupported]. Shared UI hides the root controls and the
 * per-function badges render red ("requires the rooted app version").
 */
@Singleton
class StandardTorchRootCapabilities @Inject constructor() : TorchRootCapabilities {
    override val isRootedFlavor: Boolean = false
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
