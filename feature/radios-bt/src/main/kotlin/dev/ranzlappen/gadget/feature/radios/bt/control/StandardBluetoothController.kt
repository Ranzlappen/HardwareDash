package dev.ranzlappen.gadget.feature.radios.bt.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Bluetooth controller. Every extreme-tier method
 * returns [BluetoothControllerResult.Unsupported].
 */
@Singleton
class StandardBluetoothController @Inject constructor() : BluetoothController {

    override suspend fun rfkillToggle(config: BluetoothRfkillConfig): BluetoothControllerResult =
        BluetoothControllerResult.Unsupported

    override suspend fun txPowerOverride(config: BluetoothTxPowerConfig): BluetoothControllerResult =
        BluetoothControllerResult.Unsupported

    override suspend fun hciSnoopDump(): BluetoothControllerResult =
        BluetoothControllerResult.Unsupported

    override suspend fun resetAllBluetoothMutations(): BluetoothControllerResult =
        BluetoothControllerResult.ResetCompleted(restored = 0, failed = 0)

    override suspend fun revertTxPowerOnly(): BluetoothControllerResult =
        BluetoothControllerResult.ResetCompleted(restored = 0, failed = 0)
}
