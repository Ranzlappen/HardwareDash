package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandardBtEnhancedInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : BtEnhancedInfoProvider {

    private val bluetoothManager get() =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override val isRootedFlavor = false

    override fun connectedAddresses(): Set<String> {
        val mgr = bluetoothManager ?: return emptySet()
        val profiles = listOf(BluetoothProfile.GATT, BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
        return profiles.flatMap { profile ->
            runCatching {
                @Suppress("MissingPermission")
                mgr.getConnectedDevices(profile) ?: emptyList()
            }.getOrDefault(emptyList())
        }.map { it.address }.toSet()
    }

    override suspend fun readGattBatteryAndRssi(device: BluetoothDevice): Pair<Int?, Int?> =
        BtGattBatteryReader.read(context, device)

    override fun hiddenBatteryLevel(device: BluetoothDevice): Int? = null

    override fun a2dpCodecName(device: BluetoothDevice): String? = null
}
