package dev.ranzlappen.gadget.feature.usbdebug.control


/**
 * USB function role expressible via `cmd usb set-functions`. The wire
 * names differ between API 28 and API 30 — the helper rewrites the names
 * at write time so callers can use a single canonical enum.
 */
enum class UsbFunctionType(val wireName: String) {
    NONE("none"),
    MTP("mtp"),
    PTP("ptp"),
    RNDIS("rndis"),
    MIDI("midi"),
    NCM("ncm"),
    ACCESSORY("accessory"),
}
