package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController

/**
 * Home-screen flashlight widget — 1×1 cell, single ImageButton.
 *
 * Lifecycle: [AppWidgetProvider] is a [android.content.BroadcastReceiver]
 * under the hood — Android instantiates a fresh instance for each
 * broadcast, so there's no per-instance state to keep. We reach
 * the singleton [TorchController] via [EntryPointAccessors] (same
 * recipe as the QS tile).
 *
 * Click flow:
 *   1. User taps the button on the home screen.
 *   2. Launcher fires the [PendingIntent] we attached in [onUpdate],
 *      delivering an [Intent] with action [ACTION_FLASHLIGHT_TOGGLE]
 *      to this receiver.
 *   3. [onReceive] catches the action, runs `controller.toggle()`
 *      on a one-shot `Dispatchers.Main` scope (toggling is a single
 *      Camera2 call — finishes in microseconds, safe to fire and
 *      forget).
 *   4. After toggling, schedule a widget update so the icon can
 *      reflect future state. (We don't visually swap the icon for
 *      on vs off yet; that's a Phase-2 follow-up batch.)
 */
class FlashlightWidgetProvider : AppWidgetProvider() {

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
        if (intent.action != ACTION_FLASHLIGHT_TOGGLE) return
        // Toggle on the singleton — the TorchCallback synchronously
        // updates the shared TorchState so other surfaces (screen +
        // tile) react immediately. toggle() is non-suspend; finishes
        // before this broadcast handler returns.
        EntryPointAccessors
            .fromApplication(context.applicationContext, FlashlightWidgetEntryPoint::class.java)
            .torchController()
            .toggle()
    }

    /**
     * Build the [RemoteViews] for one widget instance. Same view
     * for every instance — the click pending-intent is the only
     * variable (and even that's identical across instances because
     * the action is a singleton broadcast).
     */
    private fun buildRemoteViews(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
            setOnClickPendingIntent(
                R.id.widget_flashlight_button,
                togglePendingIntent(context),
            )
        }

    /**
     * Hilt entry point — gives a system-instantiated BroadcastReceiver
     * access to the singleton [TorchController] without
     * `@AndroidEntryPoint`.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FlashlightWidgetEntryPoint {
        fun torchController(): TorchController
    }

    companion object {
        /**
         * Custom broadcast action this receiver listens for. Declared
         * in the AndroidManifest's intent-filter alongside
         * `APPWIDGET_UPDATE` so the OS routes user taps here.
         */
        const val ACTION_FLASHLIGHT_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_FLASHLIGHT_TOGGLE"

        /**
         * Build the toggle [PendingIntent]. Mutable=false +
         * UpdateCurrent flags so the OS reuses the existing
         * PendingIntent across widget updates rather than creating
         * a new one each refresh.
         */
        fun togglePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, FlashlightWidgetProvider::class.java).apply {
                action = ACTION_FLASHLIGHT_TOGGLE
                component = ComponentName(context, FlashlightWidgetProvider::class.java)
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
