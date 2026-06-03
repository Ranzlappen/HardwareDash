package dev.ranzlappen.gadget.feature.vibration.widget

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.pin.BaseWidgetPinSuccessReceiver
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore

/**
 * Receives the success callback from [android.appwidget.AppWidgetManager.requestPinAppWidget]
 * for vibration widgets. Thin subclass of the kit's [BaseWidgetPinSuccessReceiver]
 * — the kit owns the claim → save → post-save flow; this plugs in the
 * vibration-specific action / extra-key, the Hilt accessors, and the post-save
 * repaint broadcast. Mirror of torch's `WidgetPinSuccessReceiver`.
 */
class WidgetPinSuccessReceiver : BaseWidgetPinSuccessReceiver<VibrationWidgetConfig>() {

    override val expectedAction: String = VibrationWidgetCreator.ACTION_WIDGET_PIN_SUCCESS
    override val tokenExtraKey: String = VibrationWidgetCreator.EXTRA_PENDING_CONFIG_TOKEN
    override val logTag: String = VibrationPinLog.TAG

    override fun pendingConfigs(context: Context): PendingWidgetConfigs<VibrationWidgetConfig> =
        entry(context).vibrationPendingConfigs()

    override fun configStore(context: Context): WidgetConfigStore<VibrationWidgetConfig> =
        entry(context).vibrationConfigStore()

    override suspend fun afterSave(context: Context, appWidgetId: Int, config: VibrationWidgetConfig) {
        broadcastVibrationWidgetUpdate(context, appWidgetId)
    }

    private fun entry(context: Context): VibrationPinEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, VibrationPinEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VibrationPinEntryPoint {
        fun vibrationPendingConfigs(): PendingWidgetConfigs<VibrationWidgetConfig>
        fun vibrationConfigStore(): WidgetConfigStore<VibrationWidgetConfig>
    }
}
