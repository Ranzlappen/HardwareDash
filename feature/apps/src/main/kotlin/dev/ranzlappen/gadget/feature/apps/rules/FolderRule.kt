package dev.ranzlappen.gadget.feature.apps.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Smart-folder rule. A folder's full membership is the union of its manual
 * entries (rows in `apps_folder_app`) and the apps matched by every rule in
 * its [FolderRuleSet] — see [RuleEngine.materialize].
 *
 * Variants:
 *  - [PackagePrefix]       — every record whose `packageName` starts with the
 *                            given prefix. Useful for "Google", "Samsung", …
 *  - [RecentlyInstalled]   — every record whose `firstInstallTime` is within
 *                            the last [days] days.
 *  - [WebApkOnly]          — every record flagged as a Chrome WebAPK PWA.
 *  - [UnusedSinceDays]     — every record NOT used within the last [days]
 *                            according to UsageStats. Requires the
 *                            `PACKAGE_USAGE_STATS` permission to materialize;
 *                            without it the engine returns an empty list.
 *  - [OnInternalStorage]   — apps installed on internal storage only.
 *  - [OnExternalStorage]   — apps installed on the SD card / adoptable
 *                            external storage. Most modern devices return
 *                            an empty list for this rule.
 *  - [SystemApps]          — pre-installed / OEM apps (FLAG_SYSTEM /
 *                            FLAG_UPDATED_SYSTEM_APP).
 *  - [UserApps]            — non-system, user-installed apps (the inverse of
 *                            [SystemApps]).
 *
 * Persisted as JSON via [RuleCodec] inside `apps_folder_rule.rule_json`,
 * wrapped in a [FolderRuleSet] to support multiple rules per folder.
 *
 * The `@SerialName` discriminators are pinned to the legacy wire strings so
 * folders persisted by the legacy `com.gadget.apps.rules` codec decode
 * unchanged after the migration.
 */
@Serializable
sealed class FolderRule {

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

    @Serializable
    @SerialName("on_internal_storage")
    object OnInternalStorage : FolderRule()

    @Serializable
    @SerialName("on_external_storage")
    object OnExternalStorage : FolderRule()

    @Serializable
    @SerialName("system_apps")
    object SystemApps : FolderRule()

    @Serializable
    @SerialName("user_apps")
    object UserApps : FolderRule()
}

/**
 * The complete rule configuration for one folder. Empty list means "manual
 * apps only" — i.e. only the explicitly checked entries from the editor's
 * picker, no smart-rule contribution.
 */
@Serializable
data class FolderRuleSet(
    val rules: List<FolderRule> = emptyList(),
)
