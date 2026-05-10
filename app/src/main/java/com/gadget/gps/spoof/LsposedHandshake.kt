package com.gadget.gps.spoof

/**
 * Cross-classloader sentinel used by the bundled LSPosed module to advertise
 * "I am loaded into the target process". The LSPosed module's hook
 * initializer rewrites [loadedSentinel] to [EXPECTED_SENTINEL] when active.
 *
 * Lives in `src/main/` (not `src/rooted/`) because the LSPosed module APK is
 * a separate Gradle project and references this class by FQN at runtime via
 * `XposedHelpers.findClass(...)` — the FQN must be stable across both flavor
 * source sets.
 *
 * The sentinel string itself is arbitrary but must be unique enough that no
 * caller would accidentally write the same value. Bumping the value forces
 * re-detection (e.g. after changing the hook surface).
 */
object LsposedHandshake {

    const val EXPECTED_SENTINEL: String = "hwd-spoofer-v1-loaded"

    /**
     * Default value `null`. The LSPosed module's hook initializer overwrites
     * this to [EXPECTED_SENTINEL]. The rooted-flavor controller reads this
     * on every `capabilities()` call to detect "module is actually loaded
     * by LSPosed in our process".
     *
     * Volatile because the write happens from a different classloader.
     */
    @JvmStatic
    @Volatile
    var loadedSentinel: String? = null

    fun isLoaded(): Boolean = loadedSentinel == EXPECTED_SENTINEL
}
