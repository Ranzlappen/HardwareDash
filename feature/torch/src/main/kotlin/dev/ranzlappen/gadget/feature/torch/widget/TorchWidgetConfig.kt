package dev.ranzlappen.gadget.feature.torch.widget

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetAppearance
import kotlinx.serialization.Serializable

/**
 * Per-instance configuration for a home-screen torch widget.
 *
 * One [TorchWidgetConfig] persists per `appWidgetId` inside the
 * `torch_widgets` Preferences DataStore (see
 * [TorchWidgetConfigRepository]). The widget provider reads the
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
 * - [sosMode] — strobe variant only. When `true`, [StrobeService]
 *   emits the Morse SOS pattern instead of a constant strobe.
 * - [appearance] — visual chrome + tap behaviour + toggle feedback.
 *   Defaulted to [WidgetAppearance] so existing on-disk configs
 *   migrate seamlessly: missing `appearance` field decodes as the
 *   default record.
 */
@Serializable
@Immutable
data class TorchWidgetConfig(
    val type: WidgetType,
    val displayName: String,
    val rateHz: Float = DEFAULT_RATE_HZ,
    val sosMode: Boolean = false,
    val appearance: WidgetAppearance = WidgetAppearance(),
) {
    companion object {
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
