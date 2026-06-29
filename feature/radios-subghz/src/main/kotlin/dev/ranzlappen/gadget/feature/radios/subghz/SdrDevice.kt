package dev.ranzlappen.gadget.feature.radios.subghz

/**
 * The catalogue of USB SDR / Sub-GHz transceivers this module can recognise
 * on the host bus. Android has no first-party Sub-GHz radio API, so the only
 * honest standard-flavor capability is *detecting* an attached bridge; the
 * actual register/tuning access is a rooted one-up (see the capability rows).
 *
 * Identification is by USB vendor/product id — the values are stable across
 * firmware revisions and need no device permission to read. Pure data + a
 * pure lookup so it round-trips in a plain JVM unit test.
 */
enum class SdrDevice(
    val vendorId: Int,
    val productId: Int,
    val displayName: String,
    /** True for dongles whose radio actually covers the sub-GHz ISM bands. */
    val coversSubGhz: Boolean,
) {
    RtlSdr2832(0x0bda, 0x2832, "RTL-SDR (RTL2832U)", coversSubGhz = false),
    RtlSdr2838(0x0bda, 0x2838, "RTL-SDR (RTL2838)", coversSubGhz = false),
    HackRfOne(0x1d50, 0x6089, "HackRF One", coversSubGhz = true),
    YardStickOne(0x1d50, 0x605b, "YARD Stick One (CC1111)", coversSubGhz = true),
    LimeSdrMini(0x1d50, 0x6108, "LimeSDR Mini", coversSubGhz = true),
    Cc1101Cdc(0x10c4, 0xea60, "CC1101 (CP210x bridge)", coversSubGhz = true),
    ;

    companion object {
        /**
         * Resolve a connected device by its USB ids, or `null` when the pair
         * matches nothing this module knows about.
         */
        fun match(vendorId: Int, productId: Int): SdrDevice? =
            entries.firstOrNull { it.vendorId == vendorId && it.productId == productId }
    }
}
