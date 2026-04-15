package com.gadget.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * User-configurable accessibility preferences.
 *
 * @param highContrast  Boosts color contrast for better visibility
 * @param largeText     Scales text size 1.2× throughout the app
 * @param reducedMotion Disables non-essential animations (also true when system animator scale is 0)
 */
data class AccessibilityPrefs(
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val reducedMotion: Boolean = false,
)

/**
 * CompositionLocal providing [AccessibilityPrefs] to the composable tree.
 * Provided at the root by [GadgetTheme].
 */
val LocalAccessibilityPreferences = compositionLocalOf { AccessibilityPrefs() }

/**
 * Singleton manager for accessibility preferences, following the same
 * SharedPreferences + mutableStateOf pattern as [com.gadget.localization.LocalizationManager].
 */
object AccessibilityPreferencesManager {

    private const val PREFS_NAME = "gadget_accessibility"
    private const val KEY_HIGH_CONTRAST = "high_contrast_enabled"
    private const val KEY_LARGE_TEXT = "large_text_enabled"
    private const val KEY_REDUCED_MOTION = "reduced_motion_enabled"

    private val _prefs = mutableStateOf(AccessibilityPrefs())
    val prefs: State<AccessibilityPrefs> = _prefs

    private var systemReducedMotion = false

    /**
     * Initialize from persisted preferences. Call once in Activity.onCreate().
     */
    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        systemReducedMotion = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
            ) == 0f
        } catch (_: Settings.SettingNotFoundException) {
            false
        }

        _prefs.value = AccessibilityPrefs(
            highContrast = sp.getBoolean(KEY_HIGH_CONTRAST, false),
            largeText = sp.getBoolean(KEY_LARGE_TEXT, false),
            reducedMotion = sp.getBoolean(KEY_REDUCED_MOTION, false) || systemReducedMotion,
        )
    }

    fun setHighContrast(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
        _prefs.value = _prefs.value.copy(highContrast = enabled)
    }

    fun setLargeText(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LARGE_TEXT, enabled).apply()
        _prefs.value = _prefs.value.copy(largeText = enabled)
    }

    fun setReducedMotion(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        _prefs.value = _prefs.value.copy(reducedMotion = enabled || systemReducedMotion)
    }
}
