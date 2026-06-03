package dev.ranzlappen.gadget.feature.torch.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.pin.BaseWidgetPinSuccessReceiver
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore

/**
 * Receives the success callback from
 * [android.appwidget.AppWidgetManager.requestPinAppWidget] for torch
 * widgets. Thin subclass of the kit's [BaseWidgetPinSuccessReceiver]
 * — the kit owns the full claim → save → post-save flow + `goAsync`
 * lifecycle; this class only plugs in the torch-specific intent
 * action / extra-key constants, the Hilt accessors for torch's kit
 * instances, and the torch-specific post-save action
 * ([broadcastTorchWidgetUpdate] so the new widget repaints with its
 * config immediately).
 *
 * Registered in `:feature:torch`'s AndroidManifest with
 * `exported="true"` because the OS fires the success callback from
 * the system process (outside ours). The intent's explicit
 * ComponentName routes it directly here — no cross-package resolution
 * needed.
 *
 * Every step logs under [TorchPinLog.TAG] — `adb logcat -s
 * TorchPinFlow:D` traces enqueue → callback → claim → save →
 * broadcastUpdate end-to-end.
 */
class WidgetPinSuccessReceiver : BaseWidgetPinSuccessReceiver<TorchWidgetConfig>() {

    override val expectedAction: String = TorchWidgetCreator.ACTION_WIDGET_PIN_SUCCESS

    override val tokenExtraKey: String = TorchWidgetCreator.EXTRA_PENDING_CONFIG_TOKEN

    override val logTag: String = TorchPinLog.TAG

    override fun pendingConfigs(context: Context): PendingWidgetConfigs<TorchWidgetConfig> =
        entry(context).pendingConfigs()

    override fun configStore(context: Context): WidgetConfigStore<TorchWidgetConfig> =
        entry(context).configStore()

    override suspend fun afterSave(
        context: Context,
        appWidgetId: Int,
        config: TorchWidgetConfig,
    ) {
        broadcastTorchWidgetUpdate(context, appWidgetId)
    }

    private fun entry(context: Context): TorchPinEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            TorchPinEntryPoint::class.java,
        )

    /** Hilt entry point — gives the system-instantiated
     *  BroadcastReceiver access to the kit-bound torch instances
     *  without needing `@AndroidEntryPoint`. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TorchPinEntryPoint {
        fun pendingConfigs(): PendingWidgetConfigs<TorchWidgetConfig>
        fun configStore(): WidgetConfigStore<TorchWidgetConfig>
    }
}
