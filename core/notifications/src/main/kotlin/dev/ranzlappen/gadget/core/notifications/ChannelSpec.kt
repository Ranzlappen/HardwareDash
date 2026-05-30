package dev.ranzlappen.gadget.core.notifications

/**
 * Declarative spec for one [android.app.NotificationChannel] every
 * consumer registers with the kit's [NotificationChannelRegistry].
 *
 * The registry handles SDK guards (channels only exist on API 26+),
 * idempotent creation (`getNotificationChannel` short-circuits when
 * the channel already exists so system-settings overrides users set
 * survive), the sound / vibration suppression details, and the
 * `IMPORTANCE_*` mapping. Consumers describe the channel; the
 * registry executes.
 *
 * @property id stable channel id. Must be unique across the app —
 *              convention is feature-prefixed (e.g.
 *              `"monitor_summary"`, `"widget_feedback"`). Once a
 *              channel is created the id is the system-settings
 *              identity; renaming it strands the user's
 *              importance / sound / vibration overrides.
 * @property displayName user-facing channel label shown in system
 *                       settings. Should be a localised string.
 * @property description user-facing channel description shown in
 *                      system settings. Should be a localised string.
 * @property importance Android importance level. Default
 *                      [Importance.Low] — silent, no heads-up. Use
 *                      [Importance.Default] for tap-able status
 *                      updates, [Importance.High] for active-alert
 *                      surfaces.
 * @property silent suppress sound + vibration even when [importance]
 *                  would otherwise allow them. Most kit-driven
 *                  channels (monitor summary, widget feedback) are
 *                  silent.
 */
data class ChannelSpec(
    val id: String,
    val displayName: String,
    val description: String,
    val importance: Importance = Importance.Low,
    val silent: Boolean = true,
) {
    /** Subset of [android.app.NotificationManager.IMPORTANCE_*] the
     *  kit exposes. Maps 1:1 at registration time. */
    enum class Importance { Min, Low, Default, High }
}
