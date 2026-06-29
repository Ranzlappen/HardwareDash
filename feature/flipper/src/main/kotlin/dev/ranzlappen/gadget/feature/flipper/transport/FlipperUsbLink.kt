package dev.ranzlappen.gadget.feature.flipper.transport

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * USB CDC-ACM connection to a Flipper Zero. The Flipper exposes a virtual
 * serial port at 115200-8-N-1; usb-serial-for-android handles the CDC plumbing.
 */
class FlipperUsbLink private constructor(
    private val port: UsbSerialPort,
) : FlipperLink {

    override val transportName = "USB"

    private val incoming = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            incoming.tryEmit(data)
        }

        override fun onRunError(e: Exception) {
            Timber.w(e, "USB I/O error")
        }
    }).apply { start() }

    override fun incoming(): Flow<ByteArray> = incoming.asSharedFlow()

    override suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        port.write(data, WRITE_TIMEOUT_MS)
        Unit
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        runCatching { ioManager.stop() }
        runCatching { port.close() }
        Unit
    }

    companion object {
        private const val WRITE_TIMEOUT_MS = 2000
        private const val ACTION_USB_PERMISSION = "dev.ranzlappen.gadget.feature.flipper.USB_PERMISSION"

        /** Lists Flipper Zero devices currently attached. */
        fun listDevices(context: Context): List<UsbDevice> {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            return drivers.map { it.device }
                .filter { it.vendorId == FLIPPER_VID && it.productId == FLIPPER_PID }
        }

        const val FLIPPER_VID = 0x0483
        const val FLIPPER_PID = 0x5740

        /**
         * Open the first Flipper found, requesting permission if needed.
         * Returns null if no Flipper is attached.
         */
        suspend fun open(context: Context): FlipperUsbLink? {
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val driver = UsbSerialProber.getDefaultProber()
                .findAllDrivers(manager)
                .firstOrNull { it.device.vendorId == FLIPPER_VID && it.device.productId == FLIPPER_PID }
                ?: return null

            if (!manager.hasPermission(driver.device)) {
                requestPermission(context, manager, driver.device)
                if (!manager.hasPermission(driver.device)) return null
            }

            val connection = manager.openDevice(driver.device) ?: error("Cannot open USB device")
            val port = driver.ports.firstOrNull() ?: error("Flipper has no serial port")
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            runCatching { port.setDTR(true) }
            runCatching { port.setRTS(true) }
            return FlipperUsbLink(port)
        }

        private suspend fun requestPermission(context: Context, manager: UsbManager, device: UsbDevice) {
            val granted = CompletableDeferred<Boolean>()
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_USB_PERMISSION) {
                        granted.complete(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        runCatching { context.unregisterReceiver(this) }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION))
            }
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
                PendingIntent.FLAG_IMMUTABLE,
            )
            manager.requestPermission(device, pi)
            granted.await()
        }
    }
}
