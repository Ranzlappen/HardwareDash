package com.hardwaredash.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hardwaredash.R
import com.hardwaredash.services.VoiceRecordService

class VoiceRecordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val nowRecording = VoiceRecordService.toggle(context)
            WidgetActionHandler.showToast(
                context,
                if (nowRecording) "Recording audio..." else "Recording saved"
            )
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.hardwaredash.widget.ACTION_VOICE_RECORD_TOGGLE"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_btn_speak_now)
            views.setTextViewText(R.id.widget_action_label, "Rec")

            val intent = Intent(context, VoiceRecordWidgetProvider::class.java).apply {
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
