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
 * actually hosting [appWidgetId] so a fresh-pin or in-app edit repaints the
 * placed widget immediately.
 *
 * The provider class is resolved from the live
 * [AppWidgetManager.getAppWidgetInfo] (so a legacy pattern widget repaints via
 * [PatternWidgetProvider] and everything else via the designated
 * [VibrateWidgetProvider]); it falls back to [VibrateWidgetProvider] when the
 * info isn't available yet (e.g. a just-pinned id the host hasn't surfaced).
 */
internal fun broadcastVibrationWidgetUpdate(
    context: Context,
    appWidgetId: Int,
) {
    val manager = AppWidgetManager.getInstance(context)
    val provider = manager?.getAppWidgetInfo(appWidgetId)?.provider
        ?: ComponentName(context, VibrateWidgetProvider::class.java)
    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        component = provider
    }
    context.sendBroadcast(intent)
}
