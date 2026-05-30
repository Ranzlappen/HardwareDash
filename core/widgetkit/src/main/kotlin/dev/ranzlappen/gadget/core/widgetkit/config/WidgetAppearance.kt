package dev.ranzlappen.gadget.core.widgetkit.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-instance appearance + interaction config shared by every kit-built
 * widget (Flashlight, Strobe, future feature kinds).
 *
 * Lives on each widget's persisted config as a structural sub-record so
 * customisation flows uniformly across widget types and so future widget
 * kinds can opt into the same surface without re-inventing the fields.
 * Stored as JSON via the existing `FeaturePreferences<T>` pipeline —
 * defaults on each field keep on-disk configs migrable (older serialised
 * configs missing the new block decode as `WidgetAppearance()`).
 *
 * Fields:
 * - [background] — chrome behind the icon. `GlassSurface` (default),
 *   `Solid` (uses [solidColor]), or `Transparent` (icon-only).
 * - [solidColor] — ARGB packed into a [Long]; used iff
 *   `background == Solid`. Stored as Long for kotlinx.serialization
 *   compatibility (the JSON encoder doesn't handle `androidx.compose.ui
 *   .graphics.Color` natively).
 * - [iconStyle] — active / inactive icon resource keys + tint.
 * - [tap] — tap behaviour (animation + whether taps are honoured).
 * - [feedback] — optional toggle confirmation (toast or notification).
 */
@Serializable
data class WidgetAppearance(
    val background: BackgroundMode = BackgroundMode.GlassSurface,
    val solidColor: Long = DEFAULT_SOLID_COLOR_ARGB,
    val iconStyle: IconStyle = IconStyle(),
    val tap: TapBehavior = TapBehavior(),
    val feedback: ToggleFeedback = ToggleFeedback.None,
) {
    companion object {
        /** Neutral dark grey — sensible default for the Solid mode
         *  before the user picks anything. */
        const val DEFAULT_SOLID_COLOR_ARGB: Long = 0xFF1F1F1FL
    }
}

/**
 * Background chrome for a widget instance.
 *
 * - [GlassSurface] — the design system's standard glassmorphic tile.
 *   Matches the in-app `DashCard` aesthetic.
 * - [Solid] — flat colour fill (uses [WidgetAppearance.solidColor]).
 * - [Transparent] — no background; the icon floats over the home-
 *   screen wallpaper.
 */
@Serializable
enum class BackgroundMode { GlassSurface, Solid, Transparent }

/**
 * Icon resource keys + tint mode.
 *
 * Keys reference entries in the feature's icon catalog rather than raw
 * drawable resource IDs so persisted configs survive icon-resource
 * renames and export to plain JSON cleanly.
 */
@Serializable
data class IconStyle(
    val activeKey: String = WidgetIconKeys.DEFAULT_ACTIVE,
    val inactiveKey: String = WidgetIconKeys.DEFAULT_INACTIVE,
    val tint: IconTint = IconTint.ThemeAccent,
    val customTintArgb: Long = 0xFFFFFFFFL,
)

/**
 * Tint mode for the icon.
 *
 * `ThemeAccent` / `ThemeOnSurface` pull from the active GadgetTheme via
 * the design-system Provider. `Monochrome*` pin to a fixed value (safe
 * across themes). `Custom` reads [IconStyle.customTintArgb].
 */
@Serializable
enum class IconTint { ThemeAccent, ThemeOnSurface, MonochromeWhite, MonochromeBlack, Custom }

/**
 * What happens visually when the user taps the widget.
 *
 * - [animation] — visual feedback variant.
 * - [enabled] — when `false`, the widget renders display-only and no
 *   PendingIntent is attached to the tap target. Useful for indicator
 *   variants of widgets that should mirror state but not react.
 */
@Serializable
data class TapBehavior(
    val animation: TapAnimation = TapAnimation.Ripple,
    val enabled: Boolean = true,
)

/**
 * Tap-feedback animation variant.
 *
 * AppWidget RemoteViews limit what's actually achievable here:
 * - [None] — no visual feedback at all.
 * - [Ripple] — the launcher's stock material ripple (whatever the
 *   home launcher provides; we don't override).
 * - [Pulse] / [Scale] / [Flash] — brief drawable / colour swap
 *   scheduled via Handler.postDelayed and reverted ~150 ms later.
 *   Approximations rather than smooth Compose-style animations.
 */
@Serializable
enum class TapAnimation { None, Ripple, Pulse, Scale, Flash }

/**
 * Optional confirmation surface fired when the widget's tap-handler runs.
 *
 * The placeholder grammar inside [Toast.template] / [Notification
 * .titleTemplate] / [Notification.bodyTemplate]:
 *  - `{name}` — the widget's display name.
 *  - `{state}` — `"on"` or `"off"` after the toggle.
 *
 * Sealed so future feedback kinds (Vibration, Sound, Voice) can land
 * additively without breaking existing configs.
 *
 * **Wire-format pin.** kotlinx.serialization writes a polymorphic
 * discriminator derived from each subtype's serial name (default: the
 * class's qualified name). The previous package was
 * `dev.ranzlappen.gadget.feature.torch.widget.customization`; this type
 * moved to `dev.ranzlappen.gadget.core.widgetkit.config` in
 * refactor-2026 Phase 2 / C1. The `@SerialName` annotations below **pin
 * the wire string to the legacy FQN** so every user's persisted widget
 * config keeps decoding after the upgrade. A future schema-bump
 * migrator (planned for C4's `Migrator<T>` seam) can rewrite the
 * discriminator to the new package and let us drop these pins.
 */
@Serializable
sealed class ToggleFeedback {
    /** No confirmation surface. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.None")
    object None : ToggleFeedback()

    /** Brief toast on the home-screen. Best for non-intrusive
     *  confirmations of binary toggles. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.Toast")
    data class Toast(val template: String) : ToggleFeedback()

    /** Posted notification on the "Widget feedback" channel. Auto-
     *  cancels after 3 s via [android.app.Notification.Builder
     *  .setTimeoutAfter]. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.feature.torch.widget.customization.ToggleFeedback.Notification")
    data class Notification(
        val titleTemplate: String,
        val bodyTemplate: String,
    ) : ToggleFeedback()
}
