package dev.ranzlappen.gadget.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.datastore.CustomThemeOption
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorGlobalPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel backing the Settings screen.
 *
 * Passthrough over [UserPreferencesRepository] — exposes the
 * preferences as a [StateFlow] for Compose collection and a
 * per-field setter that launches into [viewModelScope]. The actual
 * persistence (DataStore I/O, error handling, key mapping) is
 * delegated to the repository.
 *
 * Settings UI cards consume the flow via
 * `viewModel.preferences.collectAsStateWithLifecycle()` so they recompose on
 * any pref change — including changes triggered by the QS tile or
 * a widget elsewhere.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UserPreferencesRepository,
    private val monitorGlobalPrefs: MonitorGlobalPrefs,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = repository.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
            initialValue = UserPreferences(),
        )

    val monitorNotificationActionsEnabled: StateFlow<Boolean> =
        monitorGlobalPrefs.notificationActionsEnabled

    // AppCompatDelegate self-persists the per-app locale (SharedPreferences
    // pre-API-33, the native LocaleManager on 33+) — no UserPreferences /
    // DataStore field needed. Seeded once at construction; setLanguage()
    // updates it locally too since AppCompatDelegate's own change callback
    // is activity-lifecycle-shaped, not a Flow, and a language switch
    // recreates the process/activity anyway on most API levels.
    private val _language = MutableStateFlow(
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotEmpty() }),
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _language.value = language
        val locales = language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { repository.setDarkThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setCustomTheme(option: CustomThemeOption) {
        viewModelScope.launch { repository.setCustomTheme(option) }
    }

    fun setReducedMotionOverride(value: TriStatePreference) {
        viewModelScope.launch { repository.setReducedMotionOverride(value) }
    }

    fun setReducedTransparency(enabled: Boolean) {
        viewModelScope.launch { repository.setReducedTransparency(enabled) }
    }

    fun setLargeTextOverride(enabled: Boolean) {
        viewModelScope.launch { repository.setLargeTextOverride(enabled) }
    }

    fun setMonitorNotificationActionsEnabled(enabled: Boolean) {
        monitorGlobalPrefs.setNotificationActionsEnabled(enabled)
    }

    /**
     * Persists [enabled] and starts / stops `TorchOverlayService` accordingly.
     *
     * Uses a string-based implicit intent with [setPackage] to start the service
     * without a compile-time dependency on `:feature:torch`. The caller is
     * responsible for verifying `Settings.canDrawOverlays` before enabling.
     */
    fun setFloatingTorchButtonEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setFloatingTorchButtonEnabled(enabled) }
        val action = if (enabled) OVERLAY_ACTION_START else OVERLAY_ACTION_STOP
        val intent = Intent(action).setPackage(context.packageName)
        try {
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
            // Service may not be installed (wrong flavor / first launch).
            // The preference is already persisted; TorchOverlayService picks
            // it up on next start via its pref-observation coroutine.
        }
    }

    private companion object {
        /**
         * Five-second debounce on the upstream Flow so a transient
         * configuration change (rotation, back-press onto the
         * screen, etc.) doesn't tear down + recreate the DataStore
         * subscription.
         */
        const val STATE_FLOW_TIMEOUT_MS = 5_000L

        // String-based service control — avoids a compile-time dep on :feature:torch.
        const val OVERLAY_ACTION_START = "dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_START"
        const val OVERLAY_ACTION_STOP = "dev.ranzlappen.gadget.feature.torch.ACTION_OVERLAY_STOP"
    }
}
