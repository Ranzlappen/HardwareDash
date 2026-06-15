package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAdapterWrapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = manager?.adapter

    fun isAvailable(): Boolean = adapter != null

    fun isEnabled(): Boolean = try {
        adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    fun name(): String? = try {
        adapter?.name
    } catch (_: SecurityException) {
        null
    }

    fun bondedDevices(): List<BluetoothDeviceInfo> = try {
        adapter?.bondedDevices?.map { device ->
            BluetoothDeviceInfo(
                name = try { device.name } catch (_: SecurityException) { null },
                address = device.address,
                typeName = when (device.type) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                    else -> "Unknown"
                },
            )
        } ?: emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }
}
