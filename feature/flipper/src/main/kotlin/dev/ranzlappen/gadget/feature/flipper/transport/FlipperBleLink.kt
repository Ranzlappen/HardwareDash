package dev.ranzlappen.gadget.feature.flipper.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * BLE GATT connection to a Flipper Zero. Flipper exposes a Nordic UART-style
 * service: a TX characteristic the central writes to, and an RX characteristic
 * the peripheral notifies on. UUIDs come from the published flipperzero docs.
 */
@SuppressLint("MissingPermission")
class FlipperBleLink private constructor(
    private val gatt: BluetoothGatt,
    private val rxCharacteristic: BluetoothGattCharacteristic,
    private val txCharacteristic: BluetoothGattCharacteristic,
    private val callbacks: Callbacks,
) : FlipperLink {

    override val transportName = "BLE"

    private val incoming = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val writeMutex = Mutex()

    init {
        callbacks.onIncoming = { incoming.tryEmit(it) }
    }

    override fun incoming(): Flow<ByteArray> = incoming.asSharedFlow()

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        val mtu = callbacks.mtuPayload
        var offset = 0
        writeMutex.withLock {
            while (offset < data.size) {
                val chunk = data.copyOfRange(offset, minOf(offset + mtu, data.size))
                @Suppress("DEPRECATION")
                txCharacteristic.value = chunk
                txCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                val written = CompletableDeferred<Boolean>()
                callbacks.onWriteComplete = { written.complete(it) }
                @Suppress("DEPRECATION")
                val ok = gatt.writeCharacteristic(txCharacteristic)
                if (!ok || !written.await()) error("BLE write failed at offset $offset")
                offset += chunk.size
            }
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        runCatching { gatt.disconnect() }
        runCatching { gatt.close() }
        Unit
    }

    /** Mutable callback hooks shared with the GATT callback. */
    private class Callbacks {
        var onIncoming: ((ByteArray) -> Unit)? = null
        var onWriteComplete: ((Boolean) -> Unit)? = null
        var mtuPayload: Int = 20
    }

    companion object {
        // Flipper "Serial Service" GATT UUIDs (Nordic UART variant).
        val SERVICE_UUID: UUID = UUID.fromString("8fe5b3d5-2e7f-4a98-2a48-7acc60fe0000")
        val RX_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e62fe0000")
        val TX_UUID: UUID = UUID.fromString("19ed82ae-ed21-4c9d-4145-228e63fe0000")
        private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun isFlipper(device: BluetoothDevice): Boolean {
            val name = runCatching { device.name }.getOrNull() ?: return false
            return name.startsWith("Flipper ", ignoreCase = true)
        }

        suspend fun connect(context: Context, device: BluetoothDevice): FlipperBleLink {
            val cb = Callbacks()
            val connected = CompletableDeferred<Boolean>()
            val servicesDiscovered = CompletableDeferred<BluetoothGatt>()
            val mtuNegotiated = CompletableDeferred<Int>()

            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (!connected.isCompleted) connected.complete(true)
                        g.requestMtu(247)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (!connected.isCompleted) connected.complete(false)
                    }
                }

                override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                    cb.mtuPayload = (mtu - 3).coerceAtLeast(20)
                    if (!mtuNegotiated.isCompleted) mtuNegotiated.complete(mtu)
                    g.discoverServices()
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    if (!servicesDiscovered.isCompleted) {
                        if (status == BluetoothGatt.GATT_SUCCESS) servicesDiscovered.complete(g)
                        else servicesDiscovered.completeExceptionally(
                            IllegalStateException("Service discovery failed: $status"),
                        )
                    }
                }

                @Suppress("DEPRECATION")
                override fun onCharacteristicChanged(
                    g: BluetoothGatt,
                    c: BluetoothGattCharacteristic,
                ) {
                    if (c.uuid == RX_UUID) {
                        cb.onIncoming?.invoke(c.value.copyOf())
                    }
                }

                override fun onCharacteristicWrite(
                    g: BluetoothGatt,
                    c: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    if (c.uuid == TX_UUID) {
                        cb.onWriteComplete?.invoke(status == BluetoothGatt.GATT_SUCCESS)
                    }
                }
            }

            val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            } ?: error("connectGatt returned null")

            try {
                if (!connected.await()) error("BLE connection failed")
                mtuNegotiated.await()
                val g = servicesDiscovered.await()
                val service = g.getService(SERVICE_UUID) ?: error("Flipper Serial Service not found")
                val rx = service.getCharacteristic(RX_UUID) ?: error("RX characteristic missing")
                val tx = service.getCharacteristic(TX_UUID) ?: error("TX characteristic missing")
                g.setCharacteristicNotification(rx, true)
                rx.getDescriptor(CCCD_UUID)?.let {
                    @Suppress("DEPRECATION")
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(it)
                }
                return FlipperBleLink(g, rx, tx, cb)
            } catch (t: Throwable) {
                runCatching { gatt.close() }
                throw t
            }
        }

        @Suppress("DEPRECATION")
        fun bondedFlippers(context: Context): List<BluetoothDevice> {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
            return runCatching {
                adapter.bondedDevices.filter { isFlipper(it) }
            }.getOrDefault(emptyList()).also {
                Timber.d("Found ${it.size} bonded Flippers")
            }
        }
    }
}
