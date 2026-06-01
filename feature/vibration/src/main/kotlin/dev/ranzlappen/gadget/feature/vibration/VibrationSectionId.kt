package dev.ranzlappen.gadget.feature.vibration

/**
 * Stable, persisted collapse-state ids for the vibration screen's cards. The
 * vibration-owned cards in [hoisted] flow through [VibrationViewModel] into
 * [VibrationScreenState.expandedSections]; [Monitor] / [LiveMonitor] are passed
 * as `collapseId` to the reusable monitor containers (which persist via the
 * same `CollapseStateRepository`). Mirror of `TorchSectionId`.
 */
internal object VibrationSectionId {
    const val Controls = "vibration_controls"
    const val PatternBuilder = "vibration_pattern_builder"
    const val Patterns = "vibration_patterns"
    const val Widgets = "vibration_widgets"
    const val RootTools = "vibration_root_tools"
    const val Monitor = "vibration_monitor"
    const val LiveMonitor = "vibration_live_monitor"

    /** Cards whose collapse state the screen hoists (not the self-managing
     *  monitor containers). */
    val hoisted = listOf(Controls, PatternBuilder, Patterns, Widgets, RootTools)
}
