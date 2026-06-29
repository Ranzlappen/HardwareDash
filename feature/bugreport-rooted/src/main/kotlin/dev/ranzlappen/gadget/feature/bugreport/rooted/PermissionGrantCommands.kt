package dev.ranzlappen.gadget.feature.bugreport.rooted

/**
 * Pure helpers for the rooted force-grant flow — kept Android-free so the
 * command shape and the permission-name guard round-trip in a plain JVM test.
 */
object PermissionGrantCommands {

    /** Android runtime permissions are dotted, alphanumeric + underscore. */
    private val PERMISSION_PATTERN = Regex("^[A-Za-z0-9_.]+$")

    /** Reject anything that isn't a plain permission token (no shell metacharacters). */
    fun isValidPermission(permission: String): Boolean =
        permission.isNotBlank() && PERMISSION_PATTERN.matches(permission)

    /**
     * Force-grant [permission] to [packageName] via `pm grant`, bypassing the
     * system dialog. Only valid for declared dangerous (runtime) permissions.
     */
    fun grant(packageName: String, permission: String): String =
        "pm grant $packageName $permission"
}

/** Outcome of a rooted force-grant request. */
sealed interface PermissionGrantResult {
    data object Ok : PermissionGrantResult

    /** The permission token was empty or contained illegal characters. */
    data object InvalidPermission : PermissionGrantResult

    /** The user has disabled this rooted feature in safety preferences. */
    data object OptedOut : PermissionGrantResult

    /** The soft limiter rejected the call; retry after [retryAfterMillis]. */
    data class RateLimited(val retryAfterMillis: Long) : PermissionGrantResult

    /** No rooted capability on this build. */
    data object Unsupported : PermissionGrantResult

    /** `pm grant` failed (e.g. not a runtime permission, or not declared). */
    data class Error(val reason: String) : PermissionGrantResult
}
