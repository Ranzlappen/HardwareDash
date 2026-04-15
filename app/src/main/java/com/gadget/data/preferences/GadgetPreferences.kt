package com.gadget.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instances (one per logical group)
private val Context.gadgetSettingsStore: DataStore<Preferences>
        by preferencesDataStore(name = "gadget_settings_ds")

private val Context.accessibilityStore: DataStore<Preferences>
        by preferencesDataStore(name = "accessibility_ds")

/**
 * Unified DataStore-based preferences for Gadget.
 *
 * Migrates from SharedPreferences on first access.
 * Widget-related preferences remain in SharedPreferences
 * because widget providers need synchronous reads in onUpdate().
 */
@Singleton
class GadgetPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // ── Language ────────────────────────────────────────────────────────

    private val languageKey = stringPreferencesKey("language_code")

    val language: Flow<String> = context.gadgetSettingsStore.data.map { prefs ->
        prefs[languageKey] ?: migrateLanguageFromSharedPrefs()
    }

    suspend fun setLanguage(code: String) {
        context.gadgetSettingsStore.edit { prefs ->
            prefs[languageKey] = code
        }
    }

    private fun migrateLanguageFromSharedPrefs(): String {
        return try {
            val old = context.getSharedPreferences("localization_prefs", Context.MODE_PRIVATE)
            val code = old.getString("language_code", "en") ?: "en"
            Timber.d("Migrated language preference: %s", code)
            code
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate language preference")
            "en"
        }
    }

    // ── Accessibility ──────────────────────────────────────────────────

    private val highContrastKey = booleanPreferencesKey("high_contrast")
    private val largeTextKey = booleanPreferencesKey("large_text")
    private val reducedMotionKey = booleanPreferencesKey("reduced_motion")

    val highContrast: Flow<Boolean> = context.accessibilityStore.data.map { prefs ->
        prefs[highContrastKey] ?: migrateAccessibilityBool("high_contrast", false)
    }

    val largeText: Flow<Boolean> = context.accessibilityStore.data.map { prefs ->
        prefs[largeTextKey] ?: migrateAccessibilityBool("large_text", false)
    }

    val reducedMotion: Flow<Boolean> = context.accessibilityStore.data.map { prefs ->
        prefs[reducedMotionKey] ?: migrateAccessibilityBool("reduced_motion", false)
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.accessibilityStore.edit { it[highContrastKey] = enabled }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.accessibilityStore.edit { it[largeTextKey] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.accessibilityStore.edit { it[reducedMotionKey] = enabled }
    }

    private fun migrateAccessibilityBool(key: String, default: Boolean): Boolean {
        return try {
            val old = context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)
            old.getBoolean(key, default)
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate accessibility pref: %s", key)
            default
        }
    }

    // ── Onboarding ────────────────────────────────────────────────────

    private val hasSeenOnboardingKey = booleanPreferencesKey("has_seen_onboarding")

    val hasSeenOnboarding: Flow<Boolean> = context.gadgetSettingsStore.data.map { prefs ->
        prefs[hasSeenOnboardingKey] ?: context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
            .getBoolean("has_seen_onboarding", false)
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.gadgetSettingsStore.edit { it[hasSeenOnboardingKey] = seen }
    }

    // ── Strobe settings ────────────────────────────────────────────────

    private val strobeFreqKey = floatPreferencesKey("strobe_freq_hz")

    val strobeFrequency: Flow<Float> = context.gadgetSettingsStore.data.map { prefs ->
        prefs[strobeFreqKey] ?: context.getSharedPreferences("strobe_settings", Context.MODE_PRIVATE)
            .getFloat("strobe_freq_hz", 10f)
    }

    suspend fun setStrobeFrequency(freq: Float) {
        context.gadgetSettingsStore.edit { it[strobeFreqKey] = freq }
    }

    // ── DND Bypass ────────────────────────────────────────────────────

    private val bypassDndKey = booleanPreferencesKey("bypass_dnd")

    val bypassDnd: Flow<Boolean> = context.gadgetSettingsStore.data.map { prefs ->
        prefs[bypassDndKey] ?: context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
            .getBoolean("bypass_dnd", false)
    }

    suspend fun setBypassDnd(enabled: Boolean) {
        context.gadgetSettingsStore.edit { it[bypassDndKey] = enabled }
    }

    // ── Widget settings (remain in SharedPreferences for sync access) ──
    // phone_ring_duration_seconds, metric_log_*, widget_config, etc.
    // These are NOT migrated because widget providers need synchronous reads.
}
