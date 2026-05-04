package com.gadget.apps.rules

import com.gadget.data.db.apps.AppRecord

/**
 * Pure function over current state — given a [FolderRuleSet], the manual
 * membership set, the full app catalog, and (optionally) usage stats,
 * returns the apps that belong in the folder.
 *
 * Semantics: the result is the **union** of (a) all manual entries the user
 * explicitly checked in the editor and (b) every app that matches *any* rule
 * in the set. An empty rule set means "manual entries only" — the new
 * implicit default replacing the old [FolderRule.Manual] tag.
 *
 * No I/O, no Android types. Lives in `apps/rules/` so it can be exercised by
 * plain JVM unit tests.
 */
object RuleEngine {

    /**
     * @param ruleSet           the folder's rules (may be empty)
     * @param manualMembership  appKeys the user explicitly added; always
     *                          included in the result regardless of rules
     * @param allApps           every record currently in `apps_record`
     * @param usage             optional usage stats; pass `null` if
     *                          PACKAGE_USAGE_STATS is not granted (then
     *                          [FolderRule.UnusedSinceDays] degrades to empty)
     * @param nowMillis         override for tests
     */
    fun materialize(
        ruleSet: FolderRuleSet,
        manualMembership: Set<String>,
        allApps: List<AppRecord>,
        usage: List<UsageEntry>? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<AppRecord> {
        val keyToRecord = allApps.associateBy { it.appKey }
        val result = LinkedHashMap<String, AppRecord>()

        // Manual entries first — they keep their user-defined sort order via
        // the caller's iteration order (the editor provides a sorted set).
        for (key in manualMembership) {
            val record = keyToRecord[key] ?: continue
            result.putIfAbsent(key, record)
        }
        // Then every app matched by any rule. Existing keys aren't replaced
        // (manual entries win over rule-derived ones for ordering).
        for (rule in ruleSet.rules) {
            for (record in evaluateRule(rule, allApps, usage, nowMillis)) {
                result.putIfAbsent(record.appKey, record)
            }
        }
        return result.values.toList()
    }

    private fun evaluateRule(
        rule: FolderRule,
        allApps: List<AppRecord>,
        usage: List<UsageEntry>?,
        nowMillis: Long,
    ): List<AppRecord> = when (rule) {
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

        FolderRule.OnInternalStorage ->
            allApps.filter { !it.isWebLink && !it.isOnExternalStorage }

        FolderRule.OnExternalStorage ->
            allApps.filter { !it.isWebLink && it.isOnExternalStorage }

        FolderRule.SystemApps ->
            allApps.filter { !it.isWebLink && it.isSystemApp }

        FolderRule.UserApps ->
            allApps.filter { !it.isWebLink && !it.isSystemApp }
    }

    private fun Int.daysToMillis(): Long = this * 24L * 60 * 60 * 1000
}
