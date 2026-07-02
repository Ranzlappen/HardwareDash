package dev.ranzlappen.gadget.feature.gps.spoof

import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level controller for GPS spoofing. Standard flavor returns
 * [SpoofResult.Unsupported] for the rooted-only methods; rooted flavor
 * implements them through libsu + the bundled LSPosed module.
 *
 * Both flavors implement [start]/[stop]: the standard flavor uses
 * `LocationManager.addTestProvider` gated on the user enabling HardwareDash
 * as the Mock Location App in Developer Options. The rooted flavor toggles
 * the AppOp via libsu so no Dev Options dance is required.
 */
interface GpsSpoofController {

    val state: StateFlow<SpoofState>

    suspend fun capabilities(): SpoofCapabilities

    /**
     * Starts emitting per [config]. If a previous session is still running
     * the implementation MUST stop it first (idempotent recovery from
     * crashed sessions also includes calling `removeTestProvider` for any
     * stale test providers).
     */
    suspend fun start(config: SpoofConfig): SpoofResult

    suspend fun stop(): SpoofResult

    /**
     * Rooted-only: copy the bundled LSPosed module APK to /data/local/tmp,
     * `pm install`, then surface a deep-link instruction to enable the
     * module in LSPosed Manager and reboot. Standard flavor returns
     * [SpoofResult.Unsupported].
     */
    suspend fun installLsposedModule(): SpoofResult

    suspend fun uninstallLsposedModule(): SpoofResult

    /** True if the user has acknowledged the legal disclaimer at the current version. */
    suspend fun isLegalAcknowledged(): Boolean

    suspend fun acknowledgeLegal()

    companion object {
        /**
         * Default cap on a single playback session before auto-stop. Prevents
         * a forgotten spoof draining the battery overnight.
         */
        const val DEFAULT_SESSION_LIMIT_MS: Long = 4L * 60L * 60L * 1000L
    }
}
