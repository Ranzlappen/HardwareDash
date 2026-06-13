package dev.ranzlappen.gadget.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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

/**
 * Foldable **posture** signal — orthogonal to [GadgetLayoutMode].
 *
 * `GadgetLayoutMode` answers "how wide is the window?"; `GadgetPosture`
 * answers "is the window folded, and how?" A device can be `TwoPane`
 * **and** `Tabletop` at once (a half-opened foldable held landscape), so
 * a posture-aware screen reads both.
 *
 * - [Flat] — no separating hinge: a regular phone/tablet, or a foldable
 *   opened flat. The overwhelming default; treat everything as one plane.
 * - [Tabletop] — a **horizontal** half-opened hinge splits the window
 *   into a top and bottom half (laptop-like). Good for "content on top,
 *   controls on the bottom half" layouts.
 * - [Book] — a **vertical** half-opened hinge splits the window into a
 *   left and right page. Good for a two-page / list-detail split that
 *   avoids straddling the fold.
 */
@Immutable
enum class GadgetPosture {
    Flat,
    Tabletop,
    Book,
}

/**
 * Resolves the active [GadgetPosture] from `material3-adaptive`'s
 * [currentWindowAdaptiveInfo].
 *
 * Like [rememberLayoutMode], this is the **stable** seam — it returns the
 * Gadget enum, never a `material3-adaptive` `Posture`/`HingeInfo` type, so
 * the adaptive library's API churn stays contained to this file.
 *
 * Consumed by [dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold]: when the
 * device is in [GadgetPosture.Tabletop] posture and a `secondaryPane` is
 * supplied, the scaffold stacks the panes vertically (primary on top, secondary
 * below the hinge) instead of side-by-side. On non-folding devices it always
 * returns [Flat], so adopting it is free.
 *
 * ```kotlin
 * when (rememberPosture()) {
 *     Book -> TwoPageDetail()          // avoid straddling a vertical fold
 *     Tabletop -> TopContentBottomControls()
 *     Flat -> rememberLayoutMode().let { … }  // fall back to width tiers
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberPosture(): GadgetPosture {
    val posture = currentWindowAdaptiveInfo().windowPosture
    return when {
        posture.hingeList.isEmpty() -> GadgetPosture.Flat
        posture.isTabletop -> GadgetPosture.Tabletop
        else -> GadgetPosture.Book
    }
}
