package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice
import android.content.Context
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [BtViewModel]'s non-passthrough logic:
 *  - seeding [BtState.isRootedFlavor] from [RootCapabilityRegistry] at construction,
 *  - [BtViewModel.refresh] marking bonded devices connected from
 *    [BtEnhancedInfoProvider.connectedAddresses] and kicking off enrichment only when
 *    at least one device is connected, and
 *  - [BtViewModel.enrichConnectedDevices]'s fallback chain: the rooted hidden-battery
 *    API takes priority when non-null; otherwise BLE/Dual devices fall back to GATT
 *    battery+RSSI, and Classic devices with no hidden reading are left untouched.
 *
 * `BtViewModel.checkPermission()` branches on `Build.VERSION.SDK_INT >= S`; this repo
 * has no Robolectric shadow, so `SDK_INT` resolves to the stub jar's default of `0` and
 * that branch always takes the `else -> true` path (mirrors the documented convention in
 * `SettingsViewModelTest`). So every scenario here necessarily runs with
 * `permissionGranted == true` on the initial `refresh()`; the real permission-denied
 * path is left to an instrumented test.
 */
class BtViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val adapter = mockk<BluetoothAdapterWrapper>()
    private val enhancedInfo = mockk<BtEnhancedInfoProvider>()
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { adapter.isAvailable() } returns true
        every { adapter.isEnabled() } returns true
        every { adapter.name() } returns "Pixel"
        every { adapter.bondedDevices() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun rootRegistry(isRootedFlavor: Boolean): RootCapabilityRegistry {
        val registry = mockk<RootCapabilityRegistry>(relaxed = true)
        every { registry.isRootedFlavor } returns isRootedFlavor
        return registry
    }

    private fun createViewModel(isRootedFlavor: Boolean = false): BtViewModel =
        BtViewModel(context, adapter, enhancedInfo, rootRegistry(isRootedFlavor))

    // ---- construction ----

    @Test
    fun `seeds isRootedFlavor from the root capability registry`() {
        every { enhancedInfo.connectedAddresses() } returns emptySet()

        assertTrue(createViewModel(isRootedFlavor = true).state.value.isRootedFlavor)
        assertFalse(createViewModel(isRootedFlavor = false).state.value.isRootedFlavor)
    }

    // ---- refresh ----

    @Test
    fun `refresh seeds adapter availability, enabled state and name`() {
        every { enhancedInfo.connectedAddresses() } returns emptySet()

        val state = createViewModel().state.value

        assertTrue(state.adapterAvailable)
        assertTrue(state.adapterEnabled)
        assertEquals("Pixel", state.adapterName)
        assertTrue(state.permissionGranted)
    }

    @Test
    fun `refresh does not trigger enrichment when no device is connected`() {
        val device = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        every { adapter.bondedDevices() } returns listOf(device)
        every { enhancedInfo.connectedAddresses() } returns emptySet()

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.bondedDevices.size)
        assertFalse(state.bondedDevices.single().isConnected)
        coVerify(exactly = 0) { enhancedInfo.readGattBatteryAndRssi(any()) }
        coVerify(exactly = 0) { enhancedInfo.hiddenBatteryLevel(any()) }
    }

    @Test
    fun `refresh marks the matching bonded device connected`() {
        val watch = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        val speaker = BluetoothDeviceInfo(name = "Speaker", address = "BB:BB:BB:BB:BB:BB", typeName = "Classic")
        every { adapter.bondedDevices() } returns listOf(watch, speaker)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        every { adapter.remoteDevice(any()) } returns null

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val devices = viewModel.state.value.bondedDevices.associateBy { it.address }
        assertTrue(devices.getValue("AA:AA:AA:AA:AA:AA").isConnected)
        assertFalse(devices.getValue("BB:BB:BB:BB:BB:BB").isConnected)
    }

    // ---- enrichConnectedDevices ----

    @Test
    fun `hidden battery level takes priority over the GATT fallback`() {
        val watch = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        every { adapter.bondedDevices() } returns listOf(watch)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        val rawDevice = mockk<BluetoothDevice>()
        every { adapter.remoteDevice("AA:AA:AA:AA:AA:AA") } returns rawDevice
        coEvery { enhancedInfo.hiddenBatteryLevel(rawDevice) } returns 77
        coEvery { enhancedInfo.a2dpCodecName(rawDevice) } returns "LDAC"

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val device = viewModel.state.value.bondedDevices.single()
        assertEquals(77, device.batteryPercent)
        assertEquals("LDAC", device.codecName)
        coVerify(exactly = 0) { enhancedInfo.readGattBatteryAndRssi(any()) }
    }

    @Test
    fun `BLE devices fall back to GATT battery and rssi when there is no hidden reading`() {
        val watch = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        every { adapter.bondedDevices() } returns listOf(watch)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        val rawDevice = mockk<BluetoothDevice>()
        every { adapter.remoteDevice("AA:AA:AA:AA:AA:AA") } returns rawDevice
        coEvery { enhancedInfo.hiddenBatteryLevel(rawDevice) } returns null
        coEvery { enhancedInfo.readGattBatteryAndRssi(rawDevice) } returns (60 to -50)

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val device = viewModel.state.value.bondedDevices.single()
        assertEquals(60, device.batteryPercent)
        assertEquals(-50, device.rssiDbm)
        assertNull(device.codecName)
    }

    @Test
    fun `Dual devices also use the GATT fallback`() {
        val earbuds = BluetoothDeviceInfo(name = "Buds", address = "AA:AA:AA:AA:AA:AA", typeName = "Dual")
        every { adapter.bondedDevices() } returns listOf(earbuds)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        val rawDevice = mockk<BluetoothDevice>()
        every { adapter.remoteDevice("AA:AA:AA:AA:AA:AA") } returns rawDevice
        coEvery { enhancedInfo.hiddenBatteryLevel(rawDevice) } returns null
        coEvery { enhancedInfo.readGattBatteryAndRssi(rawDevice) } returns (33 to -80)

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val device = viewModel.state.value.bondedDevices.single()
        assertEquals(33, device.batteryPercent)
        assertEquals(-80, device.rssiDbm)
    }

    @Test
    fun `Classic devices with no hidden reading are left untouched`() {
        val speaker = BluetoothDeviceInfo(name = "Speaker", address = "AA:AA:AA:AA:AA:AA", typeName = "Classic")
        every { adapter.bondedDevices() } returns listOf(speaker)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        val rawDevice = mockk<BluetoothDevice>()
        every { adapter.remoteDevice("AA:AA:AA:AA:AA:AA") } returns rawDevice
        coEvery { enhancedInfo.hiddenBatteryLevel(rawDevice) } returns null

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val device = viewModel.state.value.bondedDevices.single()
        assertNull(device.batteryPercent)
        assertNull(device.rssiDbm)
        coVerify(exactly = 0) { enhancedInfo.readGattBatteryAndRssi(any()) }
    }

    @Test
    fun `a missing raw device is left untouched without calling the enhanced info provider`() {
        val watch = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        every { adapter.bondedDevices() } returns listOf(watch)
        every { enhancedInfo.connectedAddresses() } returns setOf("AA:AA:AA:AA:AA:AA")
        every { adapter.remoteDevice("AA:AA:AA:AA:AA:AA") } returns null

        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val device = viewModel.state.value.bondedDevices.single()
        assertNull(device.batteryPercent)
        coVerify(exactly = 0) { enhancedInfo.hiddenBatteryLevel(any()) }
        coVerify(exactly = 0) { enhancedInfo.readGattBatteryAndRssi(any()) }
    }

    // ---- onPermissionResult ----

    @Test
    fun `onPermissionResult(false) updates the flag without re-running refresh`() {
        val watch = BluetoothDeviceInfo(name = "Watch", address = "AA:AA:AA:AA:AA:AA", typeName = "BLE")
        every { adapter.bondedDevices() } returns listOf(watch)
        every { enhancedInfo.connectedAddresses() } returns emptySet()
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPermissionResult(false)

        assertFalse(viewModel.state.value.permissionGranted)
        // refresh() was not re-run: the bonded-device snapshot from init is untouched.
        assertEquals(1, viewModel.state.value.bondedDevices.size)
    }

    @Test
    fun `onPermissionResult(true) sets the flag and re-runs refresh`() {
        every { enhancedInfo.connectedAddresses() } returns emptySet()
        val viewModel = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPermissionResult(true)

        assertTrue(viewModel.state.value.permissionGranted)
        assertEquals("Pixel", viewModel.state.value.adapterName)
    }
}
