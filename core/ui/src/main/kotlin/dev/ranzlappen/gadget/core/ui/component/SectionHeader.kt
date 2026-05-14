package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.SectionHeaderEmphasis.Loud
import dev.ranzlappen.gadget.core.ui.component.SectionHeaderEmphasis.Quiet

/**
 * Visual weight presets for [SectionHeader].
 *
 * - [Quiet] — small caps, neutral color. Default; sits as a subtle
 *   divider above a card grid.
 * - [Loud] — larger title-styled text, primary on-surface color. Use
 *   sparingly for screen-internal navigation, e.g. "Recently triggered"
 *   on a heavyweight automation screen.
 */
enum class SectionHeaderEmphasis { Quiet, Loud }

/**
 * Lightweight section divider with a label.
 *
 * Sits between groups of DashCards or list rows. Trailing slot is for
 * inline actions ("See all →", a count chip, a filter dropdown);
 * defaults to nothing.
 *
 * Heavier than a raw [Text] (it has structured padding, optional
 * trailing slot, and a quiet/loud variant) but cheaper than a full
 * [ScreenHeader] — which appears only once per screen.
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    emphasis: SectionHeaderEmphasis = Quiet,
    trailing: @Composable (() -> Unit)? = null,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = spacing.medium,
                vertical = spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (emphasis == Quiet) label.uppercase() else label,
            style = when (emphasis) {
                Quiet -> MaterialTheme.typography.labelMedium
                Loud -> MaterialTheme.typography.titleMedium
            },
            color = when (emphasis) {
                Quiet -> MaterialTheme.colorScheme.onSurfaceVariant
                Loud -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
