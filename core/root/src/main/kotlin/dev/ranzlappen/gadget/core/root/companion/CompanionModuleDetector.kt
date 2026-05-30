package dev.ranzlappen.gadget.core.root.companion

/**
 * Probes for the optional companion Magisk / KernelSU / APatch module.
 * Standard flavor returns [CompanionModuleStatus.NotInstalled]; rooted flavor
 * reads `/data/adb/modules/<id>/module.prop` (and the KernelSU equivalent)
 * via the privileged shell.
 *
 * Batch 2 stops here — there is intentionally no downloader or auto-flash.
 * UI that surfaces "install instructions" routes the user to
 * [installInstructionsUrl] in an external browser.
 */
interface CompanionModuleDetector {
    suspend fun status(): CompanionModuleStatus

    /** Public URL with installation instructions / release downloads. */
    val installInstructionsUrl: String

    /** Stable id of the companion module on disk (used as `/data/adb/modules/<id>`). */
    val moduleId: String
}

/**
 * Static description of the companion module — version expectations, display
 * name. The detector returns one of these as a const-like value; bumping the
 * required version is a one-line change in later batches.
 */
data class CompanionModuleDescriptor(
    val moduleId: String,
    val displayName: String,
    val requiredVersionName: String,
    val requiredVersionCode: Long,
    val installInstructionsUrl: String,
)
