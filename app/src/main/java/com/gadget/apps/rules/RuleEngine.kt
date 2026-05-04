package com.gadget.apps.rules

import com.gadget.data.db.apps.AppRecord

/**
 * Pure function over current state — given a [FolderRule], the manual
 * membership set (used only for [FolderRule.Manual]), the full app catalog,
 * and (optionally) usage stats, returns the apps that belong in the folder.
 *
 * No I/O, no Android types. Lives in `apps/rules/` so it can be exercised by
 * plain JVM unit tests.
 */
object RuleEngine {

    /**
     * @param rule              the folder's rule
     * @param manualMembership  appKeys the user explicitly added; consulted only
     *                          when [rule] is [FolderRule.Manual]
     * @param allApps           every record currently in `apps_record`
     * @param usage             optional usage stats; pass `null` if
     *                          PACKAGE_USAGE_STATS is not granted (then
     *                          [FolderRule.UnusedSinceDays] degrades to empty)
     * @param nowMillis         override for tests
     */
    fun materialize(
        rule: FolderRule,
        manualMembership: Set<String>,
        allApps: List<AppRecord>,
        usage: List<UsageEntry>? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<AppRecord> = when (rule) {
        FolderRule.Manual ->
            allApps.filter { it.appKey in manualMembership }

        is FolderRule.PackagePrefix ->
            allApps.filter { it.packageName.startsWith(rule.prefix) }

        is FolderRule.RecentlyInstalled -> {
            val cutoff = nowMillis - rule.days.daysToMillis()
            allApps.filter { !it.isWebLink && it.firstInstallTime >= cutoff }
        }

        FolderRule.WebApkOnly ->
            allApps.filter { it.isWebApk }

        is FolderRule.UnusedSinceDays -> {
            if (usage == null) {
                emptyList()
            } else {
                val cutoff = nowMillis - rule.days.daysToMillis()
                val recentlyUsed = usage
                    .asSequence()
                    .filter { it.lastUsedMillis >= cutoff }
                    .map { it.packageName }
                    .toHashSet()
                allApps.filter { !it.isWebLink && it.packageName !in recentlyUsed }
            }
        }
    }

    private fun Int.daysToMillis(): Long = this * 24L * 60 * 60 * 1000
}
