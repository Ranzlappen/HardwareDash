package dev.ranzlappen.gadget.feature.adbdebug.control


/**
 * Rooted-only ADB Debugging surface. Standard flavor returns
 * [AdbDebuggingControllerResult.Unsupported] for every method.
 *
 * Privileged paths: `settings put global adb_enabled 0|1` (own helper, NOT
 * reusing the Batch-7 SystemSettingsHelper allow-list);
 * `setprop service.adb.tcp.port <port>` + `setprop ctl.restart adbd`;
 * `getprop` snapshot (read-only, tail-capped); allow-listed `setprop`
 * override across an 8-entry exact-match list plus a `log.tag.*` wildcard.
 *
 * Anything starting with `ro.` is refused regardless of caller — those
 * are read-only at the kernel level anyway and listing them in the
 * allow-list would invite confusion.
 */
interface AdbDebuggingController {

    /**
     * Toggles `Settings.Global.ADB_ENABLED` (1 = on, 0 = off). Snapshot+restore
     * via `adb-toggle://global/adb_enabled` so screen-exit revert can put
     * the toggle back if the user navigates away while it's flipped.
     */
    suspend fun toggleAdbEnabled(enabled: Boolean): AdbDebuggingControllerResult

    /**
     * Enables ADB-over-network on the supplied port. Port must be in the
     * 5555–5599 range; the helper rejects anything outside.
     * Snapshot+restore the prior `service.adb.tcp.port` value via
     * `setprop://service.adb.tcp.port`. Restart cycle uses
     * `setprop ctl.restart adbd` (canonical form on init.rc-based services
     * through API 35).
     */
    suspend fun toggleAdbOverNetwork(config: AdbNetworkConfig): AdbDebuggingControllerResult

    /**
     * Read-only `getprop` capture, tail-capped to 16 KB. Optionally persists
     * a JSON copy to the Logbook directory using the same convention as
     * [dev.ranzlappen.gadget.feature.battery.rooted.control.BatteryDumpWriter].
     */
    suspend fun dumpProperties(persist: Boolean = false): AdbDebuggingControllerResult

    /**
     * Allow-listed `setprop` override. The helper enforces the allow-list
     * regardless of caller and snapshots the prior value before writing.
     * Snapshot+restore via `setprop://<key>`.
     */
    suspend fun overrideSystemProperty(config: SetPropConfig): AdbDebuggingControllerResult

    /** Reverts every ADB-surface mutation registered with the log. */
    suspend fun resetAllAdbMutations(): AdbDebuggingControllerResult

    /**
     * Auto-revert path called on screen dispose. Filters by
     * `adb-toggle://` + `setprop://` prefixes.
     */
    suspend fun revertOnScreenExit(): AdbDebuggingControllerResult
}
