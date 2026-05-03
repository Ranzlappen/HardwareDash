package com.gadget.ui.theme

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * User-configurable theme preferences.
 *
 * @param presetId Identifier of the active [ColorPreset]; see [ColorPresets].
 */
data class ThemePrefs(
    val presetId: String = DefaultColorPreset.id,
)

/**
 * CompositionLocal providing [ThemePrefs] to the composable tree.
 * Provided at the root by [GadgetTheme].
 */
val LocalThemePreferences = compositionLocalOf { ThemePrefs() }

/**
 * Singleton manager for theme preferences. Mirrors [AccessibilityPreferencesManager]
 * (SharedPreferences + mutableStateOf), so theme changes recompose immediately.
 */
object ThemePreferencesManager {

    private const val PREFS_NAME = "gadget_theme_prefs"
    private const val KEY_PRESET_ID = "color_preset_id"

    private val _prefs = mutableStateOf(ThemePrefs())
    val prefs: State<ThemePrefs> = _prefs

    /**
     * Initialize from persisted preferences. Call once in Activity.onCreate().
     */
    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _prefs.value = ThemePrefs(
            presetId = sp.getString(KEY_PRESET_ID, DefaultColorPreset.id) ?: DefaultColorPreset.id,
        )
    }

    fun setPreset(context: Context, presetId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PRESET_ID, presetId).apply()
        _prefs.value = _prefs.value.copy(presetId = presetId)
    }
}
