package dev.ranzlappen.gadget.feature.torch

/**
 * Stable, persisted collapse-state ids for the torch screen's cards. The
 * torch-owned cards in [hoisted] flow through [TorchViewModel] into
 * [TorchScreenState.expandedSections]; [Monitor] / [LiveMonitor] are passed
 * as `collapseId` to the reusable containers, which persist via the same
 * `CollapseStateRepository`.
 */
internal object TorchSectionId {
    const val Controls = "torch_controls"
    const val Brightness = "torch_brightness"
    const val StrobeDefaults = "torch_strobe_defaults"
    const val Widgets = "torch_widgets"
    const val RootTools = "torch_root_tools"
    const val Monitor = "torch_monitor"
    const val LiveMonitor = "torch_live_monitor"

    /** Cards whose collapse state the screen hoists (not the self-managing
     *  monitor containers). */
    val hoisted = listOf(Controls, Brightness, StrobeDefaults, Widgets, RootTools)
}
