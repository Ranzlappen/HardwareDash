package dev.ranzlappen.gadget.feature.apps.rules

import kotlinx.serialization.json.Json

/**
 * JSON encode/decode for [FolderRuleSet]. The set wraps a `List<FolderRule>`,
 * so polymorphic dispatch on the sealed `FolderRule` is handled by kotlinx-
 * serialization's "type" discriminator under `rules: [{...}, {...}]`.
 *
 * `decode` is forwards-AND-backwards-compatible:
 *  - New format `{"rules":[...]}` decodes directly.
 *  - Legacy format from the single-rule era (`{"type":"package_prefix",...}`)
 *    is wrapped into a one-element set so existing folders keep working.
 *  - Legacy `{"type":"manual"}` (the dropped Manual rule) decodes to an empty
 *    rule set — semantically equivalent now that the manual list is always
 *    union'd in regardless of rules.
 *  - Garbage input returns an empty set rather than throwing, so corrupted
 *    DB rows can't crash the rule editor.
 */
object RuleCodec {

    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun encode(set: FolderRuleSet): String =
        json.encodeToString(FolderRuleSet.serializer(), set)

    fun decode(jsonString: String): FolderRuleSet {
        // Try the modern set-shaped format first -- but only when the JSON
        // actually looks like {"rules":[...]}. Every FolderRuleSet field has
        // a default, so with ignoreUnknownKeys=true a legacy single-rule blob
        // like {"type":"package_prefix",...} would otherwise decode
        // "successfully" as an empty set instead of falling through to the
        // legacy branches below, silently dropping the rule.
        if (jsonString.contains("\"rules\"")) {
            runCatching { json.decodeFromString(FolderRuleSet.serializer(), jsonString) }
                .getOrNull()
                ?.let { return it }
        }

        // Legacy "manual" tag → empty set.
        if (jsonString.contains("\"type\":\"manual\"")) return FolderRuleSet(emptyList())

        // Legacy single-rule format → wrap.
        val singleRule = runCatching {
            json.decodeFromString(FolderRule.serializer(), jsonString)
        }.getOrNull()
        return singleRule?.let { FolderRuleSet(listOf(it)) } ?: FolderRuleSet(emptyList())
    }
}
