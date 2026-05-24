package dev.ranzlappen.gadget.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository surface over [DataStore]&lt;[Preferences]&gt; that
 * exposes [UserPreferences] as a `Flow` plus per-field suspend
 * setters.
 *
 * The repository is the **only** code in the codebase that reads
 * or writes Preferences keys directly. Every other consumer goes
 * through this typed surface:
 *
 * ```kotlin
 * class SettingsViewModel @Inject constructor(
 *     private val prefs: UserPreferencesRepository,
 * ) : ViewModel() {
 *     val preferences: StateFlow<UserPreferences> =
 *         prefs.flow.stateIn(viewModelScope, WhileSubscribed(5000), UserPreferences())
 *
 *     fun setDarkThemeMode(mode: DarkThemeMode) =
 *         viewModelScope.launch { prefs.setDarkThemeMode(mode) }
 * }
 * ```
 *
 * Adding a new preference is a four-line change here:
 *   1. Add a field + default to [UserPreferences].
 *   2. Add a key constant to [UserPreferencesKeys].
 *   3. Map the new key in [readFrom].
 *   4. Add the corresponding `suspend fun set…(…)`.
 *
 * Errors are surfaced via [Flow] mapping — a corrupted Preferences
 * file falls back to the [UserPreferences] defaults rather than
 * crashing the app.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** Reactive stream of the current user preferences. */
    val flow: Flow<UserPreferences> = dataStore.data.map { it.readFrom() }

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        dataStore.edit { it[UserPreferencesKeys.DARK_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[UserPreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setReducedMotionOverride(value: TriStatePreference) {
        dataStore.edit { it[UserPreferencesKeys.REDUCED_MOTION_OVERRIDE] = value.name }
    }

    suspend fun setReducedTransparency(enabled: Boolean) {
        dataStore.edit { it[UserPreferencesKeys.REDUCED_TRANSPARENCY] = enabled }
    }

    suspend fun setLargeTextOverride(enabled: Boolean) {
        dataStore.edit { it[UserPreferencesKeys.LARGE_TEXT_OVERRIDE] = enabled }
    }

    suspend fun setDefaultStrobeRateHz(rateHz: Float) {
        dataStore.edit { it[UserPreferencesKeys.DEFAULT_STROBE_RATE_HZ] = rateHz }
    }

    suspend fun setMorseText(text: String) {
        dataStore.edit { it[UserPreferencesKeys.MORSE_TEXT] = text }
    }

    private fun Preferences.readFrom(): UserPreferences = UserPreferences(
        darkThemeMode = this[UserPreferencesKeys.DARK_THEME_MODE]
            ?.let { runCatching { DarkThemeMode.valueOf(it) }.getOrNull() }
            ?: DarkThemeMode.FollowSystem,
        dynamicColor = this[UserPreferencesKeys.DYNAMIC_COLOR] ?: true,
        reducedMotionOverride = this[UserPreferencesKeys.REDUCED_MOTION_OVERRIDE]
            ?.let { runCatching { TriStatePreference.valueOf(it) }.getOrNull() }
            ?: TriStatePreference.FollowSystem,
        reducedTransparency = this[UserPreferencesKeys.REDUCED_TRANSPARENCY] ?: false,
        largeTextOverride = this[UserPreferencesKeys.LARGE_TEXT_OVERRIDE] ?: false,
        defaultStrobeRateHz = this[UserPreferencesKeys.DEFAULT_STROBE_RATE_HZ]
            ?: UserPreferences.DEFAULT_STROBE_RATE_HZ,
        morseText = this[UserPreferencesKeys.MORSE_TEXT] ?: UserPreferences.DEFAULT_MORSE_TEXT,
    )
}

/** Internal key registry — one place to look for "what does X persist as?". */
private object UserPreferencesKeys {
    val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val REDUCED_MOTION_OVERRIDE = stringPreferencesKey("reduced_motion_override")
    val REDUCED_TRANSPARENCY = booleanPreferencesKey("reduced_transparency")
    val LARGE_TEXT_OVERRIDE = booleanPreferencesKey("large_text_override")
    val DEFAULT_STROBE_RATE_HZ = floatPreferencesKey("default_strobe_rate_hz")
    val MORSE_TEXT = stringPreferencesKey("morse_text")
}
