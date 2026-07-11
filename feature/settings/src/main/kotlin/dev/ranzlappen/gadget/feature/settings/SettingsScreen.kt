package dev.ranzlappen.gadget.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.feature.settings.components.AboutCard
import dev.ranzlappen.gadget.feature.settings.components.AccessibilityCard
import dev.ranzlappen.gadget.feature.settings.components.AppearanceCard
import dev.ranzlappen.gadget.feature.settings.components.LanguageCard
import dev.ranzlappen.gadget.feature.settings.components.MonitoringCard

/**
 * Settings v1 — cards under one [ModuleScreenScaffold]:
 *
 *   1. **About** — version / build info from `BuildConfig`.
 *   2. **Language** — per-app UI language (W8), backed by
 *      `AppCompatDelegate.setApplicationLocales()`.
 *   3. **Appearance** — dark-theme mode + dynamic colour.
 *   4. **Accessibility** — reduced motion override, reduce
 *      transparency, large text override.
 *   5. **Rooted feature toggles** ([rootFeatureToggles] slot) — the
 *      per-feature opt-in switches + safety-mode master switch. Supplied
 *      by `:app` (it depends on the legacy `RootFeaturesEntryPoint` +
 *      22 controllers, which a leaf feature module can't see) and renders
 *      nothing on the standard flavor / when root isn't granted. Defaults
 *      to an empty slot so the screen + its previews stay Hilt-free.
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
    permissionsSection: @Composable () -> Unit = {},
    backupSection: @Composable () -> Unit = {},
    rootFeatureToggles: @Composable () -> Unit = {},
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val monitorNotificationActionsEnabled by viewModel.monitorNotificationActionsEnabled.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    ModuleScreenScaffold(
        title = "Settings",
        modifier = modifier,
        functional = {
            AboutCard()
            LanguageCard(
                current = language,
                onLanguageChange = viewModel::setLanguage,
            )
            AppearanceCard(
                preferences = preferences,
                onDarkThemeModeChange = viewModel::setDarkThemeMode,
                onDynamicColorChange = viewModel::setDynamicColor,
                onCustomThemeChange = viewModel::setCustomTheme,
            )
            AccessibilityCard(
                preferences = preferences,
                onReducedMotionOverrideChange = viewModel::setReducedMotionOverride,
                onReducedTransparencyChange = viewModel::setReducedTransparency,
                onLargeTextOverrideChange = viewModel::setLargeTextOverride,
                onFloatingTorchButtonEnabledChange = viewModel::setFloatingTorchButtonEnabled,
            )
            MonitoringCard(
                notificationActionsEnabled = monitorNotificationActionsEnabled,
                onNotificationActionsEnabledChange = viewModel::setMonitorNotificationActionsEnabled,
            )
            // Centralized permissions dashboard (W5). Empty default; supplied
            // by :app from :core:permissions (PermissionsDashboardCard), which
            // aggregates every feature's @IntoMap permission contributions.
            permissionsSection()
            // Whole-app backup / restore. Empty default; supplied by :app
            // (BackupManager spans :core:data + :feature:apps, which a leaf
            // module can't see together).
            backupSection()
            // Rooted-only — empty on standard / no-root (the slot's own
            // composable returns early). Supplied by :app.
            rootFeatureToggles()
        },
    )
}
