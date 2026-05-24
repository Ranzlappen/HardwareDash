package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Fire an [AppWidgetManager.ACTION_APPWIDGET_UPDATE] broadcast at the provider
 * backing [type], scoped to a single [appWidgetId].
 *
 * The broadcast re-enters the provider's `onUpdate`, which re-reads the
 * persisted [TorchWidgetConfig] and rebuilds the RemoteViews. Both the
 * fresh-pin path ([WidgetPinSuccessReceiver]) and the in-app edit path
 * ([dev.ranzlappen.gadget.feature.torch.TorchViewModel.onSheetConfirmed]) call
 * this so a customisation change repaints the placed widget immediately
 * instead of waiting for the next tap.
 */
internal fun broadcastTorchWidgetUpdate(
    context: Context,
    type: WidgetType,
    appWidgetId: Int,
) {
    val providerClass = when (type) {
        WidgetType.Flashlight -> FlashlightWidgetProvider::class.java
        WidgetType.Strobe -> StrobeWidgetProvider::class.java
    }
    val intent = Intent(
        AppWidgetManager.ACTION_APPWIDGET_UPDATE,
        null,
        context,
        providerClass,
    ).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        component = ComponentName(context, providerClass)
    }
    context.sendBroadcast(intent)
}
