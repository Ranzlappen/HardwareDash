package dev.ranzlappen.gadget.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.adaptive.GadgetLayoutMode
import dev.ranzlappen.gadget.core.ui.adaptive.GadgetPosture
import dev.ranzlappen.gadget.core.ui.adaptive.rememberLayoutMode
import dev.ranzlappen.gadget.core.ui.adaptive.rememberPosture
import dev.ranzlappen.gadget.core.ui.module.ModuleCapabilitiesSection
import dev.ranzlappen.gadget.core.ui.module.ModuleCompatibilitySection
import dev.ranzlappen.gadget.core.ui.module.ModuleFirmwareSection
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermissionsSection

/**
 * Per-module screen scaffold
 * Provides:
 *  - Full vertical scroll via `Column(Modifier.verticalScroll(...))`.
 *  - Optional [title] header slot.
 *  - The module-blueprint sections, driven declaratively by an optional
 *    [moduleInfo]. When supplied, the scaffold renders the standard
 *    **Permissions**, **OS compatibility**, and (when present)
 *    **Firmware** cards automatically — so every module gets a
 *    consistent metadata block without hand-rolling it. See
 *    [ModuleInfo].
 *  - Two free-form section slots ([functional], [disclaimer]) for the
 *    common pattern observed in the legacy app:
 *
 *      1. functional   — the module's primary content
 *      2. moduleInfo   — permissions / OS-compat / firmware (auto)
 *      3. disclaimer   — collapsible safety / legal note
 *  - An optional [secondaryPane] slot that adapts to both the active
 *    [GadgetLayoutMode] and the device's [GadgetPosture]:
 *    - On [GadgetLayoutMode.SinglePane] it is omitted entirely.
 *    - On [GadgetLayoutMode.TwoPane] or [ThreePane] + [GadgetPosture.Flat]
 *      or [GadgetPosture.Book]: rendered to the **right** of the primary
 *      column (landscape split — primary ≈ 60 % + secondary ≈ 40 %).
 *    - On [GadgetPosture.Tabletop] (foldable held half-open like a laptop):
 *      stacked **below** the primary content instead, so content sits in
 *      the top half and controls / supplementary info in the bottom half
 *      (both at equal height, separated by the hinge).
 *    Callers should treat the pane as "supplementary content for wider
 *    / foldable screens" rather than something the primary flow depends on.
 *
 * Modules can add/remove sections freely — **this is only a common
 * pattern** for sensor/actuator modules, not enforced. A settings-style
 * module might use only [functional]; a hero/intro module might use
 * only [title] + [functional]. Pass `null` for any slot you don't need
 * and it is omitted from the layout entirely (no empty `Spacer` left
 * behind by [Arrangement.spacedBy]).
 *
 * Custom layouts inside slots are encouraged — each slot receives a
 * [ColumnScope] receiver so callers can `Spacer(...)`,
 * `Modifier.weight(...)`, etc., as needed. Slots that want richer
 * structure (rows of chips, a `LazyRow` of cards) can also do that.
 *
 * For modules that need a non-3-section layout entirely, prefer
 * building a screen directly rather than passing `null` for every slot
 * — the scaffold is a convenience for the *common* shape, not the only
 * shape.
 */
@Composable
fun ModuleScreenScaffold(
    title: String? = null,
    modifier: Modifier = Modifier,
    functional: (@Composable ColumnScope.() -> Unit)? = null,
    moduleInfo: ModuleInfo? = null,
    disclaimer: (@Composable ColumnScope.() -> Unit)? = null,
    secondaryPane: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val layoutMode = rememberLayoutMode()
    val posture = rememberPosture()
    val showSecondary = secondaryPane != null && layoutMode != GadgetLayoutMode.SinglePane

    val primary: @Composable () -> Unit = {
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(
                    horizontal = spacing.medium,
                    vertical = spacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            functional?.invoke(this)
            if (moduleInfo != null) {
                ModulePermissionsSection(permissions = moduleInfo.permissions)
                ModuleCompatibilitySection(compatibility = moduleInfo.compatibility)
                moduleInfo.firmware?.let { ModuleFirmwareSection(firmware = it) }
                ModuleCapabilitiesSection(capabilities = moduleInfo.capabilities)
            }
            disclaimer?.invoke(this)
        }
    }

    if (showSecondary && posture == GadgetPosture.Tabletop) {
        // Tabletop posture: foldable held half-open like a laptop. Stack primary
        // (content) on the top half and secondary (controls / supplementary) on
        // the bottom half, respecting the horizontal hinge between them.
        Column(modifier = modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) { primary() }
            val secondaryScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(secondaryScroll)
                    .padding(
                        horizontal = spacing.medium,
                        vertical = spacing.large,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.large),
            ) {
                secondaryPane!!.invoke(this)
            }
        }
    } else if (showSecondary) {
        // Flat or Book posture on a wide screen: side by side.
        // Primary takes ~60% on TwoPane, ~50% on ThreePane.
        Row(modifier = modifier.fillMaxSize()) {
            val primaryWeight = when (layoutMode) {
                GadgetLayoutMode.TwoPane -> 1.5f
                GadgetLayoutMode.ThreePane -> 1f
                GadgetLayoutMode.SinglePane -> 1f
            }
            Row(modifier = Modifier.weight(primaryWeight)) {
                primary()
            }
            val secondaryScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(secondaryScroll)
                    .padding(
                        horizontal = spacing.medium,
                        vertical = spacing.large,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.large),
            ) {
                secondaryPane!!.invoke(this)
            }
        }
    } else {
        Row(modifier = modifier) { primary() }
    }
}
