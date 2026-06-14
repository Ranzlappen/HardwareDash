package dev.ranzlappen.gadget.feature.radios.ir

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the user's saved IR signal library via DataStore.
 *
 * Stored as a single JSON-encoded `List<IrSignal>` rather than using
 * `FeaturePreferences<T>` (which uses `Map<Int, T>`) — signal IDs are
 * UUID strings, and order matters for display.
 */
@Singleton
class IrSignalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.irSignalsDataStore

    val signals: Flow<List<IrSignal>> = dataStore.data.map { prefs ->
        prefs[KEY_SIGNALS]?.let { runCatching { json.decodeFromString<List<IrSignal>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun save(signal: IrSignal) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_SIGNALS]
                ?.let { runCatching { json.decodeFromString<List<IrSignal>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = current.filterNot { it.id == signal.id } + signal
            prefs[KEY_SIGNALS] = json.encodeToString(updated)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_SIGNALS]
                ?.let { runCatching { json.decodeFromString<List<IrSignal>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[KEY_SIGNALS] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    private companion object {
        val KEY_SIGNALS = stringPreferencesKey("signals")
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

private val Context.irSignalsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ir_signals")
