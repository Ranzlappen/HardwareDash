package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.feature.settings.BuildConfig

/**
 * Static About card — version / flavor / build type read from
 * the feature module's generated [BuildConfig].
 *
 * Cheapest possible Settings card — no state, no DataStore touch,
 * no permission interaction. Renders inside the Settings screen's
 * functional slot so it sits at the top of the scroll.
 */
@Composable
internal fun AboutCard(modifier: Modifier = Modifier) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = "About",
        icon = Icons.Outlined.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            AboutRow(label = "Version", value = BuildConfig.VERSION_NAME ?: "—")
            AboutRow(label = "Build", value = BuildConfig.VERSION_CODE.toString())
            AboutRow(label = "Build type", value = BuildConfig.BUILD_TYPE)
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
