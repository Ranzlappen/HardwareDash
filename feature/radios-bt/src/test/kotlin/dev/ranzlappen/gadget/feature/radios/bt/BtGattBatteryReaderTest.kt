package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [BtGattBatteryReader]. There is no Robolectric shadow in
 * this repo, so a real `BluetoothGatt` connection can't be driven — instead
 * each test stubs [BluetoothDevice.connectGatt] to capture the
 * [BluetoothGattCallback] the reader registers and invoke its methods
 * synchronously, standing in for the async callbacks the real BLE stack
 * would deliver. Because [BtGattBatteryReader.read] resumes its
 * `suspendCancellableCoroutine` from inside that synchronous script (before
 * the coroutine ever actually suspends), the whole read completes
 * synchronously and a plain `runBlocking` is enough — no timeout/dispatcher
 * machinery needed.
 */
class BtGattBatteryReaderTest {

    private val batteryService: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val batteryLevelChar: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private val context = mockk<Context>(relaxed = true)
    private val gatt = mockk<BluetoothGatt>(relaxed = true)

    private fun mockDevice(connect: (BluetoothGattCallback) -> Unit): BluetoothDevice {
        val device = mockk<BluetoothDevice>()
        val callbackSlot = slot<BluetoothGattCallback>()
        every {
            device.connectGatt(any(), false, capture(callbackSlot), BluetoothDevice.TRANSPORT_LE)
        } answers {
            connect(callbackSlot.captured)
            gatt
        }
        return device
    }

    @Test
    fun `reads battery percent and rssi on a successful round trip`() = runBlocking {
        val characteristic = mockk<BluetoothGattCharacteristic>()
        val service = mockk<BluetoothGattService>()
        every { gatt.getService(batteryService) } returns service
        every { service.getCharacteristic(batteryLevelChar) } returns characteristic

        val device = mockDevice { cb ->
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
            cb.onReadRemoteRssi(gatt, -55, BluetoothGatt.GATT_SUCCESS)
            cb.onCharacteristicRead(gatt, characteristic, byteArrayOf(85), BluetoothGatt.GATT_SUCCESS)
        }

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertEquals(85, percent)
        assertEquals(-55, rssi)
    }

    @Test
    fun `a failed characteristic-read status yields a null percent but keeps the rssi`() = runBlocking {
        val characteristic = mockk<BluetoothGattCharacteristic>()
        val service = mockk<BluetoothGattService>()
        every { gatt.getService(batteryService) } returns service
        every { service.getCharacteristic(batteryLevelChar) } returns characteristic

        val device = mockDevice { cb ->
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
            cb.onReadRemoteRssi(gatt, -60, BluetoothGatt.GATT_SUCCESS)
            cb.onCharacteristicRead(gatt, characteristic, byteArrayOf(85), BluetoothGatt.GATT_FAILURE)
        }

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertNull(percent)
        assertEquals(-60, rssi)
    }

    @Test
    fun `no battery characteristic disconnects and resolves with a null percent`() = runBlocking {
        every { gatt.getService(batteryService) } returns null

        val device = mockDevice { cb ->
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
            cb.onReadRemoteRssi(gatt, -70, BluetoothGatt.GATT_SUCCESS)
            // The reader calls g.disconnect() when the characteristic is missing;
            // simulate the stack's resulting disconnect callback.
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
        }

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertNull(percent)
        assertEquals(-70, rssi)
    }

    @Test
    fun `a failed rssi read leaves rssi null but still reads the battery characteristic`() = runBlocking {
        val characteristic = mockk<BluetoothGattCharacteristic>()
        val service = mockk<BluetoothGattService>()
        every { gatt.getService(batteryService) } returns service
        every { service.getCharacteristic(batteryLevelChar) } returns characteristic

        val device = mockDevice { cb ->
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
            cb.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)
            cb.onReadRemoteRssi(gatt, -999, BluetoothGatt.GATT_FAILURE)
            cb.onCharacteristicRead(gatt, characteristic, byteArrayOf(42), BluetoothGatt.GATT_SUCCESS)
        }

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertEquals(42, percent)
        assertNull(rssi)
    }

    @Test
    fun `connectGatt throwing resolves with a null pair instead of propagating`() = runBlocking {
        val device = mockk<BluetoothDevice>()
        every {
            device.connectGatt(any(), false, any(), BluetoothDevice.TRANSPORT_LE)
        } throws SecurityException("no BLUETOOTH_CONNECT permission")

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertNull(percent)
        assertNull(rssi)
    }

    @Test
    fun `a plain disconnect with no prior rssi resolves both values null`() = runBlocking {
        val device = mockDevice { cb ->
            cb.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
        }

        val (percent, rssi) = BtGattBatteryReader.read(context, device)

        assertNull(percent)
        assertNull(rssi)
    }
}
