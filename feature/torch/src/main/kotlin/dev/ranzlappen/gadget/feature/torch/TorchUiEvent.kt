package dev.ranzlappen.gadget.feature.torch

/**
 * Every user-initiated event surfaced by [TorchScreenContent].
 *
 * Replaces the previous per-callback param list (R4 #20 callback
 * explosion). The screen content flattens its public API to a single
 * `onEvent: (TorchUiEvent) -> Unit`; [TorchViewModel.onEvent] then
 * dispatches each variant to the existing typed handlers, which stay
 * as the implementation so per-action unit testing remains easy.
 *
 * Read-only queries (e.g. resolving a widget icon) stay outside this
 * type — they aren't events, they're lookups that return a value.
 */
sealed interface TorchUiEvent {
    data object ToggleClick : TorchUiEvent
    data class MomentaryHold(val active: Boolean) : TorchUiEvent

    data object StrobeToggle : TorchUiEvent
    data class StrobeHold(val active: Boolean) : TorchUiEvent

    data object MorseToggle : TorchUiEvent
    data class MorseHold(val active: Boolean) : TorchUiEvent
    data class MorseTextChange(val text: String) : TorchUiEvent

    data class RateChange(val rateHz: Float) : TorchUiEvent
    data object RateCommit : TorchUiEvent

    /** Live drag of the brightness slider (`0f..1f`). Calls [TorchController.setBrightness]
     *  immediately so the hardware responds while the user is still dragging. */
    data class BrightnessChange(val level: Float) : TorchUiEvent

    /** Slider release — persist the last brightness as the new default. */
    data object BrightnessCommit : TorchUiEvent

    /** Open the customization sheet for a brand-new widget. One entry point
     *  now that the function (flashlight / strobe / morse) is a picker inside
     *  the sheet rather than a per-kind Add button. */
    data object AddWidget : TorchUiEvent

    data class EditWidget(val widget: SavedTorchWidget) : TorchUiEvent
    data class DeleteWidget(val widget: SavedTorchWidget) : TorchUiEvent

    /** Run a rooted tool with the currently-saved parameters. */
    data object RootBoostBrightness : TorchUiEvent
    data object RootDutyStrobe : TorchUiEvent
    data object RootMultiLed : TorchUiEvent
    data object RootThermal : TorchUiEvent

    /** Live edit of the rooted-tool parameters (slider drag / toggle).
     *  Optimistic — the persisted value follows on [RootToolsCommit]. */
    data class RootToolsChange(val config: TorchRootToolsConfig) : TorchUiEvent

    /** Persist the last-edited rooted-tool parameters (slider release /
     *  toggle flip). */
    data object RootToolsCommit : TorchUiEvent

    data class SectionToggle(val id: String) : TorchUiEvent
}
