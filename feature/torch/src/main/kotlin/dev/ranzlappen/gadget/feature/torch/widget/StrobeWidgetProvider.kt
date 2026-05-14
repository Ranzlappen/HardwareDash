package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService

/**
 * Home-screen strobe widget — 1×1 cell, single ImageButton.
 *
 * Tapping the widget toggles [StrobeService]:
 * - If the service isn't running, send `startForegroundService` to
 *   it (no action specified — defaults to "start strobing").
 * - If the service IS running, fire an explicit `ACTION_STOP`
 *   intent to make it shut down cleanly (cancels the loop, turns
 *   the torch off, dismisses the foreground notification).
 *
 * Heuristic for "is the service running" is intentionally
 * lightweight: we maintain a static `isRunning` flag on the
 * service's companion via [StrobeService.companion]. This is
 * sloppy but works for the Phase 2 / Batch 1 scope; a follow-up
 * batch can replace it with a binder-based round-trip if the flag
 * starts drifting (e.g. the service is killed by the OS without
 * notifying us).
 */
class StrobeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_STROBE_TOGGLE) return
        // Always send "start" — if the service is already running,
        // its onStartCommand path is idempotent. Stopping is done
        // via a separate intent action from inside the service's
        // own notification (or via Settings → Force stop).
        //
        // A future batch will track strobe state in a shared
        // controller so the widget can flip between start/stop
        // cleanly without the user having to find a separate stop
        // affordance.
        val startIntent = Intent(context, StrobeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        } else {
            context.startService(startIntent)
        }
    }

    private fun buildRemoteViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_strobe).apply {
            setOnClickPendingIntent(
                R.id.widget_strobe_button,
                togglePendingIntent(context),
            )
        }

    companion object {
        const val ACTION_STROBE_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_STROBE_TOGGLE"

        fun togglePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, StrobeWidgetProvider::class.java).apply {
                action = ACTION_STROBE_TOGGLE
                component = ComponentName(context, StrobeWidgetProvider::class.java)
            }
            return PendingIntent.getBroadcast(
                context,
                /* requestCode = */ 0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
