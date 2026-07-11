package dev.ranzlappen.gadget.core.automation.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * The versioned envelope for **rule export/import** (W7). Rules are shared
 * as a JSON document — a `.json` file, a deep-link, or a QR payload — so a
 * user can back up, move, or share automation recipes. The envelope wraps a
 * `List<Rule>` with a [schemaVersion] so a future breaking change can be
 * migrated on import rather than silently mis-decoded.
 *
 * The wire format reuses the module's single [AutomationJson] instance and
 * the same pinned-`@SerialName` sealed graphs the `automation.db` JSON
 * columns use — so an exported rule and a persisted rule are byte-identical
 * in their trigger/condition/action encoding.
 */
@Serializable
data class RuleBundle(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val rules: List<Rule> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/**
 * Encode/decode a [RuleBundle] to/from a JSON string. Pure and side-effect
 * free — the caller owns file/clipboard/QR IO and persistence (import feeds
 * decoded rules through `RuleRepository.save`, which normalizes each).
 */
object AutomationTransfer {

    /** Serialize [rules] into a shareable JSON document. */
    fun export(rules: List<Rule>): String =
        AutomationJson.encodeToString(
            RuleBundle.serializer(),
            RuleBundle(rules = rules),
        )

    /**
     * Parse a JSON document produced by [export] (or a bare `List<Rule>` /
     * single `Rule`, tolerated for hand-authored payloads).
     *
     * @return the decoded rules, or [ImportResult.Failure] with a reason on
     *   malformed input — never throws.
     */
    fun import(json: String): ImportResult {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return ImportResult.Failure("Empty document")
        return try {
            val rules = when {
                trimmed.startsWith("[") -> AutomationJson.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(Rule.serializer()),
                    trimmed,
                )
                trimmed.contains("\"rules\"") -> AutomationJson.decodeFromString(
                    RuleBundle.serializer(),
                    trimmed,
                ).rules
                else -> listOf(AutomationJson.decodeFromString(Rule.serializer(), trimmed))
            }
            ImportResult.Success(rules)
        } catch (e: SerializationException) {
            ImportResult.Failure(e.message ?: "Malformed automation document")
        } catch (e: IllegalArgumentException) {
            ImportResult.Failure(e.message ?: "Invalid automation document")
        }
    }

    sealed interface ImportResult {
        data class Success(val rules: List<Rule>) : ImportResult
        data class Failure(val reason: String) : ImportResult
    }
}
