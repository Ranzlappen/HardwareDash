package com.gadget.root

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed per-feature opt-in toggles. The default for every feature
 * is OFF — `RootFeatureDescriptor.defaultOn` is consulted via
 * [RootFeatureRegistry] when the prefs entry hasn't been written yet.
 */
@Singleton
class RootedRootFeatureToggles @Inject constructor(
    @RootSafetyPrefs private val dataStore: DataStore<Preferences>,
    private val featureRegistry: RootFeatureRegistry,
) : RootFeatureToggles {

    override fun isEnabled(feature: RootFeatureKey): Flow<Boolean> {
        val descriptor = featureRegistry.descriptor(feature)
        return dataStore.data.map { prefs ->
            prefs[RootPrefKeys.featureEnabledKey(feature)] ?: descriptor.defaultOn
        }
    }

    override suspend fun setEnabled(feature: RootFeatureKey, enabled: Boolean) {
        dataStore.edit { mutable ->
            mutable[RootPrefKeys.featureEnabledKey(feature)] = enabled
        }
    }

    override fun isMonitorSafetyMode(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[RootPrefKeys.MonitorSafetyMode] ?: true }

    override suspend fun setMonitorSafetyMode(enabled: Boolean) {
        dataStore.edit { mutable ->
            mutable[RootPrefKeys.MonitorSafetyMode] = enabled
        }
    }

    override suspend fun resetAllToDefault(): Int {
        val featureKeys = featureRegistry.allDescriptors().map {
            RootPrefKeys.featureEnabledKey(it.key)
        }
        val before = dataStore.data.first()
        var cleared = 0
        dataStore.edit { mutable ->
            for (key in featureKeys) {
                if (key in before) {
                    mutable.remove(key)
                    cleared++
                }
            }
        }
        return cleared
    }

    override fun isRootedAcknowledged(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[RootPrefKeys.RootedAcknowledged] ?: false }

    override suspend fun setRootedAcknowledged(acknowledged: Boolean) {
        dataStore.edit { mutable ->
            mutable[RootPrefKeys.RootedAcknowledged] = acknowledged
        }
    }
}
