package com.gadget.automation

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private val ALLOWED_SECTIONS = listOf(
    "activity",
    "package",
    "deviceidle",
    "alarm",
    "statusbar",
)
private const val SECTION_TAIL_CAP_BYTES = 8 * 1024

/**
 * Read-only `dumpsys` snapshot for a fixed allow-list of sections.
 * Each section is tail-capped to 8 KB so a runaway dump can't fill the
 * shell buffer.
 */
@Singleton
class DumpsysHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun snapshot(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (section in ALLOWED_SECTIONS) {
            val result = shell.exec("dumpsys $section 2>/dev/null | tail -c $SECTION_TAIL_CAP_BYTES")
            if (result.isSuccess) {
                out[section] = result.stdout.joinToString("\n").take(SECTION_TAIL_CAP_BYTES)
            } else {
                out[section] = "(unavailable)"
            }
        }
        return out
    }
}
