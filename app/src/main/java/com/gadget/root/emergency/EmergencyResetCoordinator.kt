package com.gadget.root.emergency

/**
 * Global one-shot reset of every privileged mutation the app has
 * tracked across all rooted surfaces. This is the safety net referenced
 * throughout the rooted-features split:
 *
 * 1. `SysfsMutationLog.revertAll(emptyList())` — reverts every tracked
 *    sysfs / setprop / cmd-* / adb-toggle / cmd-usb mutation regardless
 *    of namespace.
 * 2. `KeepAliveController.disableAndStopService()` — stops the
 *    persistent foreground service.
 * 3. `cmd appops set <own pkg> RUN_ANY_IN_BACKGROUND default` to revert
 *    the IGNORE_BATTERY_OPTIMIZATIONS-equivalent app-op (the
 *    deviceidle whitelist removal is already covered by step 1 via the
 *    `cmd-deviceidle://` namespace).
 * 4. (Optional, off by default) `RootFeatureToggles.resetAllToDefault()`
 *    clears every per-feature DataStore opt-in back to the registry's
 *    `defaultOn`. Off by default so the user's careful curation is
 *    preserved unless they explicitly request a clean slate.
 *
 * **NOT routed through `RootSafetyGate`** — the Emergency Reset is the
 * safety net itself; gating it behind a per-feature opt-out toggle
 * would be a footgun. The double-confirmation `AlertDialog` in the UI
 * is the only safety wrapper.
 *
 * **NOT rate-limited** — if the user presses Confirm twice, they mean
 * it.
 *
 * Standard flavor returns `Ok` with all zero counts so the same code
 * path compiles in both flavors.
 */
interface EmergencyResetCoordinator {
    suspend fun resetEverything(options: EmergencyResetOptions): EmergencyResetCoordinatorResult
}

/**
 * Per-step toggles for the emergency reset. Defaults preserve the most
 * conservative behaviour: revert mutations + stop service + reset
 * battery-optimization, but DO NOT touch per-feature opt-outs unless
 * the user explicitly checks that box.
 */
data class EmergencyResetOptions(
    val revertAllSysfsMutations: Boolean = true,
    val stopKeepAliveService: Boolean = true,
    val reEnableDozeAndBatteryOptimization: Boolean = true,
    val resetAllPerFeatureOptOuts: Boolean = false,
)
