package com.gadget.apps.rules

/**
 * Minimal projection of `UsageStatsManager.queryUsageStats(...)` results that
 * [RuleEngine] needs. The engine never touches Android types directly so it
 * stays unit-testable on the JVM.
 */
data class UsageEntry(
    val packageName: String,
    val lastUsedMillis: Long,
)
