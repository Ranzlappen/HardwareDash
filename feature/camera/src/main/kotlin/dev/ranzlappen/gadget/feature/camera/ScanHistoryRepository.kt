package dev.ranzlappen.gadget.feature.camera

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

@Singleton
class ScanHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.scanHistoryDataStore

    val history: Flow<List<BarcodeResult>> = dataStore.data.map { prefs ->
        prefs[KEY_HISTORY]
            ?.let { runCatching { json.decodeFromString<List<BarcodeResult>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun add(result: BarcodeResult) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_HISTORY]
                ?.let { runCatching { json.decodeFromString<List<BarcodeResult>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = (listOf(result) + current).take(MAX_HISTORY)
            prefs[KEY_HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY_HISTORY) }
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("history")
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        const val MAX_HISTORY = 20
    }
}

private val Context.scanHistoryDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "scan_history")
