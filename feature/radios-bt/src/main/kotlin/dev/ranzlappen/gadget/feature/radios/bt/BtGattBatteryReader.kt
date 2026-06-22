package dev.ranzlappen.gadget.feature.radios.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume

/**
 * One-shot GATT reader that fetches the Battery Level characteristic
 * (Battery Service 0x180F → Level 0x2A19) and the RSSI in a single
 * connection. Closes the GATT handle before resuming.
 *
 * Only call this for BLE or Dual-mode devices; Classic-only devices will
 * time out quickly with a GATT connection error.
 *
 * Permission note: BLUETOOTH_CONNECT is checked by the caller
 * ([BtViewModel.refresh]) before this path is reached.
 */
internal object BtGattBatteryReader {

    private val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_CHAR = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    private const val TIMEOUT_MS = 10_000L

    /** Returns (batteryPercent 0-100, rssiDbm); either may be null. */
    @SuppressLint("MissingPermission")
    suspend fun read(context: Context, device: BluetoothDevice): Pair<Int?, Int?> =
        withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                var gatt: BluetoothGatt? = null
                var rssi: Int? = null
                var characteristicReadDone = false

                val callback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                runCatching { g.close() }
                                if (!cont.isCompleted) cont.resume(Pair(null, rssi))
                            }
                        }
                    }

                    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                        // RSSI first — serializes with the characteristic read below.
                        g.readRemoteRssi()
                    }

                    override fun onReadRemoteRssi(g: BluetoothGatt, rssiValue: Int, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) rssi = rssiValue
                        val chr = g.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL_CHAR)
                        if (chr != null) {
                            @Suppress("DEPRECATION")
                            g.readCharacteristic(chr)
                        } else {
                            g.disconnect()
                        }
                    }

                    // API 33+ callback
                    override fun onCharacteristicRead(
                        g: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        value: ByteArray,
                        status: Int,
                    ) {
                        if (characteristicReadDone) return
                        characteristicReadDone = true
                        val pct = value.firstOrNull()?.toInt()?.and(0xFF)
                            ?.takeIf { status == BluetoothGatt.GATT_SUCCESS }
                        g.disconnect()
                        if (!cont.isCompleted) cont.resume(Pair(pct, rssi))
                    }

                    // Pre-API-33 fallback
                    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                    override fun onCharacteristicRead(
                        g: BluetoothGatt,
                        characteristic: BluetoothGattCharacteristic,
                        status: Int,
                    ) {
                        if (characteristicReadDone) return
                        characteristicReadDone = true
                        val pct = characteristic.value?.firstOrNull()?.toInt()?.and(0xFF)
                            ?.takeIf { status == BluetoothGatt.GATT_SUCCESS }
                        g.disconnect()
                        if (!cont.isCompleted) cont.resume(Pair(pct, rssi))
                    }
                }

                runCatching {
                    gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                }.onFailure {
                    if (!cont.isCompleted) cont.resume(Pair(null, null))
                }

                cont.invokeOnCancellation {
                    runCatching { gatt?.disconnect(); gatt?.close() }
                }
            }
        } ?: Pair(null, null)
}
