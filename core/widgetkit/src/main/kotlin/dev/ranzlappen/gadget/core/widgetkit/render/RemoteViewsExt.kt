package dev.ranzlappen.gadget.core.widgetkit.render

import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import kotlinx.coroutines.delay

/** How long a tap-press frame is held before reverting to the resting
 *  render. Long enough that the launcher applies it as a distinct frame
 *  rather than coalescing it with the resting render. */
const val PRESS_FRAME_MILLIS: Long = 280L

/** True for the tap animations rendered as a held "pressed" frame (as
 *  opposed to [TapAnimation.None] / the passive [TapAnimation.Ripple]). */
fun TapAnimation.hasPressFrame(): Boolean =
    this == TapAnimation.Flash || this == TapAnimation.Scale || this == TapAnimation.Pulse

/**
 * Render a held "pressed" frame on a single widget instance, then revert
 * to the resting frame after [PRESS_FRAME_MILLIS].
 *
 * Suspend — call from the provider's existing `onReceive` coroutine
 * (kept alive by the caller's `goAsync`), so it does not open its own.
 */
suspend fun playTapPressFrame(
    manager: AppWidgetManager,
    appWidgetId: Int,
    pressedViews: RemoteViews,
    restingViews: RemoteViews,
) {
    manager.updateAppWidget(appWidgetId, pressedViews)
    delay(PRESS_FRAME_MILLIS)
    manager.updateAppWidget(appWidgetId, restingViews)
}
