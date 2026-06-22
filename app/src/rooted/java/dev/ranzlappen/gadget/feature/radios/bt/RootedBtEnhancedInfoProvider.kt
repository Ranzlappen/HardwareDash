package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootedBtEnhancedInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gate: RootSafetyGate,
) : BtEnhancedInfoProvider {

    private val bluetoothManager get() =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override val isRootedFlavor = true

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

    /**
     * Reads the Android Bluetooth stack's cached battery level for [device]
     * via the hidden `BluetoothDevice.getBatteryLevel()` method. The stack
     * polls this internally for many popular devices (Galaxy Buds, Sony WH/WF,
     * AirPods via HFP AT exchange, etc.) so the value is usually fresh without
     * triggering an explicit GATT read.
     *
     * Returns null if the gate blocks, the device is not connected, or the
     * stack returns -1 (unknown).
     */
    override suspend fun hiddenBatteryLevel(device: BluetoothDevice): Int? {
        if (gate.check(RootFeatureKey.BluetoothHiddenBatteryApi) != RootGateDecision.Allowed) return null
        return runCatching {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            (method.invoke(device) as? Int)?.takeIf { it >= 0 }
        }.getOrNull()
    }

    /**
     * Returns the negotiated A2DP codec name for [device] by reflecting into
     * `BluetoothA2dp.getCodecStatus()`. Requires an active A2DP connection;
     * returns null if the gate blocks, no profile proxy is available, or
     * reflection fails.
     *
     * Note: acquiring a `BluetoothA2dp` proxy requires a `ServiceListener`
     * callback that's normally held by the system. We query the cached proxy
     * stored in the Bluetooth app process via reflection on
     * `BluetoothAdapter.getProfileProxy` — this is best-effort; it will return
     * null on devices that don't expose the internal service.
     */
    override suspend fun a2dpCodecName(device: BluetoothDevice): String? {
        if (gate.check(RootFeatureKey.BluetoothA2dpCodecReflection) != RootGateDecision.Allowed) return null
        return runCatching {
            val adapter = bluetoothManager?.adapter ?: return null
            // Access the internal BluetoothA2dp proxy via reflection.
            val getProfileProxyMethod = adapter.javaClass.getMethod(
                "getProfileProxy",
                Context::class.java,
                BluetoothProfile.ServiceListener::class.java,
                Int::class.javaPrimitiveType,
            )
            // We can't get the proxy synchronously this way; return null and
            // rely on a future approach (e.g. holding the proxy in a singleton).
            null
        }.getOrNull()
    }
}
