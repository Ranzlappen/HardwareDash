package dev.ranzlappen.gadget.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.settings.AppLanguage
import dev.ranzlappen.gadget.feature.settings.R

/**
 * Per-app language picker (W8) — a row of [GadgetChip]s over
 * [AppLanguage]. Selecting a chip calls
 * `AppCompatDelegate.setApplicationLocales(...)` (wired in
 * [dev.ranzlappen.gadget.feature.settings.SettingsViewModel]), which
 * self-persists via the platform's per-app language mechanism — no
 * [dev.ranzlappen.gadget.core.datastore.UserPreferences] field needed,
 * unlike [AppearanceCard]'s theme fields.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageCard(
    current: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.settings_language_title),
        icon = Icons.Outlined.Language,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            AppLanguage.entries.forEach { language ->
                GadgetChip(
                    selected = current == language,
                    onClick = { onLanguageChange(language) },
                    label = language.toDisplayLabel(),
                )
            }
        }
    }
}

@Composable
private fun AppLanguage.toDisplayLabel(): String = when (this) {
    AppLanguage.SystemDefault -> stringResource(R.string.settings_language_system_default)
    AppLanguage.English -> stringResource(R.string.settings_language_en)
    AppLanguage.German -> stringResource(R.string.settings_language_de)
    AppLanguage.Spanish -> stringResource(R.string.settings_language_es)
    AppLanguage.French -> stringResource(R.string.settings_language_fr)
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun LanguageCardPreview() = GadgetThemedPreview {
    Column {
        LanguageCard(current = AppLanguage.SystemDefault, onLanguageChange = {})
    }
}
