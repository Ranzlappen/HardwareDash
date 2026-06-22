package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class RootedBtEnhancedInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gate: RootSafetyGate,
) : BtEnhancedInfoProvider {

    private val bluetoothManager get() =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    override val isRootedFlavor = true

    @Volatile private var a2dpProxy: BluetoothA2dp? = null
    private val a2dpMutex = Mutex()

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
     * Returns the negotiated A2DP codec name for [device] (e.g. "AAC", "LDAC",
     * "aptX") via [BluetoothA2dp.getCodecStatus] (API 29+).
     *
     * A [BluetoothA2dp] profile proxy is acquired lazily on first call and
     * cached for the process lifetime — subsequent calls are instant. Returns
     * null if the gate blocks, API < 29, the proxy times out (3 s), the device
     * has no active A2DP connection, or the device doesn't report codec info.
     */
    override suspend fun a2dpCodecName(device: BluetoothDevice): String? {
        if (gate.check(RootFeatureKey.BluetoothA2dpCodecReflection) != RootGateDecision.Allowed) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val proxy = acquireA2dpProxy() ?: return null
            readA2dpCodec(proxy, device)
        }.getOrNull()
    }

    /**
     * Acquires a [BluetoothA2dp] profile proxy, waiting up to 3 s for
     * [BluetoothProfile.ServiceListener.onServiceConnected]. The proxy is
     * cached; [onServiceDisconnected] clears it so the next call re-binds.
     */
    private suspend fun acquireA2dpProxy(): BluetoothA2dp? {
        a2dpProxy?.let { return it }
        return a2dpMutex.withLock {
            a2dpProxy?.let { return@withLock it }
            val adapter = bluetoothManager?.adapter ?: return@withLock null
            withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine { cont ->
                    val listener = object : BluetoothProfile.ServiceListener {
                        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                            val p = proxy as? BluetoothA2dp
                            a2dpProxy = p
                            if (!cont.isCompleted) cont.resume(p)
                        }
                        override fun onServiceDisconnected(profile: Int) {
                            a2dpProxy = null
                        }
                    }
                    @Suppress("MissingPermission")
                    adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)
                }
            }
        }
    }

    // BluetoothA2dp.getCodecStatus() is @hide — access via reflection.
    private fun readA2dpCodec(proxy: BluetoothA2dp, device: BluetoothDevice): String? {
        val status = runCatching {
            proxy.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
                .invoke(proxy, device)
        }.getOrNull() ?: return null
        val config = runCatching {
            status.javaClass.getMethod("getCodecConfig").invoke(status)
        }.getOrNull() ?: return null
        val codecType = runCatching {
            config.javaClass.getMethod("getCodecType").invoke(config) as? Int
        }.getOrNull() ?: return null
        return when (codecType) {
            0 -> "SBC"
            1 -> "AAC"
            2 -> "aptX"
            3 -> "aptX HD"
            4 -> "LDAC"
            6 -> "LC3"
            else -> null
        }
    }
}
