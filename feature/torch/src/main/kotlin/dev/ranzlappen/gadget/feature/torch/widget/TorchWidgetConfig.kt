package dev.ranzlappen.gadget.feature.torch.widget

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-instance configuration for a home-screen torch widget.
 *
 * One [TorchWidgetConfig] persists per `appWidgetId` inside the
 * `torch_widgets` Preferences DataStore (see
 * [WidgetConfigStore]). The widget provider reads the
 * config in `onUpdate` / `onReceive` to drive both the RemoteViews
 * presentation and (for strobe widgets) the Intent extras passed to
 * [dev.ranzlappen.gadget.feature.torch.strobe.StrobeService].
 *
 * `@Immutable` so Compose-side consumers (the in-app widget list)
 * skip recompositions when the structural value is unchanged.
 *
 * `@Serializable` so kotlinx.serialization can encode to JSON inside
 * the Preferences DataStore.
 *
 * Fields:
 * - [type] — discriminates flashlight vs strobe variants.
 * - [displayName] — user-facing label shown in the in-app widgets
 *   list. Defaults to a localized string at creation time (handled
 *   by the ViewModel — this data class doesn't reach into resources).
 * - [rateHz] — strobe cadence. Only meaningful when `type == Strobe`;
 *   ignored otherwise. Range 1f..20f in the UI; the service clamps
 *   defensively in case of corrupted on-disk values.
 * - [morseMode] — strobe variant only. When `true`, [StrobeService]
 *   loops [morseText] as Morse (defaulting to "SOS" when the box is
 *   blank) instead of a constant strobe. Persisted under the legacy
 *   JSON key `sosMode` (via [SerialName]) so existing on-disk configs
 *   migrate unchanged.
 * - [appearance] — visual chrome + tap behaviour + toggle feedback.
 *   Defaulted to [WidgetAppearance] so existing on-disk configs
 *   migrate seamlessly: missing `appearance` field decodes as the
 *   default record.
 * - [removed] — set when the user deletes the widget from the in-app
 *   list. A non-host app can't pull a placed widget off the launcher,
 *   so instead of deleting the config (which would let the provider
 *   self-heal it straight back) we flag it `removed`: the in-app list
 *   drops it and the home-screen instance repaints inert until the user
 *   drags it off (which fires `onDeleted` and purges the config).
 */
@Serializable
@Immutable
data class TorchWidgetConfig(
    val type: WidgetType,
    override val displayName: String,
    val rateHz: Float = DEFAULT_RATE_HZ,
    @SerialName("sosMode")
    val morseMode: Boolean = false,
    val morseText: String = "",
    val appearance: WidgetAppearance = WidgetAppearance(),
    override val removed: Boolean = false,
    override val schemaVersion: Int = SCHEMA_VERSION,
) : WidgetKitConfig {
    companion object {
        /** Current serialized schema version. Bump when a field is renamed
         *  or its meaning shifts (additive fields with defaults don't need a
         *  bump — they decode cleanly on old configs). A future
         *  `Migrator<TorchWidgetConfig>` keys off this. */
        const val SCHEMA_VERSION: Int = 1

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
