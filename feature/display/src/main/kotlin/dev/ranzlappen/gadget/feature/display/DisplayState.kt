package dev.ranzlappen.gadget.feature.display

/**
 * One entry in the refresh-rate picker, sourced from the standard
 * [android.view.Display.getSupportedModes] API (readable on both flavors,
 * no root required). Selecting an entry only *applies* on the rooted
 * flavor — [dev.ranzlappen.gadget.feature.display.control.DisplayController]
 * reports [dev.ranzlappen.gadget.feature.display.control.DisplayControllerResult.Unsupported]
 * on standard, surfaced via [DisplayState.statusMessage].
 */
data class RefreshRateOption(
    val modeId: Int,
    val refreshRateHz: Float,
)

/**
 * UI state for the Display screen. Standard-tier readouts (brightness,
 * refresh rate, rotation, resolution) are always populated; the
 * rooted-only fields ([densityDpi] mutation, [surfaceFlingerExcerpt])
 * stay at their defaults until an operation succeeds.
 */
data class DisplayState(
    val isRootedFlavor: Boolean = false,

    // Standard-tier brightness (Settings.System.SCREEN_BRIGHTNESS).
    val brightnessPercent: Int = 0,
    val brightnessWritable: Boolean = false,

    // Standard-tier read-only readouts.
    val refreshRateHz: Float = 0f,
    val availableRefreshRates: List<RefreshRateOption> = emptyList(),
    val selectedRefreshRateHz: Float? = null,
    val rotationDegrees: Int = 0,
    val resolutionWidth: Int = 0,
    val resolutionHeight: Int = 0,

    // Rooted-only density override.
    val densityDpi: Int = DEFAULT_DENSITY_DPI,

    // Busy flags per privileged operation, so only the relevant control
    // shows a loading state instead of the whole screen.
    val isApplyingRefreshRate: Boolean = false,
    val isApplyingDensity: Boolean = false,
    val isResetting: Boolean = false,
    val isLoadingSurfaceFlinger: Boolean = false,

    // Rooted-only read-only diagnostic panel content.
    val surfaceFlingerExcerpt: String? = null,

    // Last operation's human-readable outcome, shown until the next
    // operation replaces it.
    val statusMessage: String? = null,
) {
    companion object {
        /** Matches DensityHelper's mid-range default; a safe starting
         *  point for the slider before any override has been read back. */
        const val DEFAULT_DENSITY_DPI = 420
        /** Mirrors DensityHelper.DENSITY_MIN_DPI (the rooted clamp floor);
         *  duplicated here since DisplayController exposes no query
         *  method for it — the slider range must match the helper's
         *  clamp so the UI never shows an unreachable value as valid. */
        const val MIN_DENSITY_DPI = 120
        /** Mirrors DensityHelper.DENSITY_MAX_DPI. */
        const val MAX_DENSITY_DPI = 560
    }
}

/** User-driven intents from [DisplayScreenContent]. */
sealed interface DisplayUiEvent {
    /** Fired once on slider release (not per drag frame) — see [dev.ranzlappen.gadget.core.ui.component.GadgetSlider]. */
    data class BrightnessCommitted(val percent: Int) : DisplayUiEvent
    data class RefreshRateSelected(val option: RefreshRateOption) : DisplayUiEvent
    data class DensityCommitted(val dpi: Int) : DisplayUiEvent
    data object SurfaceFlingerSnapshotRequested : DisplayUiEvent
    data object ResetAllRequested : DisplayUiEvent
    data object ReadoutsRefreshRequested : DisplayUiEvent
}
