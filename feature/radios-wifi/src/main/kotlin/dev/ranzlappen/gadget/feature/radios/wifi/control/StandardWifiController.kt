package dev.ranzlappen.gadget.feature.radios.wifi.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Wi-Fi controller. Every extreme-tier method returns
 * [WifiControllerResult.Unsupported] — the standard APK has no privileged
 * shell so direct rfkill / iw / sysfs writes are impossible regardless
 * of permissions.
 */
@Singleton
class StandardWifiController @Inject constructor() : WifiController {

    override suspend fun rfkillToggle(config: RfkillConfig): WifiControllerResult =
        WifiControllerResult.Unsupported

    override suspend fun txPowerOverride(config: TxPowerConfig): WifiControllerResult =
        WifiControllerResult.Unsupported

    override suspend fun channelOverride(config: ChannelConfig): WifiControllerResult =
        WifiControllerResult.Unsupported

    override suspend fun probeInjectionCapability(): WifiControllerResult =
        WifiControllerResult.Unsupported

    override suspend fun resetAllWifiMutations(): WifiControllerResult =
        WifiControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertTxPowerOnly(): WifiControllerResult =
        WifiControllerResult.ResetCompleted(restored = 0, failed = 0)
}
