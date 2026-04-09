package com.hardwaredash.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hardwaredash.R
import com.hardwaredash.ui.logbook.LogbookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.hardwaredash.ui.logbook.LogbookEntry
import java.time.Instant
import java.util.UUID

class LogNowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_LOG_NOW) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = LogbookRepository(context)
                    val store = repo.storeFlow.first()
                    val metrics = WidgetMetric.snapshotEnabled(context)
                    val entry = LogbookEntry(
                        id = UUID.randomUUID().toString(),
                        isoDate = Instant.now().toString(),
                        text = "",
                        custom = false,
                        tags = listOf("widget"),
                        metrics = metrics,
                    )
                    repo.save(store.copy(entries = listOf(entry) + store.entries))
                    WidgetActionHandler.showToast(context, "Logged!")
                } catch (e: Exception) {
                    WidgetActionHandler.showToast(context, "Log failed: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val ACTION_LOG_NOW = "com.hardwaredash.widget.ACTION_LOG_NOW"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_input_add)
            views.setTextViewText(R.id.widget_action_label, "Log")

            val intent = Intent(context, LogNowWidgetProvider::class.java).apply {
                action = ACTION_LOG_NOW
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
