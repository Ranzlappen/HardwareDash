package dev.ranzlappen.gadget.feature.vibration.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Icon alpha (0..255) for a widget the user deleted in-app but that the
 *  launcher still hosts. Dim enough to read as defunct without vanishing
 *  entirely (the app can't remove it from a third-party launcher). */
internal const val REMOVED_WIDGET_ICON_ALPHA: Int = 70

/**
 * Fire an [AppWidgetManager.ACTION_APPWIDGET_UPDATE] broadcast at the provider
 * backing [type], scoped to a single [appWidgetId], so a fresh-pin or in-app
 * edit repaints the placed widget immediately.
 */
internal fun broadcastVibrationWidgetUpdate(
    context: Context,
    type: WidgetType,
    appWidgetId: Int,
) {
    val providerClass = type.providerClass
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
