package dev.ranzlappen.gadget.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.datastore.DarkThemeMode
import dev.ranzlappen.gadget.core.datastore.TriStatePreference
import dev.ranzlappen.gadget.core.datastore.UserPreferences
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.monitoring.MonitorGlobalPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun setDarkThemeMode(mode: DarkThemeMode) {
        viewModelScope.launch { repository.setDarkThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
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

    private companion object {
        /**
         * Five-second debounce on the upstream Flow so a transient
         * configuration change (rotation, back-press onto the
         * screen, etc.) doesn't tear down + recreate the DataStore
         * subscription.
         */
        const val STATE_FLOW_TIMEOUT_MS = 5_000L
    }
}
