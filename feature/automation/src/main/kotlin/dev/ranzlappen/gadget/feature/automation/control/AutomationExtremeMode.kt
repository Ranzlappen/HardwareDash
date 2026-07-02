package dev.ranzlappen.gadget.feature.automation.control


/**
 * Configures a privileged `am broadcast` / `am start` / `am start-service`
 * shell-out. The impl enforces an action *deny-list* (REBOOT, SHUTDOWN,
 * FACTORY_RESET, MASTER_CLEAR, system-ui recents/screenshot) regardless
 * of caller input.
 */
data class PrivilegedIntentConfig(
    val verb: PrivilegedIntentVerb,
    val action: String,
    val componentFlatten: String? = null,
    val extraStringPairs: List<Pair<String, String>> = emptyList(),
)

enum class PrivilegedIntentVerb {
    BROADCAST,
    START_ACTIVITY,
    START_SERVICE,
}

/**
 * Configures a `settings put <scope> <key> <value>` write. Scope is
 * `system` / `secure` / `global`. Hard allow-list of writable keys
 * enforced inside the helper.
 */
data class SystemSettingsOverrideConfig(
    val scope: SystemSettingsScope,
    val key: String,
    val value: String,
)

enum class SystemSettingsScope {
    SYSTEM,
    SECURE,
    GLOBAL,
}
