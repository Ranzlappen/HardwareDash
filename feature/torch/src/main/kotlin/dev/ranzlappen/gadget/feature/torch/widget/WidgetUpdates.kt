package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.ranzlappen.gadget.feature.torch.widget.customization.TapAnimation
import kotlinx.coroutines.delay

/** How long a tap-press frame is held before reverting to the resting
 *  render. Long enough that the launcher applies it as a distinct frame
 *  rather than coalescing it with the resting render. */
internal const val PRESS_FRAME_MILLIS: Long = 280L

/** Icon alpha (0..255) for a widget the user deleted in-app but that the
 *  launcher still hosts. Dim enough to read as defunct without vanishing
 *  entirely (the app can't remove it from a third-party launcher). */
internal const val REMOVED_WIDGET_ICON_ALPHA: Int = 70

/** True for the tap animations rendered as a held "pressed" frame (as
 *  opposed to [TapAnimation.None] / the passive [TapAnimation.Ripple]). */
internal fun TapAnimation.hasPressFrame(): Boolean =
    this == TapAnimation.Flash || this == TapAnimation.Scale || this == TapAnimation.Pulse

/**
 * Render a held "pressed" frame on a single widget instance, then revert
 * to the resting frame after [PRESS_FRAME_MILLIS].
 *
 * Suspend — call from the provider's existing `onReceive` coroutine
 * (kept alive by the caller's `goAsync`), so it does not open its own.
 */
internal suspend fun playTapPressFrame(
    manager: AppWidgetManager,
    appWidgetId: Int,
    pressedViews: RemoteViews,
    restingViews: RemoteViews,
) {
    manager.updateAppWidget(appWidgetId, pressedViews)
    delay(PRESS_FRAME_MILLIS)
    manager.updateAppWidget(appWidgetId, restingViews)
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
