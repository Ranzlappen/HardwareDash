package com.gadget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gadget.MainActivity
import com.gadget.R

class GadgetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (id in appWidgetIds) {
            prefs.remove(prefKey(id))
        }
        prefs.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val id = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, AppWidgetManager.getInstance(context), id)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "widget_config"
        const val ACTION_REFRESH = "com.gadget.widget.ACTION_REFRESH"

        fun prefKey(appWidgetId: Int) = "metric_$appWidgetId"

        fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val metricKey = prefs.getString(prefKey(appWidgetId), null) ?: return
            val metric = WidgetMetric.fromKey(metricKey) ?: return

            val value = try { metric.fetch(context) } catch (_: Exception) { "Error" }

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_metric_name, metric.displayName)
            views.setTextViewText(R.id.widget_metric_value, value)

            // Tap widget -> open app
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPi = PendingIntent.getActivity(
                context, appWidgetId, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, launchPi)

            // Refresh button
            val refreshIntent = Intent(context, GadgetWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPi = PendingIntent.getBroadcast(
                context, appWidgetId + 10000, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPi)

            manager.updateAppWidget(appWidgetId, views)
        }

        /** Update all active widgets (called from WorkManager). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, GadgetWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }
    }
}
