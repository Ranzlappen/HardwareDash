package com.gadget.torch

import com.gadget.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.torch.TorchRootAvailability
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.TorchRootResult
import javax.inject.Inject
import javax.inject.Singleton
import com.gadget.torch.TorchControllerResult as LegacyResult

/**
 * Rooted-flavor adapter that surfaces the new modular Torch screen's root
 * capabilities by delegating to the existing legacy rooted Torch controller
 * (`RootedTorchController`, injected via the rooted-bound [TorchController]).
 * This reuses the battle-tested sysfs / libsu paths and the `RootSafetyGate`
 * gating rather than re-implementing them, and maps the legacy result tiers
 * onto the modular [TorchRootResult].
 */
@Singleton
class RootedTorchRootCapabilities @Inject constructor(
    private val registry: RootCapabilityRegistry,
    private val legacy: TorchController,
    private val paths: TorchSysfsPaths,
) : TorchRootCapabilities {

    override val isRootedFlavor: Boolean get() = registry.isRootedFlavor

    override fun hasRootAccess(): Boolean = registry.hasRootAccess()

    override suspend fun probe(): TorchRootAvailability {
        registry.probe()
        val rootAccess = registry.hasRootAccess()
        val ledNodeFound = rootAccess && paths.resolvePrimary() != null
        return TorchRootAvailability(
            rootedFlavor = registry.isRootedFlavor,
            rootAccess = rootAccess,
            ledNodeFound = ledNodeFound,
        )
    }

    override suspend fun boostBrightness(percent: Int): TorchRootResult =
        legacy.boostBrightness(percent).toModular()

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = legacy.dutyCycleStrobe(frequencyHz, dutyPercent, durationMillis).toModular()

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchRootResult = legacy.multiLedActivate(durationMillis, includeScreen).toModular()

    override suspend fun thermalOverrideStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = legacy.withThermalOverride(durationMillis) {
        legacy.dutyCycleStrobe(frequencyHz, dutyPercent, durationMillis)
    }.toModular()

    private fun LegacyResult.toModular(): TorchRootResult = when (this) {
        LegacyResult.Ok -> TorchRootResult.Ok
        LegacyResult.Unsupported -> TorchRootResult.Unsupported
        LegacyResult.OptedOut -> TorchRootResult.OptedOut
        is LegacyResult.RateLimited -> TorchRootResult.RateLimited(retryAfterMillis)
        is LegacyResult.HardwareError -> TorchRootResult.Error(message)
    }
}
