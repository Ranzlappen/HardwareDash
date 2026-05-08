package com.gadget.root.companion

/**
 * Shared constants describing the optional Magisk / KernelSU / APatch
 * companion module. Both flavors read these so the rooted detector and any
 * future "install instructions" UI agree on the moduleId and download URL.
 *
 * TODO(post-batch-N): when the companion repo exists, replace
 * [InstallInstructionsUrl] with the real GitHub releases URL.
 */
object CompanionModuleConstants {
    const val ModuleId: String = "gadget_root_companion"
    const val DisplayName: String = "Gadget Root Companion"
    const val RequiredVersionName: String = "0.1.0"
    const val RequiredVersionCode: Long = 1L
    const val InstallInstructionsUrl: String =
        "https://github.com/Ranzlappen/HardwareDash/releases"
}
