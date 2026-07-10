package dev.ranzlappen.gadget.feature.apps.root

/**
 * Rooted-only per-app management surface for App-Organizer: pm-based
 * freeze / unfreeze and `am force-stop`. Standard flavor returns
 * [AppsRootControllerResult.Unsupported] for every method — there is no
 * privileged shell in that APK, so package-manager mutation and
 * force-stop are physically impossible.
 *
 * Mirrors the `feature/storage` split: the contract (+ result type) and
 * the standard no-op live here in the base `:feature:apps` module so both
 * flavors share one binding surface; the privileged implementation lives
 * in the sibling `:feature:apps-rooted` module and is wired in only via
 * `rootedImplementation` (see `app/src/rooted/.../RootBindings.kt`).
 *
 * Every method takes a raw `packageName`. Implementations MUST reject a
 * hard deny-list of system-critical packages (this app's own package,
 * `android`, `com.android.systemui`, `com.android.settings`, the phone/
 * dialer stack, the package installer, and the device's stock launcher)
 * before touching `pm`/`am` — see `RootedAppsRootController`'s deny-list
 * for the exact set and rationale.
 */
interface AppsRootController {

    /**
     * Freezes [packageName] via `pm disable-user --user 0 <packageName>`.
     * The app disappears from the launcher and can't run until
     * [unfreezeApp] re-enables it.
     */
    suspend fun freezeApp(packageName: String): AppsRootControllerResult

    /** Re-enables a previously frozen [packageName] via `pm enable`. */
    suspend fun unfreezeApp(packageName: String): AppsRootControllerResult

    /** Kills every running process of [packageName] via `am force-stop`. */
    suspend fun forceStopApp(packageName: String): AppsRootControllerResult
}

/** Outcome of an [AppsRootController] call. */
sealed class AppsRootControllerResult {
    /** The `pm`/`am` command completed successfully. */
    data class Ok(val statusNote: String? = null) : AppsRootControllerResult()

    /** No privileged shell available in this build (standard flavor). */
    data object Unsupported : AppsRootControllerResult()

    /** The user has turned this rooted feature off in Settings. */
    data object OptedOut : AppsRootControllerResult()

    /** [packageName] is on the hard safety deny-list — request refused. */
    data class Denied(val message: String) : AppsRootControllerResult()

    /** The soft rate-limit window was exceeded. */
    data class RateLimited(val retryAfterMillis: Long) : AppsRootControllerResult()

    /** The underlying shell command failed. */
    data class HardwareError(val message: String) : AppsRootControllerResult()
}
