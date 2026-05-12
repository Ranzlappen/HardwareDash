package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.glassSurface
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing

/**
 * Glassmorphic Material 3 dashboard tile.
 *
 * Wraps a glass surface with an optional icon-plus-title header and an
 * arbitrary content slot. Content text inherits
 * `colorScheme.onSurfaceVariant` so numeric readouts and short
 * descriptions feel secondary to the title without an explicit color
 * call at each call site.
 *
 * Typical usage on a sensor dashboard:
 *
 * ```kotlin
 * DashCard(
 *     title = "Battery",
 *     icon = Icons.Filled.BatteryFull,
 *     onClick = { nav.toBattery() },
 * ) {
 *     Text("87% • Charging", style = MaterialTheme.typography.titleLarge)
 *     SparklineChart(samples = batteryHistory)
 * }
 * ```
 *
 * Pass `intensity = GlassIntensity.Vivid` when the card sits on top of a
 * gradient or a sensor-colored hero strip; the higher transparency
 * lets the layer below bleed through.
 */
@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    intensity: GlassIntensity = GlassIntensity.Standard,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(GadgetSpacing.Medium),
    content: @Composable () -> Unit,
) {
    val baseModifier = modifier.glassSurface(intensity = intensity)
    val interactiveModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }
    Box(modifier = interactiveModifier) {
        Column(modifier = Modifier.padding(contentPadding)) {
            if (title != null || icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        if (title != null) {
                            Spacer(modifier = Modifier.width(GadgetSpacing.Tiny))
                        }
                    }
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(GadgetSpacing.Small))
            }
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                content()
            }
        }
    }
}
