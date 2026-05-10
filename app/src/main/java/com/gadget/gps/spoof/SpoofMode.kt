package com.gadget.gps.spoof

/**
 * Which technical mechanism is currently driving the spoof. UI uses this for
 * the "active mode" chip(s) — multiple can be active simultaneously
 * (TestProvider AND LSPosed-hook).
 */
enum class SpoofMode {
    /** Standard flavor: user enabled HardwareDash as Mock Location App in Dev Options. */
    TestProviderUserGranted,

    /** Rooted flavor: AppOp granted via libsu, no Dev Options dance. */
    TestProviderRootGranted,

    /** Rooted + bundled LSPosed module loaded; isFromMockProvider/isMock are hidden. */
    LsposedHookActive,
}
