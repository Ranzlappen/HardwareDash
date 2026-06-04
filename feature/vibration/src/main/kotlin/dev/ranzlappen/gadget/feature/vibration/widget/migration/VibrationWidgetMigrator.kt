package dev.ranzlappen.gadget.feature.vibration.widget.migration

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.store.Migrator
import dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig

/**
 * Upgrades a v1 [VibrationWidgetConfig] (the legacy `type` + `amplitudePercent`
 * / `durationMillis` / `patternId` shape) to v2 (the function-driven
 * `actionKey` + `params` shape).
 *
 * v1 records still decode because the v2 data class keeps the legacy field
 * names as nullable decode-only carriers (plus the store's `ignoreUnknownKeys`).
 * This migrator reads those carriers, derives the equivalent function +
 * params, nulls the carriers, and stamps [VibrationWidgetConfig.SCHEMA_VERSION].
 * It only acts when `schemaVersion < 2`, so v2 reads pass through untouched.
 *
 * Mapping:
 * - `type == "Pattern"` → `actionKey = pattern_play`, `params = {pattern_id}`.
 * - everything else (`"Vibrate"` / null / unknown) → `actionKey = oneshot`,
 *   `params = {amplitude, duration_ms}` (defaults filled when the v1 fields
 *   were absent).
 *
 * Appearance is preserved verbatim; size defaults to [WidgetSizePreset.Medium]
 * (v1 carried no size).
 */
class VibrationWidgetMigrator : Migrator<VibrationWidgetConfig> {

    @Suppress("DEPRECATION")
    override fun migrate(stored: VibrationWidgetConfig): VibrationWidgetConfig {
        if (stored.schemaVersion >= VibrationWidgetConfig.SCHEMA_VERSION) return stored

        val isPattern = stored.type == LEGACY_TYPE_PATTERN
        val actionKey: String
        val params: Map<String, String>
        if (isPattern) {
            actionKey = VibrationWidgetConfig.FUNCTION_PATTERN
            params = mapOf(
                VibrationActionHandler.PARAM_PATTERN_ID to stored.patternId.orEmpty(),
            )
        } else {
            actionKey = VibrationWidgetConfig.FUNCTION_ONESHOT
            val amplitude = stored.amplitudePercent ?: VibrationWidgetConfig.DEFAULT_AMPLITUDE_PERCENT
            val duration = stored.durationMillis ?: VibrationWidgetConfig.DEFAULT_DURATION_MS
            params = mapOf(
                VibrationActionHandler.PARAM_AMPLITUDE to amplitude.toString(),
                VibrationActionHandler.PARAM_DURATION_MS to duration.toString(),
            )
        }

        return VibrationWidgetConfig(
            displayName = stored.displayName,
            actionKey = actionKey,
            params = params,
            sizePreset = WidgetSizePreset.Medium,
            appearance = stored.appearance,
            removed = stored.removed,
            schemaVersion = VibrationWidgetConfig.SCHEMA_VERSION,
            type = null,
            amplitudePercent = null,
            durationMillis = null,
            patternId = null,
        )
    }

    private companion object {
        /** The v1 `WidgetType` enum's `Pattern` serialized name. */
        const val LEGACY_TYPE_PATTERN = "Pattern"
    }
}
