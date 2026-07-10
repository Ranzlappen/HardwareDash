package dev.ranzlappen.gadget.feature.adbdebug.control

/**
 * UI-facing mirror of the setprop allow-list enforced authoritatively by
 * `SetPropHelper.SETPROP_EXACT_ALLOW_LIST` in `:feature:adbdebug-rooted`.
 *
 * Duplicated here (rather than shared) because the base `:feature:adbdebug`
 * module — which owns [dev.ranzlappen.gadget.feature.adbdebug.AdbDebugScreen]
 * and therefore must build on the standard flavor too — cannot depend on
 * `:feature:adbdebug-rooted` (a rooted-flavor-only sibling module; see that
 * module's `build.gradle.kts`). The list here is display-only: the actual
 * allow-list is re-enforced server-side inside `SetPropHelper.apply()`
 * regardless of what the picker offers, so a stale/incomplete UI list can
 * never widen what a caller can write.
 */
object AdbSetPropAllowList {
    val EXACT_KEYS: List<String> = listOf(
        "debug.hwui.renderer",
        "debug.hwui.profile",
        "debug.egl.profiler",
        "dalvik.vm.heapsize",
        "dalvik.vm.heapgrowthlimit",
        "persist.adb.tcp.port",
        "persist.sys.usb.config",
    )

    /** Free-text keys are only accepted under this prefix. */
    const val LOG_TAG_PREFIX: String = "log.tag."
}

/**
 * UI-facing mirror of the port bounds enforced authoritatively by
 * `AdbNetworkHelper` (`ADB_NETWORK_PORT_MIN`/`MAX`, both `internal` to
 * `:feature:adbdebug-rooted`). See [AdbSetPropAllowList] for why this is
 * duplicated rather than shared.
 */
object AdbNetworkPortRange {
    const val MIN: Int = 5555
    const val MAX: Int = 5599
    const val DEFAULT: Int = MIN
}
