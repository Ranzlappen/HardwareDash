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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen flashlight widget — 1×1 cell, single ImageButton.
 *
 * Lifecycle: [AppWidgetProvider] is a [android.content.BroadcastReceiver]
 * under the hood — Android instantiates a fresh instance for each
 * broadcast, so there's no per-instance state to keep. We reach the
 * singleton [TorchController] + [TorchWidgetConfigRepository] via
 * [EntryPointAccessors] (same recipe as the QS tile).
 *
 * Click flow:
 *   1. User taps the button on the home screen.
 *   2. Launcher fires the [PendingIntent] we attached in [onUpdate],
 *      delivering an [Intent] with action [ACTION_FLASHLIGHT_TOGGLE]
 *      to this receiver.
 *   3. [onReceive] catches the action, runs `controller.toggle()` —
 *      a single Camera2 call, microsecond-fast.
 *   4. We then schedule an immediate self-update so the icon flips
 *      to reflect the new state.
 *
 * Per-instance config: each widget owns a [TorchWidgetConfig] entry
 * in [TorchWidgetConfigRepository] keyed by its `appWidgetId`. The
 * Flashlight variant doesn't currently surface configurable fields
 * in the in-app UI, but the load path is in place so future config-
 * driven Flashlight features (auto-off timer, brightness — issue
 * https://github.com/Ranzlappen/HardwareDash/issues/95) land without
 * disturbing the persistence shape.
 *
 * [onDeleted] purges the corresponding config so dragging the widget
 * off the home screen doesn't leak a record into the repository.
 */
class FlashlightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val controller = entry(context).torchController()
        val isOn = controller.state.value.isOn
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, isOn))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_FLASHLIGHT_TOGGLE) return
        // Toggle on the singleton — the TorchCallback synchronously
        // updates the shared TorchState so other surfaces (screen +
        // tile) react immediately. toggle() is non-suspend; finishes
        // before this broadcast handler returns.
        val controller = entry(context).torchController()
        controller.toggle()
        // Refresh every flashlight widget instance so the icon
        // flips immediately. Cheap — RemoteViews binding is the
        // only IPC.
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, FlashlightWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) onUpdate(context, appWidgetManager, ids)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Async purge — runs after onReceive returns so the OS's
        // 10-second receiver budget isn't a concern.
        val repo = entry(context).widgetRepository()
        val purgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        purgeScope.launch {
            appWidgetIds.forEach { repo.delete(it) }
        }
    }

    /**
     * Build the [RemoteViews] for one widget instance. The state-
     * sensitive bit is the icon resource: `ic_flashlight_on` when
     * the torch is currently on, `ic_flashlight_off` otherwise.
     */
    private fun buildRemoteViews(context: Context, torchOn: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
            setImageViewResource(
                R.id.widget_flashlight_button,
                if (torchOn) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off,
            )
            setOnClickPendingIntent(
                R.id.widget_flashlight_button,
                togglePendingIntent(context),
            )
        }

    private fun entry(context: Context): FlashlightWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FlashlightWidgetEntryPoint::class.java,
        )

    /**
     * Hilt entry point — gives a system-instantiated BroadcastReceiver
     * access to the singletons without `@AndroidEntryPoint`.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FlashlightWidgetEntryPoint {
        fun torchController(): TorchController
        fun widgetRepository(): TorchWidgetConfigRepository
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
