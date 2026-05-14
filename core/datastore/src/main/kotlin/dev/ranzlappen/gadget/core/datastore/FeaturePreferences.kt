package dev.ranzlappen.gadget.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Generic per-feature preferences collection.
 *
 * Wraps a [DataStore]&lt;[Preferences]&gt; with a typed Map&lt;Int, T&gt;
 * API. Each entry is stored as a JSON-encoded string under a stable
 * preference key of the form `"$keyPrefix$id"`; e.g. with
 * `keyPrefix = "widget_"`, the entry for `id = 42` lives under
 * `widget_42`.
 *
 * The integer key fits the natural shape of consumers that already deal
 * in OS-assigned integer IDs (AppWidget IDs, notification IDs,
 * `Sensor.getId()`, etc.). Other future modules with non-Int keys can
 * either map to an Int domain or grow a sibling `FeaturePreferences<K, T>`
 * abstraction when needed.
 *
 * Construct one of these per feature collection via
 * [FeaturePreferencesFactory.create]. Don't call the constructor
 * directly — the factory plugs in the right [DataStore] instance and
 * the shared [Json] configuration so behavior stays consistent across
 * features.
 *
 * @param T the @Serializable value type.
 * @param dataStore the backing Preferences DataStore (one per feature).
 * @param keyPrefix the namespace prefix for this collection's keys
 *                  inside the DataStore. Lets a single DataStore file
 *                  host multiple distinct collections without key
 *                  collision (currently we use one file per collection
 *                  for simplicity, but the prefix gives us headroom).
 * @param serializer kotlinx.serialization serializer for [T].
 * @param json the configured [Json] instance.
 */
class FeaturePreferences<T : Any> internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val keyPrefix: String,
    private val serializer: KSerializer<T>,
    private val json: Json,
) {

    /**
     * Reactive snapshot of every entry in this collection.
     *
     * Emits a fresh map every time any entry changes. Corrupted entries
     * (failed JSON decode — schema drift or a bad write) are silently
     * dropped from the emitted map rather than failing the whole flow.
     * Schema migrations should land before any field-removal commit;
     * the silent drop is a safety net, not a feature.
     */
    val all: Flow<Map<Int, T>> = dataStore.data.map { prefs ->
        prefs.asMap().entries
            .mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(keyPrefix)) return@mapNotNull null
                val idText = name.removePrefix(keyPrefix)
                val id = idText.toIntOrNull() ?: return@mapNotNull null
                val rawJson = value as? String ?: return@mapNotNull null
                runCatching { json.decodeFromString(serializer, rawJson) }
                    .getOrNull()
                    ?.let { id to it }
            }
            .toMap()
    }

    /**
     * One-shot snapshot of every entry. Useful for synchronous-ish
     * reads from a [android.appwidget.AppWidgetProvider.onUpdate]
     * pathway where suspending into a flow collection is awkward.
     */
    suspend fun getAll(): Map<Int, T> = all.first()

    /** One-shot read of a single entry. */
    suspend fun get(id: Int): T? = getAll()[id]

    /**
     * Persist a value under [id]. Replaces any existing entry. The
     * [all] flow emits the updated map after the underlying
     * DataStore commit completes.
     */
    suspend fun save(id: Int, value: T) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("$keyPrefix$id")] =
                json.encodeToString(serializer, value)
        }
    }

    /**
     * Remove the entry for [id]. No-op if no such entry exists.
     */
    suspend fun delete(id: Int) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("$keyPrefix$id"))
        }
    }
}
