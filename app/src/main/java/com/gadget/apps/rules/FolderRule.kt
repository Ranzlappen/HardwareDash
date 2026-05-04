package com.gadget.apps.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How a folder decides which apps it contains.
 *
 *  - [Manual]              — only apps the user explicitly added (rows in
 *                            `apps_folder_app`).
 *  - [PackagePrefix]       — every record whose `packageName` starts with the
 *                            given prefix. Useful for "Google", "Samsung", etc.
 *  - [RecentlyInstalled]   — every record whose `firstInstallTime` is within
 *                            the last [days] days.
 *  - [WebApkOnly]          — every record flagged as a Chrome WebAPK PWA.
 *  - [UnusedSinceDays]     — every record NOT used within the last [days]
 *                            according to UsageStats. Requires the
 *                            `PACKAGE_USAGE_STATS` permission to materialize;
 *                            without it the engine returns an empty list.
 *
 * Persisted as JSON via [RuleCodec] in `apps_folder_rule.rule_json`.
 */
@Serializable
sealed class FolderRule {

    @Serializable
    @SerialName("manual")
    object Manual : FolderRule()

    @Serializable
    @SerialName("package_prefix")
    data class PackagePrefix(val prefix: String) : FolderRule()

    @Serializable
    @SerialName("recently_installed")
    data class RecentlyInstalled(val days: Int) : FolderRule()

    @Serializable
    @SerialName("web_apk_only")
    object WebApkOnly : FolderRule()

    @Serializable
    @SerialName("unused_since_days")
    data class UnusedSinceDays(val days: Int) : FolderRule()
}
