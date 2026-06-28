package dev.ranzlappen.gadget.feature.radios.subghz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The single source of truth for the Sub-GHz bridge state. Enumerates the USB
 * host bus for a recognised SDR / Sub-GHz transceiver and re-scans on every
 * attach/detach broadcast, so the screen, the metric source and the automation
 * handler all observe the same `@Singleton`.
 */
@Singleton
class SubghzMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usbManager =
        context.applicationContext.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val usbHostAvailable =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    private val _state = MutableStateFlow(buildCurrentState())
    val state: StateFlow<SubghzState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            _state.value = buildCurrentState()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(receiver, filter)
    }

    /** Re-scan on demand (e.g. when the screen resumes). */
    fun refresh() {
        _state.value = buildCurrentState()
    }

    private fun buildCurrentState(): SubghzState = SubghzState(
        usbHostAvailable = usbHostAvailable,
        device = attachedSdr(),
    )

    private fun attachedSdr(): SdrDevice? {
        val devices: Collection<UsbDevice> = usbManager?.deviceList?.values ?: return null
        return devices.firstNotNullOfOrNull { SdrDevice.match(it.vendorId, it.productId) }
    }
}
