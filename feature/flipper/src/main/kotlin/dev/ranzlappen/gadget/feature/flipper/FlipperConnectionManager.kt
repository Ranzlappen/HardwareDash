package dev.ranzlappen.gadget.feature.flipper

import android.bluetooth.BluetoothDevice
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.flipper.rpc.FlipperRpcClient
import dev.ranzlappen.gadget.feature.flipper.rpc.InfraredCommands
import dev.ranzlappen.gadget.feature.flipper.rpc.StorageCommands
import dev.ranzlappen.gadget.feature.flipper.rpc.SubGhzCommands
import dev.ranzlappen.gadget.feature.flipper.rpc.SystemCommands
import dev.ranzlappen.gadget.feature.flipper.transport.FlipperBleLink
import dev.ranzlappen.gadget.feature.flipper.transport.FlipperLink
import dev.ranzlappen.gadget.feature.flipper.transport.FlipperUsbLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val RPC_HANDSHAKE_DELAY_MS = 150L

/**
 * Owns the active [FlipperLink] and the [FlipperRpcClient] that talks over it.
 * Exposes a [state] StateFlow the UI can render; surfaces command suites via
 * [system], [storage], [subGhz], [infrared].
 */
@Singleton
class FlipperConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    sealed interface State {
        data object Disconnected : State
        data class Connecting(val transport: String) : State
        data class Connected(
            val transport: String,
            val deviceName: String?,
            val firmwareVersion: String?,
            val batteryPercent: Int?,
        ) : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    private val mutex = Mutex()
    private var link: FlipperLink? = null
    private var client: FlipperRpcClient? = null

    val system: SystemCommands? get() = client?.let { SystemCommands(it) }
    val storage: StorageCommands? get() = client?.let { StorageCommands(it) }
    val subGhz: SubGhzCommands?
        get() = client?.let { c -> SubGhzCommands(c, StorageCommands(c)) }
    val infrared: InfraredCommands?
        get() = client?.let { c -> InfraredCommands(c, StorageCommands(c)) }

    suspend fun connectUsb() = mutex.withLock {
        disconnectInternal()
        _state.value = State.Connecting("USB")
        try {
            val newLink = FlipperUsbLink.open(context) ?: error("No Flipper Zero attached via USB")
            attach(newLink)
        } catch (t: Throwable) {
            Timber.w(t, "USB connect failed")
            _state.value = State.Failed(t.message ?: "USB connection failed")
        }
    }

    suspend fun connectBle(device: BluetoothDevice) = mutex.withLock {
        disconnectInternal()
        _state.value = State.Connecting("BLE")
        try {
            val newLink = FlipperBleLink.connect(context, device)
            attach(newLink)
        } catch (t: Throwable) {
            Timber.w(t, "BLE connect failed")
            _state.value = State.Failed(t.message ?: "BLE connection failed")
        }
    }

    /** Flipper Zeros currently bonded over Bluetooth (for the BLE picker). */
    fun bondedFlippers(): List<BluetoothDevice> = FlipperBleLink.bondedFlippers(context)

    suspend fun disconnect() = mutex.withLock { disconnectInternal() }

    private suspend fun disconnectInternal() {
        client?.runCatching { stop() }
        link?.runCatching { close() }
        client = null
        link = null
        _state.value = State.Disconnected
    }

    private suspend fun attach(newLink: FlipperLink) = withContext(Dispatchers.IO) {
        link = newLink
        val rpc = FlipperRpcClient(newLink).also { it.start() }
        client = rpc
        // Give the Flipper a moment to flush any greeting text, then enter RPC mode.
        delay(RPC_HANDSHAKE_DELAY_MS)
        rpc.enterRpcSession()
        delay(RPC_HANDSHAKE_DELAY_MS)

        val sys = SystemCommands(rpc)
        val deviceInfo = runCatching { sys.deviceInfo() }.getOrDefault(SystemCommands.DeviceInfo(emptyMap()))
        val powerInfo = runCatching { sys.powerInfo() }.getOrDefault(SystemCommands.PowerInfo(emptyMap()))
        _state.value = State.Connected(
            transport = newLink.transportName,
            deviceName = deviceInfo.hardwareName,
            firmwareVersion = deviceInfo.firmwareVersion,
            batteryPercent = powerInfo.batteryLevel,
        )
    }
}
