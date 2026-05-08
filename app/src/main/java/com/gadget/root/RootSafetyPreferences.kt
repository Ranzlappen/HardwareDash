package com.gadget.root

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * Centralized DataStore key declarations for the rooted-features safety
 * framework. Concrete read/write helpers will be added when the rooted flavor
 * actually binds a real [RootSafetyGate]; this batch only nails down the
 * contract so future migrations don't churn key strings.
 *
 * Stable key strings — DO NOT rename, doing so would silently reset the user's
 * preference.
 */
object RootPrefKeys {
    val MasterEnabled = booleanPreferencesKey("root_master_enabled")
    val MonitorSafetyMode = booleanPreferencesKey("root_monitor_safety_mode")
    val MasterSafetyInitialized = booleanPreferencesKey("root_master_safety_initialized")
    val RootedAcknowledged = booleanPreferencesKey("root_rooted_acknowledged")

    fun featureEnabledKey(feature: RootFeatureKey) =
        booleanPreferencesKey("root_feat_enabled_${feature.id}")

    fun featureWindowStartKey(feature: RootFeatureKey) =
        longPreferencesKey("root_feat_window_start_${feature.id}")

    fun featureInvocationCountKey(feature: RootFeatureKey) =
        longPreferencesKey("root_feat_invocations_${feature.id}")
}
