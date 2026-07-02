package dev.ranzlappen.gadget.feature.gps.spoof

/**
 * Snapshot of what's possible on this device + flavor right now. UI's
 * capability card renders one row per field; the per-vector detection matrix
 * is computed downstream from these flags.
 */
data class SpoofCapabilities(
    /** True if the user has selected HardwareDash as Mock Location App in Dev Options. */
    val mockLocationAppSelected: Boolean = false,

    /**
     * True if some OTHER package currently holds the Mock Location App slot.
     * UI surfaces this as a conflict requiring the user to switch us in.
     */
    val competingMockLocationAppActive: Boolean = false,
    val competingMockLocationAppPackage: String? = null,

    /** Rooted flavor only: libsu is available and `su` was acquired at least once. */
    val rootGranted: Boolean = false,

    /** Rooted flavor only: LSPosed manager package is installed. */
    val lsposedFrameworkActive: Boolean = false,

    /** Rooted flavor only: our LSPosed module APK is installed (may not be enabled yet). */
    val lsposedModuleInstalled: Boolean = false,

    /** Rooted flavor only: LSPosed handshake confirms the module is loaded after reboot. */
    val lsposedModuleLoaded: Boolean = false,

    /** Bundled LSPosed module versionCode in `assets/`. 0 if no asset. */
    val lsposedBundledVersion: Int = 0,

    /** Installed LSPosed module versionCode (PackageManager.getPackageInfo). 0 if not installed. */
    val lsposedInstalledVersion: Int = 0,

    /** Set of modes that would actually drive emissions if start() ran right now. */
    val activeModes: Set<SpoofMode> = emptySet(),

    /** Human-readable per-API caveats (e.g. "LSPosed unsupported on API 29"). */
    val perApiCaveats: List<String> = emptyList(),
)
