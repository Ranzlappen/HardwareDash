package dev.ranzlappen.gadget.core.widgetkit.provider

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * The content-source → repaint seam for [BaseContentWidgetProvider]-built
 * widgets.
 *
 * A content widget's painted preview depends on feature data that changes
 * outside the widget (the user edits a folder, an app is installed/removed).
 * A feature `@Singleton` observer collects those flows and calls
 * [requestUpdate] to repaint every placed instance of its provider — the
 * direct analogue of the monitoring framework's widget-notifier seam, kept in
 * the kit so no feature re-hand-rolls the enumerate-ids + fire-update dance.
 *
 * Implemented as an explicit `ACTION_APPWIDGET_UPDATE` self-broadcast carrying
 * the provider's current `appWidgetId`s, which re-enters the provider's
 * `onUpdate` → `renderAll` on the framework's own dispatch — so the repaint
 * goes through the exact same path as a launcher-initiated update.
 */
object ContentWidgetUpdater {

    /**
     * Repaint every placed instance of [providerClass]. No-op when none are
     * placed (avoids a pointless broadcast). Safe to call from any thread.
     */
    fun requestUpdate(context: Context, providerClass: Class<out AppWidgetProvider>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, providerClass)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
            this.component = component
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}
