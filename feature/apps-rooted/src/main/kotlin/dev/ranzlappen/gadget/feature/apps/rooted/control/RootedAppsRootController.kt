package dev.ranzlappen.gadget.feature.apps.rooted.control

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootControllerResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hard safety deny-list: no [freezeApp]/[unfreezeApp]/[forceStopApp] call may
 * ever touch one of these packages, regardless of the caller (manual tap or
 * an automation rule). Freezing/disabling any of these can brick the
 * launcher, settings, telephony, or the ability to fix a mistake, so this
 * list intentionally errs toward over-protecting rather than under-
 * protecting — see `PrivilegedIntentHelper.COMPONENT_DENY_FRAGMENTS`
 * (`:feature:automation-rooted`) for the sibling precedent this mirrors.
 *
 * This app's own package (both the standard `dev.ranzlappen.gadget` and the
 * rooted `dev.ranzlappen.gadget.rooted` applicationId, plus any legacy
 * `com.gadget`/`com.gadget.root` install still on the device) is added
 * dynamically in [isDenied] via [Context.getPackageName] rather than
 * hardcoded here, since the exact id depends on the running flavor.
 */
private val HARD_DENY_PACKAGES = setOf(
    // Android system server / core OS.
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.providers.settings",
    // Telephony / dialer stack — freezing these can kill emergency calling.
    "com.android.phone",
    "com.android.server.telecom",
    "com.android.incallui",
    // Package installer — freezing this makes future un-freezing/uninstall
    // impossible from the Settings UI.
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    // Common stock launchers — freezing the active launcher strands the user
    // with no home screen. Includes AOSP + the two most common OEM/Google
    // launchers; not exhaustive, but the biggest real-world risk.
    "com.android.launcher3",
    "com.google.android.apps.nexuslauncher",
    // Legacy `com.gadget` builds still installable side-by-side (see
    // Flavors-and-Root-Safety.md) — never let a rule freeze the sibling app.
    "com.gadget",
    "com.gadget.root",
)

/** Conservative Android package-name shape: letters/digits/underscore/dot. */
private val PACKAGE_NAME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

/**
 * Rooted-flavor [AppsRootController]. Wires the safety gate to three `pm`/
 * `am` shell operations:
 *  - [freezeApp] → `pm disable-user --user 0 <package>` (reversible;
 *    the app disappears from the launcher and can't run).
 *  - [unfreezeApp] → `pm enable <package>`.
 *  - [forceStopApp] → `am force-stop <package>` (kills every running
 *    process; not persistent — the app can be relaunched immediately).
 *
 * Every call is checked against [HARD_DENY_PACKAGES] (+ this app's own
 * package) *before* the [RootSafetyGate] check, so a denied package never
 * reaches the shell even if the user has disabled Safety Mode.
 */
@Singleton
class RootedAppsRootController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) : AppsRootController {

    override suspend fun freezeApp(packageName: String): AppsRootControllerResult =
        runGuarded(packageName, RootFeatureKey.AppsFreezeApp) { pkg ->
            val result = shell.exec("pm disable-user --user 0 $pkg", timeoutMillis = 10_000)
            if (result.isSuccess) {
                AppsRootControllerResult.Ok(statusNote = "$pkg disabled")
            } else {
                AppsRootControllerResult.HardwareError(
                    "pm disable-user failed: ${result.stderr.firstOrNull().orEmpty()}",
                )
            }
        }

    override suspend fun unfreezeApp(packageName: String): AppsRootControllerResult =
        runGuarded(packageName, RootFeatureKey.AppsUnfreezeApp) { pkg ->
            val result = shell.exec("pm enable $pkg", timeoutMillis = 10_000)
            if (result.isSuccess) {
                AppsRootControllerResult.Ok(statusNote = "$pkg enabled")
            } else {
                AppsRootControllerResult.HardwareError(
                    "pm enable failed: ${result.stderr.firstOrNull().orEmpty()}",
                )
            }
        }

    override suspend fun forceStopApp(packageName: String): AppsRootControllerResult =
        runGuarded(packageName, RootFeatureKey.AppsForceStopApp) { pkg ->
            val result = shell.exec("am force-stop $pkg", timeoutMillis = 10_000)
            if (result.isSuccess) {
                AppsRootControllerResult.Ok(statusNote = "$pkg force-stopped")
            } else {
                AppsRootControllerResult.HardwareError(
                    "am force-stop failed: ${result.stderr.firstOrNull().orEmpty()}",
                )
            }
        }

    /**
     * Validates [packageName]'s shape, rejects it against the deny-list
     * (own package included), then routes through [RootSafetyGate] before
     * running [block]. Denial/validation failures never reach the shell.
     */
    private suspend inline fun runGuarded(
        packageName: String,
        feature: RootFeatureKey,
        crossinline block: suspend (String) -> AppsRootControllerResult,
    ): AppsRootControllerResult {
        val pkg = packageName.trim()
        if (!PACKAGE_NAME_PATTERN.matches(pkg)) {
            return AppsRootControllerResult.Denied("\"$packageName\" is not a valid package name")
        }
        if (isDenied(pkg)) {
            return AppsRootControllerResult.Denied("$pkg is a protected system package")
        }
        return when (val gate = safetyGate.check(feature)) {
            RootGateDecision.Allowed -> block(pkg).also {
                if (it is AppsRootControllerResult.Ok) safetyGate.recordInvocation(feature)
            }
            RootGateDecision.BlockedByUser -> AppsRootControllerResult.OptedOut
            is RootGateDecision.BlockedByLimiter ->
                AppsRootControllerResult.RateLimited(gate.retryAfterMillis)
            RootGateDecision.Unsupported -> AppsRootControllerResult.Unsupported
        }
    }

    private fun isDenied(packageName: String): Boolean =
        packageName == context.packageName || packageName in HARD_DENY_PACKAGES
}
