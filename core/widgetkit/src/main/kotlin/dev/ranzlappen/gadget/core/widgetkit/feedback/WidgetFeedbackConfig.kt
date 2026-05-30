package dev.ranzlappen.gadget.core.widgetkit.feedback

import androidx.annotation.DrawableRes

/**
 * Per-feature configuration for the kit's [WidgetFeedbackDispatcher].
 *
 * Each widget-bearing feature provides one of these from its Hilt
 * module so the kit-side dispatcher knows which notification channel
 * to lazily create, which small icon to draw on the posted
 * notification, and which integer range to scope hashed notification
 * IDs into.
 *
 * Why per-feature rather than kit-global: features get user-facing
 * channel names in their own strings.xml, and a feature's icon is
 * already on its drawable path. Scoping the notification-id base
 * prevents two features' hashed-template IDs from colliding inside
 * `NotificationManagerCompat.notify`.
 *
 * **Multibinding note.** As more widget-bearing features ship, this
 * pattern will move to a `Map<FeatureId, WidgetFeedbackConfig>`
 * multibinding so a single dispatcher instance serves every feature.
 * Torch is the first consumer; the type stays a simple per-feature
 * provide until the second feature lands and the multibinding
 * collision becomes real.
 */
data class WidgetFeedbackConfig(
    /** Stable notification channel ID. Must be feature-prefixed
     *  (e.g. `"torch_widget_feedback"`) to avoid colliding with
     *  channels other features create. */
    val channelId: String,

    /** Localised channel display name shown in system Settings. */
    val channelName: String,

    /** Localised channel description shown in system Settings. */
    val channelDescription: String,

    /** Drawable shown as the notification's small icon. */
    @DrawableRes
    val smallIcon: Int,

    /** High bytes of the hashed notification ID — keeps each feature's
     *  hashed IDs in a disjoint integer range so two features can't
     *  collide on `NotificationManager.notify`. */
    val notificationIdBase: Int,
)
