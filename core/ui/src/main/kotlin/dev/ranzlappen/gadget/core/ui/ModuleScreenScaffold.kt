package dev.ranzlappen.gadget.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing

/**
 * Per-module screen scaffold.
 *
 * Provides:
 *  - Full vertical scroll via `Column(Modifier.verticalScroll(...))`.
 *  - Optional [title] header slot.
 *  - Three example section slots ([functional], [permissions],
 *    [disclaimer]) demonstrating the common pattern observed in the
 *    legacy app:
 *
 *      1. functional   — the module's primary content
 *      2. permissions  — required Android permissions table
 *      3. disclaimer   — collapsible safety / legal note
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
    permissions: (@Composable ColumnScope.() -> Unit)? = null,
    disclaimer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                horizontal = GadgetSpacing.Medium,
                vertical = GadgetSpacing.Large,
            ),
        verticalArrangement = Arrangement.spacedBy(GadgetSpacing.Large),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        functional?.invoke(this)
        permissions?.invoke(this)
        disclaimer?.invoke(this)
    }
}
