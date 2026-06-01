package dev.ranzlappen.gadget.feature.vibration.widget

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import kotlinx.serialization.Serializable

/**
 * Per-instance configuration for a home-screen vibration widget.
 *
 * One [VibrationWidgetConfig] persists per `appWidgetId` inside the
 * `vibration_widgets` Preferences DataStore. The widget provider reads it in
 * `onUpdate` / `onReceive` to drive both the RemoteViews presentation and the
 * Intent extras passed to
 * [dev.ranzlappen.gadget.feature.vibration.VibrationPlaybackService].
 *
 * Fields:
 * - [type] — discriminates the one-tap vibrate vs saved-pattern variants.
 * - [displayName] — user-facing label shown in the in-app widgets list.
 * - [amplitudePercent] — strength for the [WidgetType.Vibrate] variant
 *   (1–100). Ignored for the pattern variant.
 * - [durationMillis] — buzz length for the [WidgetType.Vibrate] variant.
 * - [patternId] — the saved [dev.ranzlappen.gadget.feature.vibration.VibrationPattern]
 *   id for the [WidgetType.Pattern] variant; empty otherwise.
 * - [appearance] — visual chrome + tap behaviour + toggle feedback.
 * - [removed] — soft-delete flag (the "deleted in-app but still placed"
 *   inert-render pattern; see [WidgetKitConfig]).
 */
@Serializable
@Immutable
data class VibrationWidgetConfig(
    val type: WidgetType,
    override val displayName: String,
    val amplitudePercent: Int = DEFAULT_AMPLITUDE_PERCENT,
    val durationMillis: Long = DEFAULT_DURATION_MS,
    val patternId: String = "",
    override val appearance: WidgetAppearance = WidgetAppearance(),
    override val removed: Boolean = false,
    override val schemaVersion: Int = SCHEMA_VERSION,
) : WidgetKitConfig {
    companion object {
        /** Current serialized schema version. */
        const val SCHEMA_VERSION: Int = 1

        const val DEFAULT_AMPLITUDE_PERCENT: Int = 60
        const val DEFAULT_DURATION_MS: Long = 300L

        /** UI slider bounds for the one-tap vibrate config. */
        const val MIN_AMPLITUDE_PERCENT: Int = 1
        const val MAX_AMPLITUDE_PERCENT: Int = 100
        const val MIN_DURATION_MS: Long = 10L
        const val MAX_DURATION_MS: Long = 5_000L
    }
}
