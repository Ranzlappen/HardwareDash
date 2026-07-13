package dev.ranzlappen.gadget.feature.display

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.display.control.DensityOverrideConfig
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.display.control.DisplayControllerResult
import dev.ranzlappen.gadget.feature.display.control.RefreshRateOverrideConfig
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the display screen (W6 in-screen surface). */
data class DisplayRootToolsState(
    val surfaceFlinger: RootActionState = RootActionState(),
)

@HiltViewModel
class DisplayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: DisplayController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val isRootedFlavor = rootCapabilityRegistry.isRootedFlavor

    // ViewModel.clear() cancels viewModelScope's Job BEFORE it invokes
    // onCleared() (it's stored as a Closeable tag, closed ahead of the
    // onCleared() call) — so a fire-on-exit revert launched via
    // viewModelScope from inside onCleared() would start into an
    // already-cancelled scope and never actually run the suspend body.
    // This module-private scope outlives that cancellation long enough
    // for the one-shot revert call to complete.
    private val exitScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(DisplayState(isRootedFlavor = isRootedFlavor))
    val state: StateFlow<DisplayState> = _state.asStateFlow()

    private val _rootTools = MutableStateFlow(DisplayRootToolsState())

    /** Live status of the rooted read-only SurfaceFlinger dump. */
    val rootTools: StateFlow<DisplayRootToolsState> = _rootTools.asStateFlow()

    private val brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            _state.update { it.copy(brightnessPercent = readBrightnessPercent()) }
        }
    }

    init {
        refreshReadouts()
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            brightnessObserver,
        )
    }

    fun onEvent(event: DisplayUiEvent) {
        when (event) {
            is DisplayUiEvent.BrightnessCommitted -> setBrightness(event.percent)
            is DisplayUiEvent.RefreshRateSelected -> overrideRefreshRate(event.option)
            is DisplayUiEvent.DensityCommitted -> overrideDensity(event.dpi)
            DisplayUiEvent.SurfaceFlingerSnapshotRequested -> loadSurfaceFlingerSnapshot()
            DisplayUiEvent.ResetAllRequested -> resetAll()
            DisplayUiEvent.ReadoutsRefreshRequested -> refreshReadouts()
        }
    }

    /** Re-reads every standard-tier readout: no root, no permission needed. */
    fun refreshReadouts() {
        val display = currentDisplay()
        val metrics = context.resources.displayMetrics
        val rotationDegrees = when (display?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        val modes = display?.supportedModes.orEmpty()
            .map { RefreshRateOption(modeId = it.modeId, refreshRateHz = it.refreshRate) }
            .distinctBy { it.refreshRateHz.toInt() }
            .sortedBy { it.refreshRateHz }
        val currentModeId = display?.mode?.modeId
        _state.update {
            it.copy(
                brightnessPercent = readBrightnessPercent(),
                brightnessWritable = Settings.System.canWrite(context),
                refreshRateHz = display?.refreshRate ?: 0f,
                availableRefreshRates = modes,
                selectedRefreshRateHz = modes.firstOrNull { mode -> mode.modeId == currentModeId }?.refreshRateHz,
                rotationDegrees = rotationDegrees,
                resolutionWidth = metrics.widthPixels,
                resolutionHeight = metrics.heightPixels,
            )
        }
    }

    private fun setBrightness(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        if (!Settings.System.canWrite(context)) {
            _state.update { it.copy(statusMessage = context.getString(R.string.display_status_write_settings_denied)) }
            return
        }
        val raw = (clamped * MAX_RAW_BRIGHTNESS / 100).coerceIn(0, MAX_RAW_BRIGHTNESS)
        runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
        }.onSuccess {
            _state.update { it.copy(brightnessPercent = clamped, statusMessage = null) }
        }.onFailure {
            _state.update { it.copy(statusMessage = context.getString(R.string.display_status_write_settings_denied)) }
        }
    }

    private fun overrideRefreshRate(option: RefreshRateOption) {
        viewModelScope.launch {
            _state.update { it.copy(isApplyingRefreshRate = true) }
            val result = controller.overrideRefreshRate(
                RefreshRateOverrideConfig(targetModeId = option.modeId),
            )
            _state.update {
                it.copy(
                    isApplyingRefreshRate = false,
                    selectedRefreshRateHz = if (result.appliedSuccessfully()) {
                        option.refreshRateHz
                    } else {
                        it.selectedRefreshRateHz
                    },
                    statusMessage = result.toStatusMessage(),
                )
            }
        }
    }

    private fun overrideDensity(dpi: Int) {
        val clamped = dpi.coerceIn(DisplayState.MIN_DENSITY_DPI, DisplayState.MAX_DENSITY_DPI)
        viewModelScope.launch {
            _state.update { it.copy(isApplyingDensity = true) }
            val result = controller.overrideDensity(DensityOverrideConfig(dpi = clamped))
            _state.update {
                it.copy(
                    isApplyingDensity = false,
                    densityDpi = if (result.appliedSuccessfully()) clamped else it.densityDpi,
                    statusMessage = result.toStatusMessage(),
                )
            }
        }
    }

    private fun loadSurfaceFlingerSnapshot() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSurfaceFlinger = true) }
            val result = controller.surfaceFlingerSnapshot()
            _state.update {
                it.copy(
                    isLoadingSurfaceFlinger = false,
                    surfaceFlingerExcerpt = (result as? DisplayControllerResult.SurfaceFlingerExcerpt)?.excerpt
                        ?: it.surfaceFlingerExcerpt,
                    statusMessage = if (result is DisplayControllerResult.SurfaceFlingerExcerpt) {
                        null
                    } else {
                        result.toStatusMessage()
                    },
                )
            }
        }
    }

    private fun resetAll() {
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            val result = controller.resetAllDisplayMutations()
            _state.update {
                it.copy(
                    isResetting = false,
                    densityDpi = DisplayState.DEFAULT_DENSITY_DPI,
                    surfaceFlingerExcerpt = null,
                    statusMessage = result.toStatusMessage(),
                )
            }
            refreshReadouts()
        }
    }

    override fun onCleared() {
        context.contentResolver.unregisterContentObserver(brightnessObserver)
        // Fire-and-forget: best-effort revert of any active rooted
        // mutation as the screen goes away. See the [exitScope] doc above
        // for why viewModelScope can't be used here.
        exitScope.launch { controller.revertOnScreenExit() }
    }

    private fun readBrightnessPercent(): Int {
        val raw = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        return (raw * 100 / MAX_RAW_BRIGHTNESS).coerceIn(0, 100)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplay(): Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
    }

    private fun DisplayControllerResult.appliedSuccessfully(): Boolean = when (this) {
        is DisplayControllerResult.Ok,
        is DisplayControllerResult.RefreshRateSnapshot,
        is DisplayControllerResult.DensitySnapshot,
        is DisplayControllerResult.BrightnessSnapshot,
        is DisplayControllerResult.SurfaceFlingerExcerpt,
        is DisplayControllerResult.ResetCompleted,
        -> true
        DisplayControllerResult.Unsupported,
        is DisplayControllerResult.RateLimited,
        DisplayControllerResult.OptedOut,
        is DisplayControllerResult.HardwareError,
        -> false
    }

    private fun DisplayControllerResult.toStatusMessage(): String? = when (this) {
        is DisplayControllerResult.Ok -> statusNote ?: context.getString(R.string.display_status_ok)
        DisplayControllerResult.Unsupported -> context.getString(R.string.display_status_unsupported)
        is DisplayControllerResult.RateLimited ->
            context.getString(R.string.display_status_rate_limited, retryAfterMillis)
        DisplayControllerResult.OptedOut -> context.getString(R.string.display_status_opted_out)
        is DisplayControllerResult.HardwareError -> message
        is DisplayControllerResult.ResetCompleted ->
            context.getString(R.string.display_status_reset_completed, restored, failed)
        is DisplayControllerResult.BrightnessSnapshot ->
            context.getString(R.string.display_status_brightness_applied, appliedRaw, maxBrightness)
        is DisplayControllerResult.RefreshRateSnapshot ->
            context.getString(R.string.display_status_refresh_applied, appliedModeId)
        is DisplayControllerResult.DensitySnapshot ->
            context.getString(R.string.display_status_density_applied, appliedDpi)
        is DisplayControllerResult.SurfaceFlingerExcerpt -> null
    }

    fun onDumpSurfaceFlinger() {
        viewModelScope.launch {
            _rootTools.update { it.copy(surfaceFlinger = it.surfaceFlinger.copy(running = true)) }
            val result = controller.surfaceFlingerSnapshot()
            _rootTools.update { it.copy(surfaceFlinger = result.toActionState()) }
        }
    }

    private fun DisplayControllerResult.toActionState(): RootActionState = when (this) {
        is DisplayControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        DisplayControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is DisplayControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        DisplayControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is DisplayControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is DisplayControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is DisplayControllerResult.BrightnessSnapshot ->
            RootActionState(message = "Brightness ${appliedRaw}/${maxBrightness}")
        is DisplayControllerResult.RefreshRateSnapshot ->
            RootActionState(message = "Mode $appliedModeId")
        is DisplayControllerResult.DensitySnapshot ->
            RootActionState(message = "Density ${appliedDpi} dpi")
        is DisplayControllerResult.SurfaceFlingerExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars")
    }

    companion object {
        private const val MAX_RAW_BRIGHTNESS = 255
    }
}
