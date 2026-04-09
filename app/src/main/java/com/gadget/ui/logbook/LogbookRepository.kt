package com.gadget.ui.logbook

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ---- DataStore singleton (keep "ticked_store" name for backwards compatibility) ----
private val Context.logbookDataStore: DataStore<Preferences> by preferencesDataStore(name = "ticked_store")

class LogbookRepository(private val context: Context) {

    companion object {
        private val KEY_STORE = stringPreferencesKey("ticked_store_json")
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }
    }

    // ---- Public API ----

    /** Observe the full store reactively. */
    val storeFlow: Flow<LogbookStore> = context.logbookDataStore.data.map { prefs ->
        val raw = prefs[KEY_STORE]
        if (raw != null) {
            try {
                val obj = Json.parseToJsonElement(raw).jsonObject
                val migrated = migrate(obj)
                json.decodeFromJsonElement<LogbookStore>(migrated)
            } catch (_: Exception) {
                LogbookStore()
            }
        } else {
            LogbookStore()
        }
    }

    /** Persist the full store. */
    suspend fun save(store: LogbookStore) {
        val payload = store.copy(
            version = LOGBOOK_SCHEMA_VERSION,
            savedAt = Instant.now().toString(),
        )
        val encoded = json.encodeToString(LogbookStore.serializer(), payload)
        context.logbookDataStore.edit { prefs ->
            prefs[KEY_STORE] = encoded
        }
    }

    // ---- Import / Export helpers ----

    /** Parse an imported JSON string, migrate it, and merge with current state. */
    fun parseImport(jsonString: String, current: LogbookStore): LogbookStore {
        val parsed = Json.parseToJsonElement(jsonString)

        // Support both legacy array format and new envelope format
        val envelope: JsonObject = if (parsed is JsonArray) {
            buildJsonObject {
                put("version", 1)
                put("entries", parsed)
                put("processes", JsonArray(emptyList()))
            }
        } else {
            parsed.jsonObject
        }

        val migrated = migrate(envelope)
        val imported = json.decodeFromJsonElement<LogbookStore>(migrated)

        // Merge with duplicate detection by ID
        val existingEntryIds = current.entries.map { it.id }.toSet()
        val newEntries = imported.entries.filter { it.id !in existingEntryIds && it.isoDate.isNotBlank() }
        val mergedEntries = (current.entries + newEntries).sortedByDescending { it.isoDate }

        val existingProcIds = current.processes.map { it.id }.toSet()
        val newProcs = imported.processes.filter { it.id !in existingProcIds }
        val mergedProcs = (current.processes + newProcs).sortedByDescending { it.isoDate }

        return current.copy(
            entries = mergedEntries,
            processes = mergedProcs,
            palette = imported.palette.ifEmpty { current.palette },
        )
    }

    /** Build the export JSON string (cross-compatible with web app — keeps "Ticked" name). */
    fun buildExportJson(store: LogbookStore): String {
        val export = LogbookExport(
            app = "Ticked",
            version = LOGBOOK_SCHEMA_VERSION,
            exportedAt = Instant.now().toString(),
            palette = store.palette,
            entries = store.entries,
            processes = store.processes,
        )
        return json.encodeToString(LogbookExport.serializer(), export)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Schema migrations v1 → v6 (mirrors web app's migrate() exactly)
    // ═══════════════════════════════════════════════════════════════════

    private fun migrate(raw: JsonObject): JsonObject {
        var store = raw
        val startVersion = store["version"]?.jsonPrimitive?.intOrNull ?: 1
        for (v in (startVersion + 1)..LOGBOOK_SCHEMA_VERSION) {
            store = when (v) {
                2 -> migrateV2(store)
                3 -> migrateV3(store)
                4 -> migrateV4(store)
                5 -> migrateV5(store)
                6 -> migrateV6(store)
                7 -> migrateV7(store)
                else -> store
            }
        }
        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("version", LOGBOOK_SCHEMA_VERSION)
        }
    }

    /** v2: Add `custom = false` default to entries that lack it. */
    private fun migrateV2(store: JsonObject): JsonObject {
        val entries = store["entries"]?.jsonArray ?: JsonArray(emptyList())
        val patched = JsonArray(entries.map { e ->
            val obj = e.jsonObject
            if ("custom" !in obj) {
                buildJsonObject {
                    obj.forEach { (k, v) -> put(k, v) }
                    put("custom", false)
                }
            } else obj
        })
        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("entries", patched)
            put("version", 2)
        }
    }

    /** v3: Normalise timestamps to ISO 8601; assign UUIDs if missing. */
    private fun migrateV3(store: JsonObject): JsonObject {
        val entries = store["entries"]?.jsonArray ?: JsonArray(emptyList())
        val patched = JsonArray(entries.map { e ->
            val obj = e.jsonObject
            buildJsonObject {
                obj.forEach { (k, v) -> put(k, v) }
                // Ensure id
                if ("id" !in obj || obj["id"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) {
                    put("id", UUID.randomUUID().toString())
                }
                // Normalise timestamp → isoDate
                val ts = obj["timestamp"]?.jsonPrimitive?.contentOrNull
                val iso = obj["isoDate"]?.jsonPrimitive?.contentOrNull
                if (iso.isNullOrBlank() && !ts.isNullOrBlank()) {
                    put("isoDate", normaliseToIso(ts))
                }
                // Ensure tags array
                if ("tags" !in obj) put("tags", JsonArray(emptyList()))
            }
        })
        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("entries", patched)
            put("version", 3)
        }
    }

    /** v4: Add bgColor, borderColor defaults; initialise palette; normalise processes. */
    private fun migrateV4(store: JsonObject): JsonObject {
        fun patchColors(arr: JsonArray): JsonArray = JsonArray(arr.map { e ->
            val obj = e.jsonObject
            buildJsonObject {
                obj.forEach { (k, v) -> put(k, v) }
                if ("bgColor" !in obj) put("bgColor", "")
                if ("borderColor" !in obj) put("borderColor", "")
                if ("tags" !in obj) put("tags", JsonArray(emptyList()))
            }
        })

        val entries = store["entries"]?.jsonArray ?: JsonArray(emptyList())
        val processes = store["processes"]?.jsonArray ?: JsonArray(emptyList())
        val palette = store["palette"]?.jsonArray

        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("entries", patchColors(entries))
            put("processes", patchColors(processes))
            if (palette == null) {
                put("palette", JsonArray(DEFAULT_PALETTE.map { JsonPrimitive(it) }))
            }
            put("version", 4)
        }
    }

    /**
     * v5: Convert fixed stages to dynamic checkpoints.
     * Old format had `stage` (index 0-4) mapping to fixed stage names.
     */
    private fun migrateV5(store: JsonObject): JsonObject {
        val fixedStageNames = listOf("Logged", "In Progress", "Review", "Done", "Archived")
        val processes = store["processes"]?.jsonArray ?: JsonArray(emptyList())

        val patched = JsonArray(processes.map { e ->
            val obj = e.jsonObject
            if ("checkpoints" in obj) return@map obj // Already migrated

            val stageIdx = obj["stage"]?.jsonPrimitive?.intOrNull ?: 0
            val isoDate = obj["isoDate"]?.jsonPrimitive?.contentOrNull ?: ""

            val checkpoints = JsonArray(fixedStageNames.mapIndexed { idx, name ->
                buildJsonObject {
                    put("id", UUID.randomUUID().toString())
                    put("name", name)
                    put("timestamp", if (idx == 0) isoDate else "")
                    put("comment", "")
                    put("dueDate", "")
                    put("remindAt", "")
                    put("notify", false)
                }
            })

            buildJsonObject {
                obj.forEach { (k, v) ->
                    if (k != "stage") put(k, v)
                }
                put("currentCheckpoint", stageIdx.coerceIn(0, fixedStageNames.lastIndex))
                put("checkpoints", checkpoints)
            }
        })

        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("processes", patched)
            put("version", 5)
        }
    }

    /** v6: Ensure all checkpoints have `remindAt` field. */
    private fun migrateV6(store: JsonObject): JsonObject {
        val processes = store["processes"]?.jsonArray ?: JsonArray(emptyList())

        val patched = JsonArray(processes.map { p ->
            val obj = p.jsonObject
            val cps = obj["checkpoints"]?.jsonArray ?: return@map obj
            val patchedCps = JsonArray(cps.map { c ->
                val cp = c.jsonObject
                if ("remindAt" in cp) cp
                else buildJsonObject {
                    cp.forEach { (k, v) -> put(k, v) }
                    put("remindAt", "")
                }
            })
            buildJsonObject {
                obj.forEach { (k, v) -> put(k, v) }
                put("checkpoints", patchedCps)
            }
        })

        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("processes", patched)
            put("version", 6)
        }
    }

    /** v7: Ensure all entries have `metrics` field. */
    private fun migrateV7(store: JsonObject): JsonObject {
        val entries = store["entries"]?.jsonArray ?: JsonArray(emptyList())
        val patched = JsonArray(entries.map { e ->
            val obj = e.jsonObject
            if ("metrics" in obj) obj
            else buildJsonObject {
                obj.forEach { (k, v) -> put(k, v) }
                put("metrics", buildJsonObject {})
            }
        })
        return buildJsonObject {
            store.forEach { (k, v) -> put(k, v) }
            put("entries", patched)
            put("version", 7)
        }
    }

    // ---- Timestamp helpers ----

    /** Best-effort normalise a legacy timestamp string to ISO 8601. */
    private fun normaliseToIso(raw: String): String {
        return try {
            Instant.parse(raw).toString()
        } catch (_: Exception) {
            try {
                // Try common formats: "MM/dd/yyyy, hh:mm:ss a"
                val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm:ss a")
                val ldt = LocalDateTime.parse(raw, formatter)
                ldt.atZone(ZoneId.systemDefault()).toInstant().toString()
            } catch (_: Exception) {
                raw // Return as-is if we can't parse
            }
        }
    }
}
