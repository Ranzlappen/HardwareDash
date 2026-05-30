package dev.ranzlappen.gadget.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single seam for creating + reading + updating Android notification
 * channels across the whole app.
 *
 * Replaces every consumer's hand-rolled
 * `if (mgr.getNotificationChannel(id) == null) mgr.createNotificationChannel(NotificationChannel(...))`
 * dance — each one was 5-15 lines with subtle differences in the
 * version guard, sound suppression, vibration suppression, importance
 * mapping, and lazy-creation pattern. The registry's [ensure] is
 * idempotent so it's safe to call once per consumer at construction
 * time and again on every notification post — system-settings
 * overrides users set survive across both paths.
 *
 * **Pre-API-26 no-op.** Android only exposes notification channels
 * from API 26; below that [ensure] returns immediately. Consumers can
 * call it unconditionally.
 *
 * **Not a notification poster.** Posting itself stays at the call
 * site (each consumer needs its own `NotificationCompat.Builder`
 * construction). The registry only owns channel lifecycle.
 *
 * Consumers inject this `@Singleton` and call [ensure] for each
 * channel they post to. Idiomatic usage:
 *
 * ```kotlin
 * @Singleton
 * class FooNotifier @Inject constructor(
 *     @ApplicationContext private val context: Context,
 *     private val channels: NotificationChannelRegistry,
 * ) {
 *     private val spec = ChannelSpec(
 *         id = "foo_status",
 *         displayName = context.getString(R.string.foo_channel_name),
 *         description = context.getString(R.string.foo_channel_description),
 *     )
 *     fun post(...) {
 *         channels.ensure(spec)
 *         NotificationManagerCompat.from(context).notify(id, build(...))
 *     }
 * }
 * ```
 */
@Singleton
class NotificationChannelRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val systemManager: NotificationManager?
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /**
     * Idempotently ensure a system channel exists for [spec]. No-op
     * pre-API-26. After the first call the channel is in system
     * settings; subsequent calls return immediately without touching
     * the user's overrides.
     */
    fun ensure(spec: ChannelSpec) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = systemManager ?: return
        if (mgr.getNotificationChannel(spec.id) != null) return
        val channel = NotificationChannel(spec.id, spec.displayName, spec.importance.toAndroid()).apply {
            description = spec.description
            if (spec.silent) {
                setSound(null, null)
                enableVibration(false)
            }
        }
        mgr.createNotificationChannel(channel)
    }

    private fun ChannelSpec.Importance.toAndroid(): Int = when (this) {
        ChannelSpec.Importance.Min -> NotificationManager.IMPORTANCE_MIN
        ChannelSpec.Importance.Low -> NotificationManager.IMPORTANCE_LOW
        ChannelSpec.Importance.Default -> NotificationManager.IMPORTANCE_DEFAULT
        ChannelSpec.Importance.High -> NotificationManager.IMPORTANCE_HIGH
    }
}
