package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme

/**
 * Top-of-screen header — title, optional subtitle, trailing action slot.
 *
 * The subtitle's appearance is animated (fade + vertical expand) so a
 * screen whose subtitle toggles based on state (e.g. "Loading…" →
 * "Last updated 2 minutes ago") doesn't pop visibly. The title itself
 * doesn't animate; that level of motion would be distracting at the
 * top of every screen.
 *
 * Used as the first composable inside every screen's Scaffold. The
 * screen's glass background sits behind this header, not on it —
 * keeping the header non-glass means a screen's title is always
 * legible against the dark backdrop.
 *
 * Trailing slot is reserved for at most 1–2 icon buttons; anything
 * larger should live in the Scaffold's top app bar.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = spacing.medium,
                end = spacing.medium,
                top = spacing.large,
                bottom = spacing.medium,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AnimatedVisibility(
                visible = subtitle != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    // Inside AnimatedVisibility the null-check guarantees a
                    // non-null subtitle at composition time of the inner
                    // content; the `!!` is the cleanest way to express that
                    // invariant to the Kotlin null-check.
                    text = subtitle!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actions != null) {
            Box { actions() }
        }
    }
}
