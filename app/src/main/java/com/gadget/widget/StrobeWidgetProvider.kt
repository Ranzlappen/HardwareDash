package com.gadget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.services.StrobeService

class StrobeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val lang = LocalizationManager.loadLanguage(context)
            val nowRunning = StrobeService.toggle(context)
            WidgetActionHandler.showToast(
                context,
                if (nowRunning) S.Widget.strobeStarted(lang) else S.Widget.strobeStopped(lang)
            )
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.gadget.widget.ACTION_STROBE_TOGGLE"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_dialog_alert)
            views.setTextViewText(R.id.widget_action_label, "Strobe")

            val intent = Intent(context, StrobeWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pi = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_action_root, pi)
            manager.updateAppWidget(id, views)
        }
    }
}
