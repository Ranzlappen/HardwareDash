package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [BluetoothAdapterWrapper] — not a thin passthrough: every
 * accessor swallows a [SecurityException] into a safe default (the caller
 * may be missing `BLUETOOTH_CONNECT` on API 31+), and [BluetoothAdapterWrapper.bondedDevices]
 * maps the platform's `BluetoothDevice.type` int into the feature's
 * `typeName` label. Both are genuine standard-tier logic worth pinning.
 */
class BluetoothAdapterWrapperTest {

    private fun wrapper(adapter: BluetoothAdapter?): BluetoothAdapterWrapper {
        val manager = mockk<BluetoothManager>()
        every { manager.adapter } returns adapter
        val context = mockk<Context>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        return BluetoothAdapterWrapper(context)
    }

    private fun wrapperWithNoBluetoothService(): BluetoothAdapterWrapper {
        val context = mockk<Context>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns null
        return BluetoothAdapterWrapper(context)
    }

    // ---- isAvailable ----

    @Test
    fun `isAvailable is true when the adapter resolves`() {
        assertTrue(wrapper(mockk(relaxed = true)).isAvailable())
    }

    @Test
    fun `isAvailable is false when the manager has no adapter`() {
        assertFalse(wrapper(null).isAvailable())
    }

    @Test
    fun `isAvailable is false when the system has no BLUETOOTH_SERVICE`() {
        assertFalse(wrapperWithNoBluetoothService().isAvailable())
    }

    // ---- isEnabled ----

    @Test
    fun `isEnabled reflects the adapter state`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.isEnabled } returns true
        assertTrue(wrapper(adapter).isEnabled())
    }

    @Test
    fun `isEnabled is false when disabled`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.isEnabled } returns false
        assertFalse(wrapper(adapter).isEnabled())
    }

    @Test
    fun `isEnabled swallows a SecurityException as false`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.isEnabled } throws SecurityException("missing BLUETOOTH_CONNECT")
        assertFalse(wrapper(adapter).isEnabled())
    }

    @Test
    fun `isEnabled is false when there is no adapter at all`() {
        assertFalse(wrapper(null).isEnabled())
    }

    // ---- name ----

    @Test
    fun `name returns the adapter's name`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.name } returns "Pixel"
        assertEquals("Pixel", wrapper(adapter).name())
    }

    @Test
    fun `name swallows a SecurityException as null`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.name } throws SecurityException("missing BLUETOOTH_CONNECT")
        assertNull(wrapper(adapter).name())
    }

    // ---- bondedDevices ----

    @Test
    fun `bondedDevices maps the platform type to the feature's typeName`() {
        val classic = mockk<BluetoothDevice>()
        every { classic.name } returns "Classic Speaker"
        every { classic.address } returns "AA:AA:AA:AA:AA:AA"
        every { classic.type } returns BluetoothDevice.DEVICE_TYPE_CLASSIC

        val ble = mockk<BluetoothDevice>()
        every { ble.name } returns "BLE Tracker"
        every { ble.address } returns "BB:BB:BB:BB:BB:BB"
        every { ble.type } returns BluetoothDevice.DEVICE_TYPE_LE

        val dual = mockk<BluetoothDevice>()
        every { dual.name } returns "Dual Buds"
        every { dual.address } returns "CC:CC:CC:CC:CC:CC"
        every { dual.type } returns BluetoothDevice.DEVICE_TYPE_DUAL

        val unknown = mockk<BluetoothDevice>()
        every { unknown.name } returns "Mystery"
        every { unknown.address } returns "DD:DD:DD:DD:DD:DD"
        every { unknown.type } returns BluetoothDevice.DEVICE_TYPE_UNKNOWN

        val adapter = mockk<BluetoothAdapter>()
        every { adapter.bondedDevices } returns setOf(classic, ble, dual, unknown)

        val devices = wrapper(adapter).bondedDevices().associateBy { it.address }

        assertEquals("Classic", devices.getValue("AA:AA:AA:AA:AA:AA").typeName)
        assertEquals("BLE", devices.getValue("BB:BB:BB:BB:BB:BB").typeName)
        assertEquals("Dual", devices.getValue("CC:CC:CC:CC:CC:CC").typeName)
        assertEquals("Unknown", devices.getValue("DD:DD:DD:DD:DD:DD").typeName)
    }

    @Test
    fun `bondedDevices reports a null name when reading it throws`() {
        val device = mockk<BluetoothDevice>()
        every { device.name } throws SecurityException("missing BLUETOOTH_CONNECT")
        every { device.address } returns "AA:AA:AA:AA:AA:AA"
        every { device.type } returns BluetoothDevice.DEVICE_TYPE_CLASSIC

        val adapter = mockk<BluetoothAdapter>()
        every { adapter.bondedDevices } returns setOf(device)

        val result = wrapper(adapter).bondedDevices()

        assertEquals(1, result.size)
        assertNull(result.single().name)
        assertEquals("AA:AA:AA:AA:AA:AA", result.single().address)
    }

    @Test
    fun `bondedDevices is empty when there is no adapter`() {
        assertEquals(emptyList(), wrapper(null).bondedDevices())
    }

    @Test
    fun `bondedDevices swallows a SecurityException as an empty list`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.bondedDevices } throws SecurityException("missing BLUETOOTH_CONNECT")

        assertEquals(emptyList(), wrapper(adapter).bondedDevices())
    }

    // ---- remoteDevice ----

    @Test
    fun `remoteDevice resolves the device by address`() {
        val device = mockk<BluetoothDevice>()
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.getRemoteDevice("AA:AA:AA:AA:AA:AA") } returns device

        assertEquals(device, wrapper(adapter).remoteDevice("AA:AA:AA:AA:AA:AA"))
    }

    @Test
    fun `remoteDevice is null when there is no adapter`() {
        assertNull(wrapper(null).remoteDevice("AA:AA:AA:AA:AA:AA"))
    }

    @Test
    fun `remoteDevice swallows an invalid-address exception as null`() {
        val adapter = mockk<BluetoothAdapter>()
        every { adapter.getRemoteDevice(any<String>()) } throws IllegalArgumentException("bad address")

        assertNull(wrapper(adapter).remoteDevice("not-a-mac"))
    }
}
