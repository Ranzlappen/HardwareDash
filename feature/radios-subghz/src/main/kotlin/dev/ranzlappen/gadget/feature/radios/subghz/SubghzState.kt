package dev.ranzlappen.gadget.feature.radios.subghz

/**
 * Read-only snapshot of the Sub-GHz bridge surfaced to the UI. A bridge is
 * "connected" when a recognised SDR / Sub-GHz transceiver is on the USB host
 * bus; everything else is graceful-unavailable.
 */
data class SubghzState(
    /** Whether the host platform exposes a USB host bus at all. */
    val usbHostAvailable: Boolean = false,
    /** A recognised SDR / Sub-GHz dongle currently attached, if any. */
    val device: SdrDevice? = null,
) {
    /** A usable Sub-GHz bridge is attached. */
    val bridgeConnected: Boolean get() = device != null
}
