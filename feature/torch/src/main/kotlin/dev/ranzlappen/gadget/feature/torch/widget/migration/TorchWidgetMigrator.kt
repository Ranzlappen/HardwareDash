package dev.ranzlappen.gadget.feature.torch.widget.migration

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.store.Migrator
import dev.ranzlappen.gadget.feature.torch.automation.TorchActionHandler
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig

/**
 * Upgrades a legacy **v1** [TorchWidgetConfig] to the function-driven **v2**
 * shape on read.
 *
 * v1 discriminated flashlight vs strobe vs Morse via the `type`
 * (`"Flashlight"` / `"Strobe"`) + `sosMode` (Morse) + `rateHz` / `morseText`
 * fields. v2 collapses that into a single [TorchWidgetConfig.actionKey] (the
 * bound widget-function id) plus a generic [TorchWidgetConfig.params] map keyed
 * by the action's param names — so the generic provider can dispatch any
 * function without a `when (type)` switch.
 *
 * Mapping:
 * - `type == "Strobe"` + `sosMode == true` → [TorchWidgetConfig.FUNCTION_MORSE]
 *   (momentary), params `{ text, rate_hz }` (text defaults to `"SOS"` when the
 *   legacy box was blank).
 * - `type == "Strobe"` (no SOS) → [TorchWidgetConfig.FUNCTION_STROBE] (toggle),
 *   params `{ rate_hz }`.
 * - `type == "Flashlight"` (or any unknown / null) →
 *   [TorchWidgetConfig.FUNCTION_FLASHLIGHT] (toggle), no params.
 *
 * `appearance` / `displayName` / `removed` survive untouched (only the
 * function-routing fields change), and the legacy decode-only fields are nulled
 * so the migrated record encodes cleanly as pure v2.
 *
 * Bound from [dev.ranzlappen.gadget.feature.torch.di.TorchProvidesModule] as
 * the [TorchWidgetConfig] store's [Migrator]; the store calls [migrate] on
 * every read so each user's configs upgrade lazily as they come into scope.
 */
class TorchWidgetMigrator : Migrator<TorchWidgetConfig> {
    @Suppress("DEPRECATION")
    override fun migrate(stored: TorchWidgetConfig): TorchWidgetConfig {
        if (stored.schemaVersion >= TorchWidgetConfig.SCHEMA_VERSION) return stored

        val rate = (stored.rateHz ?: TorchWidgetConfig.DEFAULT_RATE_HZ).toString()
        val (actionKey, params) = when (stored.type) {
            "Strobe" -> if (stored.morseMode == true) {
                TorchWidgetConfig.FUNCTION_MORSE to mapOf(
                    TorchActionHandler.PARAM_TEXT to
                        (stored.morseText?.takeIf { it.isNotBlank() } ?: "SOS"),
                    TorchActionHandler.PARAM_RATE_HZ to rate,
                )
            } else {
                TorchWidgetConfig.FUNCTION_STROBE to
                    mapOf(TorchActionHandler.PARAM_RATE_HZ to rate)
            }
            // "Flashlight" or any unknown / null type → the plain power toggle.
            else -> TorchWidgetConfig.FUNCTION_FLASHLIGHT to emptyMap()
        }

        return stored.copy(
            actionKey = actionKey,
            params = params,
            sizePreset = WidgetSizePreset.Medium,
            schemaVersion = TorchWidgetConfig.SCHEMA_VERSION,
            type = null,
            rateHz = null,
            morseMode = null,
            morseText = null,
        )
    }
}
