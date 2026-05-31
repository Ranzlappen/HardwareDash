package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.torch.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repaints placed [FlashlightWidgetProvider] instances when the torch turns
 * on/off **from outside the widget** — the system QS tile, another app's
 * `setTorchMode`, or an OEM power-button gesture. Those paths never deliver a
 * broadcast to our provider, so without this observer a placed flashlight
 * widget would show a stale icon until the next tap or framework update.
 *
 * [StandardTorchController] already folds every OS-level torch change into its
 * `CameraManager.TorchCallback`-backed [TorchController.state]; this observer
 * just watches that flow and fires an `ACTION_APPWIDGET_UPDATE` self-broadcast
 * (the same repaint path [dev.ranzlappen.gadget.feature.torch.monitor.TorchMonitorWidgetNotifier]
 * uses) when `isOn` actually flips.
 *
 * Only the **flashlight** widget reflects torch on/off; the strobe widget
 * reflects [dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime] (already
 * live), so it is intentionally not repainted here.
 *
 * **Cost.** One process-lifetime flow collection, started lazily the first
 * time a widget broadcast reaches the provider ([ensureStarted], idempotent)
 * and never before. `distinctUntilChanged().drop(1)` means it does work only
 * on a genuine external state flip — not on startup, not on no-op emissions —
 * and the repaint early-returns when no widget is placed. So an idle or
 * widget-less app incurs effectively zero overhead.
 */
@Singleton
class TorchWidgetStateObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: TorchController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)

    /** Begin observing on first call; later calls are no-ops. Safe to call
     *  from every widget broadcast — it re-arms after a process restart. */
    fun ensureStarted() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            controller.state
                .map { it.isOn }
                .distinctUntilChanged()
                .drop(1) // skip the current state — onUpdate already painted it
                .collect { repaintFlashlightWidgets() }
        }
    }

    private fun repaintFlashlightWidgets() {
        val component = ComponentName(context, FlashlightWidgetProvider::class.java)
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, FlashlightWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                this.component = component
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            },
        )
    }
}
