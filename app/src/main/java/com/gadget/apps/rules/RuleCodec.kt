package com.gadget.apps.rules

import kotlinx.serialization.json.Json

/**
 * JSON encode/decode for [FolderRule]. Produces objects shaped like
 * `{"type": "package_prefix", "prefix": "com.google."}` thanks to kotlinx
 * serialization's polymorphic handling of sealed classes.
 *
 * The format is forwards-compatible: unknown keys are ignored on decode, so
 * adding fields to a future rule variant won't break older clients reading
 * the same JSON.
 */
object RuleCodec {

    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun encode(rule: FolderRule): String =
        json.encodeToString(FolderRule.serializer(), rule)

    fun decode(jsonString: String): FolderRule? =
        runCatching { json.decodeFromString(FolderRule.serializer(), jsonString) }.getOrNull()
}
