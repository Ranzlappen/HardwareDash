package com.gadget.adbdebug

/**
 * Enables or disables ADB-over-network. [enabled] = false flips the
 * listener off (port `-1`). Otherwise the helper writes [port] to
 * `service.adb.tcp.port` and restarts adbd. The helper rejects anything
 * outside 5555–5599.
 */
data class AdbNetworkConfig(
    val enabled: Boolean,
    val port: Int,
)

/**
 * Allow-listed `setprop` override. The helper enforces the allow-list
 * regardless of caller. [value] is written verbatim — the kernel decides
 * whether the value is acceptable for that key.
 */
data class SetPropConfig(
    val key: String,
    val value: String,
)
