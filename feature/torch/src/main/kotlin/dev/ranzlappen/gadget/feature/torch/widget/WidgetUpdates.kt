package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.ranzlappen.gadget.feature.torch.widget.customization.TapAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a tap-press frame is held before reverting to the resting
 *  render. ~150 ms reads as a deliberate press without feeling sluggish. */
internal const val PRESS_FRAME_MILLIS: Long = 150L

/** True for the tap animations rendered as a held "pressed" frame (as
 *  opposed to [TapAnimation.None] / the passive [TapAnimation.Ripple]). */
internal fun TapAnimation.hasPressFrame(): Boolean =
    this == TapAnimation.Flash || this == TapAnimation.Scale || this == TapAnimation.Pulse

/**
 * Render a held "pressed" frame on a single widget instance, then revert
 * to the resting frame after [PRESS_FRAME_MILLIS].
 *
 * Call from an [AppWidgetProvider]'s `onReceive` (an [AppWidgetProvider]
 * is a [BroadcastReceiver]): [goAsync] keeps the receiver process alive
 * across the delay so the revert lands even after `onReceive` returns.
 */
internal fun BroadcastReceiver.playTapPressFrame(
    context: Context,
    appWidgetId: Int,
    pressedViews: RemoteViews,
    restingViews: RemoteViews,
) {
    val manager = AppWidgetManager.getInstance(context)
    manager.updateAppWidget(appWidgetId, pressedViews)
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            delay(PRESS_FRAME_MILLIS)
            manager.updateAppWidget(appWidgetId, restingViews)
        } finally {
            pendingResult.finish()
        }
    }
}

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
