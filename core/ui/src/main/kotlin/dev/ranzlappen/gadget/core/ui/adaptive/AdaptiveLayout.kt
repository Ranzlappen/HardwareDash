package dev.ranzlappen.gadget.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/**
 * High-level layout-mode signal derived from [LocalWindowSizeClass].
 *
 * Tiers:
 *
 * - [SinglePane] — width < 600 dp (typical phone portrait). Stack
 *   content vertically; never split panes.
 * - [TwoPane] — width 600–840 dp (foldable open, tablet portrait,
 *   small landscape). Primary content + optional secondary pane on
 *   the right.
 * - [ThreePane] — width > 840 dp (tablet landscape, Chromebook,
 *   desktop window). Primary content + secondary pane +
 *   nav-rail-with-labels.
 *
 * Phase 1 wires `SinglePane` / `TwoPane` only — `ThreePane` is
 * recognised but currently maps to the same layout as `TwoPane`
 * pending the Phase-2 list-detail-extra layout work.
 */
@Immutable
enum class GadgetLayoutMode {
    SinglePane,
    TwoPane,
    ThreePane,
}

/**
 * Resolves the active [GadgetLayoutMode] from
 * [LocalWindowSizeClass].current.widthSizeClass.
 *
 * Most feature screens should consume this helper rather than reading
 * `LocalWindowSizeClass` directly — the layout-mode abstraction is
 * the **stable** seam, while `WindowSizeClass` is implementation
 * detail (and its API may shift between M3 versions).
 *
 * Example:
 *
 * ```kotlin
 * when (rememberLayoutMode()) {
 *     SinglePane -> CompactDashboard()
 *     TwoPane, ThreePane -> SplitPaneDashboard()
 * }
 * ```
 */
@Composable
fun rememberLayoutMode(): GadgetLayoutMode {
    val widthClass = LocalWindowSizeClass.current.widthSizeClass
    return when (widthClass) {
        WindowWidthSizeClass.Compact -> GadgetLayoutMode.SinglePane
        WindowWidthSizeClass.Medium -> GadgetLayoutMode.TwoPane
        WindowWidthSizeClass.Expanded -> GadgetLayoutMode.ThreePane
        else -> GadgetLayoutMode.SinglePane
    }
}

/**
 * Thin wrapper combining [BoxWithConstraints] with the active
 * [GadgetLayoutMode]. Use when a layout decision needs **both**
 * pixel constraints (e.g. shimmer width scaling) and the semantic
 * size class (e.g. "should we show two panes?").
 *
 * The receiver of [content] is `(BoxWithConstraintsScope,
 * GadgetLayoutMode) -> Unit` so callers don't have to nest two
 * lambda receivers manually.
 *
 * Example:
 *
 * ```kotlin
 * BoxWithConstraintsAdaptive { mode ->
 *     val skeletonWidth = maxWidth  // BoxWithConstraintsScope.maxWidth
 *     if (mode == SinglePane) {
 *         Column { … }
 *     } else {
 *         Row { … }
 *     }
 * }
 * ```
 */
@Composable
fun BoxWithConstraintsAdaptive(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(GadgetLayoutMode) -> Unit,
) {
    val mode = rememberLayoutMode()
    BoxWithConstraints(modifier = modifier) {
        content(mode)
    }
}
