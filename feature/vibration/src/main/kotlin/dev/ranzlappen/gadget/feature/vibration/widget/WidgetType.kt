package dev.ranzlappen.gadget.feature.vibration.widget

import android.appwidget.AppWidgetProvider
import kotlinx.serialization.Serializable

/**
 * Variant of a vibration-related home-screen widget.
 *
 * Used inside [VibrationWidgetConfig] to discriminate between
 * [VibrateWidgetProvider] (a one-tap one-shot/predefined buzz) and
 * [PatternWidgetProvider] (plays a saved [dev.ranzlappen.gadget.feature.vibration.VibrationPattern]).
 *
 * Each variant carries its backing [AppWidgetProvider] subclass via
 * [providerClass] so call sites read the class directly off the type.
 *
 * **kotlinx.serialization note.** The generated enum serializer encodes the
 * discriminator by name only (`"Vibrate"` / `"Pattern"`); constructor args
 * outside the serialized form are ignored automatically.
 */
@Serializable
enum class WidgetType(val providerClass: Class<out AppWidgetProvider>) {
    Vibrate(VibrateWidgetProvider::class.java),
    Pattern(PatternWidgetProvider::class.java),
}
