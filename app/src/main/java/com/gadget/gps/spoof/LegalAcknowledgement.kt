package com.gadget.gps.spoof

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.spoofLegalDataStore by preferencesDataStore(name = "gps_spoof_legal")

/**
 * Persists the user's acknowledgement of the GPS spoofing legal disclaimer.
 * Bumping [CURRENT_VERSION] forces a re-prompt — useful if the disclaimer
 * text changes.
 */
@Singleton
class LegalAcknowledgement @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val acknowledgedVersion: Flow<Int> = context.spoofLegalDataStore.data
        .map { it[KEY_VERSION] ?: 0 }

    suspend fun isAcknowledged(): Boolean = acknowledgedVersion.first() >= CURRENT_VERSION

    suspend fun acknowledge() {
        context.spoofLegalDataStore.edit { prefs -> prefs[KEY_VERSION] = CURRENT_VERSION }
    }

    suspend fun reset() {
        context.spoofLegalDataStore.edit { prefs -> prefs[KEY_VERSION] = 0 }
    }

    companion object {
        const val CURRENT_VERSION = 1
        private val KEY_VERSION = intPreferencesKey("ack_version")
    }
}
