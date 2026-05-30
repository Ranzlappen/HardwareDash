package dev.ranzlappen.gadget.feature.torch.rooted

import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsController
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.torch.TorchRootAvailability
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import dev.ranzlappen.gadget.feature.torch.TorchRootResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import dev.ranzlappen.gadget.feature.torch.sysfs.TorchSysfsControllerResult as SysfsResult
import dev.ranzlappen.gadget.feature.torch.TorchController as ModularTorchController

/**
 * Rooted-flavor adapter that surfaces the new modular Torch screen's root
 * capabilities by delegating to the rooted sysfs Torch controller
 * (`RootedTorchController`, injected via the rooted-bound [TorchSysfsController]).
 * This reuses the battle-tested sysfs / libsu paths and the `RootSafetyGate`
 * gating rather than re-implementing them, and maps the sysfs result tiers
 * onto the modular [TorchRootResult].
 *
 * It also tracks the live commanded brightness: a successful boost records
 * the commanded percent; turning the torch off (observed via the modular
 * Camera2 controller) clears it back to 0 so a subsequent normal on reads as
 * 100 rather than a stale boost. The monitoring metric folds this with the
 * on/off state to chart real intensity up to the boost ceiling.
 */
@Singleton
class RootedTorchRootCapabilities @Inject constructor(
    private val registry: RootCapabilityRegistry,
    private val sysfs: TorchSysfsController,
    private val paths: TorchSysfsPaths,
    modularController: ModularTorchController,
) : TorchRootCapabilities {

    override val isRootedFlavor: Boolean get() = registry.isRootedFlavor

    override val maxBrightnessPercent: Int = BRIGHTNESS_BOOST_CAP_PERCENT

    private val _commandedBrightnessPercent = MutableStateFlow(0)
    override val commandedBrightnessPercent: StateFlow<Int> =
        _commandedBrightnessPercent.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Clear the commanded boost whenever the torch turns off — a direct
        // sysfs boost doesn't survive a Camera2 off→on cycle, so the next
        // normal on must read as 100, not the previous boost.
        scope.launch {
            modularController.state.collect { state ->
                if (!state.isOn) _commandedBrightnessPercent.value = 0
            }
        }
    }

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
        sysfs.boostBrightness(percent).toModular().also { result ->
            if (result is TorchRootResult.Ok) {
                _commandedBrightnessPercent.value = percent.coerceIn(0, maxBrightnessPercent)
            }
        }

    override suspend fun dutyCycleStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = sysfs.dutyCycleStrobe(frequencyHz, dutyPercent, durationMillis).toModular()

    override suspend fun multiLedActivate(
        durationMillis: Long,
        includeScreen: Boolean,
    ): TorchRootResult = sysfs.multiLedActivate(durationMillis, includeScreen).toModular()

    override suspend fun thermalOverrideStrobe(
        frequencyHz: Int,
        dutyPercent: Int,
        durationMillis: Long,
    ): TorchRootResult = sysfs.withThermalOverride(durationMillis) {
        sysfs.dutyCycleStrobe(frequencyHz, dutyPercent, durationMillis)
    }.toModular()

    private fun SysfsResult.toModular(): TorchRootResult = when (this) {
        SysfsResult.Ok -> TorchRootResult.Ok
        SysfsResult.Unsupported -> TorchRootResult.Unsupported
        SysfsResult.OptedOut -> TorchRootResult.OptedOut
        is SysfsResult.RateLimited -> TorchRootResult.RateLimited(retryAfterMillis)
        is SysfsResult.HardwareError -> TorchRootResult.Error(message)
    }
}
