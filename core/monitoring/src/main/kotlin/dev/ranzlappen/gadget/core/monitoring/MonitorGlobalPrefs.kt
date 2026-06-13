package dev.ranzlappen.gadget.core.monitoring

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MonitorGlobalDataStore

/**
 * App-wide monitoring preferences that apply across all metrics.
 * Backed by its own DataStore file (`"monitor_global"`).
 */
@Singleton
class MonitorGlobalPrefs @Inject constructor(
    @MonitorGlobalDataStore private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Whether per-metric notifications should include a "Stop monitoring" action button. */
    val notificationActionsEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[KEY_NOTIFICATION_ACTIONS] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)

    fun setNotificationActionsEnabled(value: Boolean) {
        scope.launch { dataStore.edit { it[KEY_NOTIFICATION_ACTIONS] = value } }
    }

    private companion object {
        val KEY_NOTIFICATION_ACTIONS = booleanPreferencesKey("notification_actions_enabled")
    }
}
