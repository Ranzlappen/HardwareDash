package dev.ranzlappen.gadget.feature.manual

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

@Composable
fun ManualScreen(modifier: Modifier = Modifier) {
    ManualScreenContent(modifier = modifier)
}

@Composable
internal fun ManualScreenContent(modifier: Modifier = Modifier) {
    ModuleScreenScaffold(
        title = stringResource(R.string.manual_screen_title),
        modifier = modifier,
        moduleInfo = null,
        functional = {
            ManualSection(
                title = stringResource(R.string.manual_intro_title),
                body = stringResource(R.string.manual_intro_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_sensors),
                body = stringResource(R.string.manual_section_sensors_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_radios),
                body = stringResource(R.string.manual_section_radios_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_actuators),
                body = stringResource(R.string.manual_section_actuators_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_system),
                body = stringResource(R.string.manual_section_system_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_automation),
                body = stringResource(R.string.manual_section_automation_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_monitoring),
                body = stringResource(R.string.manual_section_monitoring_body),
            )
            ManualSection(
                title = stringResource(R.string.manual_section_rooted),
                body = stringResource(R.string.manual_section_rooted_body),
            )
        },
    )
}

@Composable
private fun ManualSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    DashCard(modifier = modifier.fillMaxWidth(), title = title) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun ManualScreenPreview() = GadgetThemedPreview { ManualScreenContent() }
