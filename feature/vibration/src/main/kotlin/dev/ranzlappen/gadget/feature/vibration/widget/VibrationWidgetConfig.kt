package dev.ranzlappen.gadget.feature.vibration.widget

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-instance configuration for a home-screen vibration widget (schema v2 —
 * the function-driven model).
 *
 * One [VibrationWidgetConfig] persists per `appWidgetId` inside the
 * `vibration_widgets` Preferences DataStore. The generic
 * [dev.ranzlappen.gadget.core.widgetkit.provider.BaseGadgetWidgetProvider]
 * resolves the bound [dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction]
 * from [actionKey] and dispatches it through the
 * [dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionDispatcher]
 * (which routes to [dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler]),
 * so the widget never starts a hardware service directly. Every vibration
 * function is **momentary** (a discrete buzz / pattern play), so the widget
 * never shows a persistent active state.
 *
 * Fields:
 * - [actionKey] — the bound function id; [FUNCTION_ONESHOT] (== the action
 *   handler's `oneshot` key) or [FUNCTION_PATTERN] (== `pattern_play`), plus the
 *   rooted function ids on the rooted flavor.
 * - [params] — raw string action params keyed by the function's
 *   [dev.ranzlappen.gadget.core.automation.ActionParam.name] (`amplitude` /
 *   `duration_ms` for one-shot, `pattern_id` for pattern).
 * - [sizePreset] — the user's chosen starting size (a density hint).
 * - [displayName] — user-facing label shown in the in-app widgets list + the
 *   adaptive name label.
 * - [appearance] — visual chrome + tap behaviour + toggle feedback.
 * - [removed] — soft-delete flag (the "deleted in-app but still placed"
 *   inert-render pattern; see [WidgetKitConfig]).
 *
 * The trailing nullable [type] / [amplitudePercent] / [durationMillis] /
 * [patternId] are **v1-only decode-only carriers**: an old on-disk record still
 * decodes (`ignoreUnknownKeys` plus the matching field names), and
 * [dev.ranzlappen.gadget.feature.vibration.widget.migration.VibrationWidgetMigrator]
 * folds them into [actionKey] + [params] then nulls them. New writes never set
 * them.
 */
@Serializable
@Immutable
data class VibrationWidgetConfig(
    override val displayName: String,
    val actionKey: String = FUNCTION_ONESHOT,
    val params: Map<String, String> = emptyMap(),
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val appearance: WidgetAppearance = WidgetAppearance(),
    override val removed: Boolean = false,
    override val schemaVersion: Int = SCHEMA_VERSION,
    // ─── v1-only, decode-only — folded by the migrator then nulled ──────────
    @Deprecated("v1 only") val type: String? = null,
    @Deprecated("v1 only") val amplitudePercent: Int? = null,
    @Deprecated("v1 only") val durationMillis: Long? = null,
    @Deprecated("v1 only") val patternId: String? = null,
) : WidgetKitConfig {
    companion object {
        /** Current serialized schema version. */
        const val SCHEMA_VERSION: Int = 2

        /** One-tap buzz function id — equals
         *  [dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler.ACTION_ONESHOT]. */
        const val FUNCTION_ONESHOT: String = "oneshot"

        /** Saved-pattern function id — equals
         *  [dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler.ACTION_PATTERN_PLAY]. */
        const val FUNCTION_PATTERN: String = "pattern_play"

        /** Continuous ("perma") vibrate **toggle** function id. Distinct from
         *  its paired on/off action keys (a toggle binds two actions), like
         *  torch's flashlight/strobe toggles. */
        const val FUNCTION_CONTINUOUS: String = "continuous"

        /** Loop-pattern **toggle** function id: first tap plays the saved
         *  pattern in a loop, second tap stops it. */
        const val FUNCTION_PATTERN_TOGGLE: String = "pattern_toggle"

        const val DEFAULT_AMPLITUDE_PERCENT: Int = 60
        const val DEFAULT_DURATION_MS: Long = 300L

        /** UI slider bounds for the one-tap vibrate config. */
        const val MIN_AMPLITUDE_PERCENT: Int = 1
        const val MAX_AMPLITUDE_PERCENT: Int = 100
        const val MIN_DURATION_MS: Long = 10L
        const val MAX_DURATION_MS: Long = 5_000L
    }
}
