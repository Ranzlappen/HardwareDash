package dev.ranzlappen.gadget.feature.torch.widget

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-instance configuration for a home-screen torch widget — **schema v2**,
 * the function-driven shape every kit widget now persists.
 *
 * One [TorchWidgetConfig] persists per `appWidgetId` inside the
 * `torch_widgets` Preferences DataStore (see
 * [dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore]). The widget
 * provider reads it in `onUpdate` / the tap path to drive both the RemoteViews
 * presentation and the `:core:automation` action the tap dispatches.
 *
 * `@Immutable` so Compose-side consumers (the in-app widget list) skip
 * recompositions when the structural value is unchanged. `@Serializable` so
 * kotlinx.serialization can encode it to JSON inside the Preferences DataStore.
 *
 * **v2 canonical fields:**
 * - [displayName] — user-facing label, shown in the in-app list **and** painted
 *   under the icon at expanded widget sizes ([dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity.showLabel]).
 * - [actionKey] — the bound [dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction.id]
 *   (one of [FUNCTION_FLASHLIGHT] / [FUNCTION_STROBE] / [FUNCTION_MORSE]). The
 *   provider resolves this against [dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetFunctionCatalog]
 *   and dispatches the matched function through the kit's function dispatcher.
 * - [params] — the function's persisted param values (e.g. `rate_hz`, `text`),
 *   passed verbatim to the automation dispatch.
 * - [sizePreset] — the user's chosen starting size (a render-density hint).
 * - [appearance] — visual chrome + tap behaviour + toggle feedback.
 * - [removed] — set when the user deletes the widget from the in-app list. A
 *   non-host app can't pull a placed widget off the launcher, so instead of
 *   deleting the config (which would let the provider self-heal it straight
 *   back) we flag it `removed`: the in-app list drops it and the home-screen
 *   instance repaints inert until the user drags it off (which fires
 *   `onDeleted` and purges the config).
 * - [schemaVersion] — bumped to [SCHEMA_VERSION] (2). [TorchWidgetMigrator]
 *   folds the legacy v1 fields below into the v2 shape on read.
 *
 * **Legacy v1, decode-only fields.** Older on-disk configs carried a `type`
 * discriminator + `rateHz` / `sosMode` / `morseText`. They survive on this
 * class as nullable `@Deprecated` properties so the lenient
 * (`ignoreUnknownKeys` + `encodeDefaults`) JSON decoder doesn't drop them
 * before [TorchWidgetMigrator] can read them. The migrator folds them into
 * [actionKey] + [params] and nulls them out. **Never read them directly** —
 * always go through the migrated config the store hands back.
 */
@Serializable
@Immutable
data class TorchWidgetConfig(
    override val displayName: String,
    val actionKey: String = FUNCTION_FLASHLIGHT,
    val params: Map<String, String> = emptyMap(),
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val appearance: WidgetAppearance = WidgetAppearance(),
    override val removed: Boolean = false,
    override val schemaVersion: Int = SCHEMA_VERSION,
    // ── v1-only, decode-only; folded by TorchWidgetMigrator then nulled. Do not read. ──
    @Deprecated("v1 only — folded into actionKey/params by TorchWidgetMigrator")
    val type: String? = null,
    @Deprecated("v1 only — folded into params[rate_hz] by TorchWidgetMigrator")
    val rateHz: Float? = null,
    @SerialName("sosMode")
    @Deprecated("v1 only — folded into actionKey by TorchWidgetMigrator")
    val morseMode: Boolean? = null,
    @Deprecated("v1 only — folded into params[text] by TorchWidgetMigrator")
    val morseText: String? = null,
) : WidgetKitConfig {
    companion object {
        /** Current serialized schema version. v2 is the function-driven shape
         *  (`actionKey` + `params` replacing `type` + per-strobe fields).
         *  [TorchWidgetMigrator] upgrades v1 records on read. */
        const val SCHEMA_VERSION: Int = 2

        /** Toggle function id for the binary on/off flashlight. */
        const val FUNCTION_FLASHLIGHT: String = "torch_power"

        /** Toggle function id for the constant strobe. */
        const val FUNCTION_STROBE: String = "strobe"

        /** Momentary function id for Morse / SOS playback. */
        const val FUNCTION_MORSE: String = "morse"

        /** Initial rate for new strobe widgets when the user hasn't
         *  touched the TorchScreen rate slider yet. */
        const val DEFAULT_RATE_HZ: Float = 5f

        /** Minimum strobe rate the UI exposes via the slider. */
        const val MIN_RATE_HZ: Float = 1f

        /** Maximum strobe rate the UI exposes via the slider. Beyond
         *  ~20 Hz Camera2's setTorchMode is rate-limited on most
         *  OEMs and the flash visibly stalls. */
        const val MAX_RATE_HZ: Float = 20f
    }
}
