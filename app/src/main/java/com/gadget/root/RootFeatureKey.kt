package com.gadget.root

/**
 * Stable identifier for every rooted-only capability the app may gate behind
 * [RootSafetyGate]. New entries are added when their feature module lands.
 *
 * `id` is used as the suffix of DataStore preference keys, so it must never
 * change once shipped — renaming a key would silently reset the user's choice.
 */
sealed class RootFeatureKey(val id: String) {
    data object BackupFullData : RootFeatureKey("backup_full_data")
    data object WifiInternalTools : RootFeatureKey("wifi_internal_tools")
}
