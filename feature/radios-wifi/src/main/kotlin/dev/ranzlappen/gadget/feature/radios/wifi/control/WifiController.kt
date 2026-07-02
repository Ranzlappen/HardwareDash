package dev.ranzlappen.gadget.feature.radios.wifi.control

/**
 * Rooted-only Wi-Fi capability surface. The standard-flavor implementation
 * always returns [WifiControllerResult.Unsupported] so shared UI uses one
 * code path for both flavors.
 *
 * Every method routes through `dev.ranzlappen.gadget.core.root.RootSafetyGate` before
 * doing anything privileged. Hard cutoffs (TX-power ceiling, channel
 * allow-list, active windows) are enforced inside the impl and cannot
 * be extended by callers.
 */
interface WifiController {

    /**
     * Toggles `rfkill` for the Wi-Fi radio. Hard 60-second active window;
     * limiter window 60 s / 3 invocations.
     */
    suspend fun rfkillToggle(config: RfkillConfig): WifiControllerResult

    /**
     * Override the OEM TX-power cap. Hard 100 mW (20 dBm) ceiling
     * inside the helper. Snapshot+restore via the shared mutation log.
     * `requiresExplicitConfirm = true` on the descriptor.
     */
    suspend fun txPowerOverride(config: TxPowerConfig): WifiControllerResult

    /**
     * Override the active Wi-Fi channel. Restricted to 1–14 (2.4 GHz)
     * + standard 5 GHz UNII bands inside the helper.
     */
    suspend fun channelOverride(config: ChannelConfig): WifiControllerResult

    /**
     * **Read-only capability probe.** Inspects `iw phy <phy> info` and
     * reports whether the driver advertises monitor / IBSS modes. Does
     * NOT enable injection — actual packet injection requires a custom
     * kernel module (e.g. nexmon) which this app does not ship.
     */
    suspend fun probeInjectionCapability(): WifiControllerResult

    /**
     * Reverts every Wi-Fi-surface mutation registered with the shared
     * `SysfsMutationLog`. Standard-flavor returns `ResetCompleted(0, 0)`.
     */
    suspend fun resetAllWifiMutations(): WifiControllerResult

    /**
     * Auto-revert path called on `RadiosScreen` dispose. Filters the
     * mutation log by TX-power-only path prefixes — narrower than
     * [resetAllWifiMutations] so a screen dispose doesn't accidentally
     * clear a user-applied channel override that they want to keep.
     */
    suspend fun revertTxPowerOnly(): WifiControllerResult
}
