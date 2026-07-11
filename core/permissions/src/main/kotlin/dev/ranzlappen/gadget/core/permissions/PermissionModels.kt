package dev.ranzlappen.gadget.core.permissions

/**
 * One Android runtime permission a feature relies on, with the SDK window
 * it applies to (so an SDK-33-only permission like `POST_NOTIFICATIONS`
 * isn't scanned on older devices). This is the centralized replacement for
 * the per-feature ad-hoc permission lists (the `:feature:bugreport` scan and
 * a dozen others), aggregated by [PermissionRegistry].
 */
data class RuntimePermission(
    val permission: String,
    val label: String,
    val rationale: String,
    val minSdk: Int = 1,
    val maxSdk: Int = Int.MAX_VALUE,
    val optional: Boolean = false,
)

/**
 * A "special" permission — one that isn't granted through the normal runtime
 * dialog but via a dedicated Settings screen (overlay, exact alarm,
 * WRITE_SETTINGS, notification-listener access, all-files access). Each maps
 * to a live grant-state query + a Settings deep-link (see
 * [SpecialPermissions]).
 */
enum class SpecialPermission {
    Overlay,
    ExactAlarm,
    WriteSettings,
    NotificationListener,
    AllFilesAccess,
}

/**
 * A feature's declared permission needs, contributed to the registry via
 * `@IntoMap @StringKey(featureId)`. A feature binds one of these to make its
 * permissions visible in the centralized dashboard without the dashboard
 * importing the feature.
 */
data class FeaturePermissions(
    val featureId: String,
    val displayName: String,
    val runtime: List<RuntimePermission> = emptyList(),
    val special: List<SpecialPermission> = emptyList(),
)
