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

    data object AddFlashlight : TorchUiEvent
    data object AddStrobe : TorchUiEvent

    /** "Pin with the default look" — skip the configuration sheet and
     *  immediately request a pin with the same default config the Add
     *  buttons would have started the sheet with. */
    data object QuickPinFlashlight : TorchUiEvent
    data object QuickPinStrobe : TorchUiEvent

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
