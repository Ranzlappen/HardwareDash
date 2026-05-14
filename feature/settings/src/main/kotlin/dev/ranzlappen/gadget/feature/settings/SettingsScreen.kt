package dev.ranzlappen.gadget.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.feature.settings.components.AboutCard
import dev.ranzlappen.gadget.feature.settings.components.AccessibilityCard
import dev.ranzlappen.gadget.feature.settings.components.AppearanceCard

/**
 * Settings v1 — three cards under one [ModuleScreenScaffold]:
 *
 *   1. **About** — version / build info from `BuildConfig`.
 *   2. **Appearance** — dark-theme mode + dynamic colour.
 *   3. **Accessibility** — reduced motion override, reduce
 *      transparency, large text override.
 *
 * State is hoisted to [SettingsViewModel]. Each card receives
 * the current [UserPreferences] snapshot + per-field callbacks.
 * Cards never reach into the ViewModel directly — they're pure
 * presentation. This keeps them previewable without a Hilt graph.
 *
 * Heavier sections (Backup, Flipper, Keep-Alive, etc.) ship in
 * dedicated batches once the underlying managers are ported from
 * legacy-main per `docs/migration-guide.md`.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsState()
    ModuleScreenScaffold(
        title = "Settings",
        modifier = modifier,
        functional = {
            AboutCard()
            AppearanceCard(
                preferences = preferences,
                onDarkThemeModeChange = viewModel::setDarkThemeMode,
                onDynamicColorChange = viewModel::setDynamicColor,
            )
            AccessibilityCard(
                preferences = preferences,
                onReducedMotionOverrideChange = viewModel::setReducedMotionOverride,
                onReducedTransparencyChange = viewModel::setReducedTransparency,
                onLargeTextOverrideChange = viewModel::setLargeTextOverride,
            )
        },
    )
}
