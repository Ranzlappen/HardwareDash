package dev.ranzlappen.gadget.core.root.companion

/**
 * Result of probing the device for the optional Magisk / KernelSU / APatch
 * companion module that augments the rooted APK's capabilities. The app
 * works fully without it; presence simply unlocks extra features in later
 * batches.
 */
sealed class CompanionModuleStatus {
    data object NotInstalled : CompanionModuleStatus()
    data class Installed(val versionName: String, val versionCode: Long) : CompanionModuleStatus()
    data class Outdated(val installedVersionName: String, val requiredVersionName: String) : CompanionModuleStatus()
}
