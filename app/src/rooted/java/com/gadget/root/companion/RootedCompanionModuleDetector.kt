package com.gadget.root.companion

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor companion module detector. Uses the privileged shell to read
 * `module.prop` from the Magisk / KernelSU module directory. Batch 2 stops
 * at detection — no downloader, no auto-flash. The "install instructions"
 * URL is exposed via [installInstructionsUrl] for UI to open in a browser.
 */
@Singleton
class RootedCompanionModuleDetector @Inject constructor(
    private val shell: RootShell,
) : CompanionModuleDetector {

    override val moduleId: String = CompanionModuleConstants.ModuleId
    override val installInstructionsUrl: String = CompanionModuleConstants.InstallInstructionsUrl

    override suspend fun status(): CompanionModuleStatus {
        val moduleProp = readModuleProp() ?: return CompanionModuleStatus.NotInstalled
        val versionName = moduleProp["version"] ?: return CompanionModuleStatus.NotInstalled
        val versionCode = moduleProp["versionCode"]?.toLongOrNull() ?: 0L

        return if (versionCode < CompanionModuleConstants.RequiredVersionCode) {
            CompanionModuleStatus.Outdated(
                installedVersionName = versionName,
                requiredVersionName = CompanionModuleConstants.RequiredVersionName,
            )
        } else {
            CompanionModuleStatus.Installed(versionName, versionCode)
        }
    }

    private suspend fun readModuleProp(): Map<String, String>? {
        // Magisk and APatch share /data/adb/modules/<id>/module.prop layout.
        // KernelSU uses /data/adb/ksu/modules/<id>/module.prop.
        val candidates = listOf(
            "/data/adb/modules/$moduleId/module.prop",
            "/data/adb/ksu/modules/$moduleId/module.prop",
        )
        for (path in candidates) {
            val result = shell.exec("cat \"$path\" 2>/dev/null")
            if (result.isSuccess && result.stdout.isNotEmpty()) {
                return parseProperties(result.stdout)
            }
        }
        return null
    }

    private fun parseProperties(lines: List<String>): Map<String, String> =
        lines.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val eq = trimmed.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            trimmed.substring(0, eq).trim() to trimmed.substring(eq + 1).trim()
        }.toMap()
}
