package dev.ranzlappen.gadget.feature.torch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Icon alpha (0..255) for a widget the user deleted in-app but that the
 *  launcher still hosts. Dim enough to read as defunct without vanishing
 *  entirely (the app can't remove it from a third-party launcher).
 *
 *  Stays torch-side: it's part of the per-feature "deleted-but-still-
 *  placed" inert-rendering pattern the providers own. */
internal const val REMOVED_WIDGET_ICON_ALPHA: Int = 70

/**
 * Fire an [AppWidgetManager.ACTION_APPWIDGET_UPDATE] broadcast at the provider
 * actually hosting [appWidgetId], scoped to that single instance.
 *
 * The provider is resolved from the placed widget's
 * [AppWidgetManager.getAppWidgetInfo] so the broadcast reaches whichever
 * receiver hosts it — [FlashlightWidgetProvider] for every new widget, or the
 * legacy [StrobeWidgetProvider] for an already-placed strobe instance. When the
 * info isn't available yet (a freshly-pinned id the manager hasn't surfaced),
 * it falls back to the designated new-pin provider.
 *
 * The broadcast re-enters the provider's `onUpdate`, which re-reads the
 * persisted [TorchWidgetConfig] and rebuilds the RemoteViews. Both the
 * fresh-pin path ([WidgetPinSuccessReceiver]) and the in-app edit / delete
 * paths ([dev.ranzlappen.gadget.feature.torch.TorchViewModel]) call this so a
 * customisation change repaints the placed widget immediately instead of
 * waiting for the next tap.
 */
internal fun broadcastTorchWidgetUpdate(
    context: Context,
    appWidgetId: Int,
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val providerComponent = appWidgetManager.getAppWidgetInfo(appWidgetId)?.provider
        ?: ComponentName(context, FlashlightWidgetProvider::class.java)
    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
        component = providerComponent
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
    }
    context.sendBroadcast(intent)
}
