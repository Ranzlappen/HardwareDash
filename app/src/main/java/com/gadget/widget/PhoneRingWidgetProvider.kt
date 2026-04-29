package com.gadget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.receivers.ScheduleActionReceiver

class PhoneRingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_RING_30S) {
            val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
            val delaySec = prefs.getInt("phone_ring_delay_seconds", 0)
            val durationSec = prefs.getInt("phone_ring_duration_seconds", 30)
            val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val alarmIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                action = ScheduleActionReceiver.ACTION_FIRE
                putExtra(ScheduleActionReceiver.EXTRA_TYPE, "ring")
                putExtra(ScheduleActionReceiver.EXTRA_TITLE, "Phone Ring")
                putExtra(ScheduleActionReceiver.EXTRA_ID, id)
                putExtra(ScheduleActionReceiver.EXTRA_DURATION_OVERRIDE, durationSec)
            }
            val pi = PendingIntent.getBroadcast(
                context, id, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delaySec * 1000L,
                pi,
            )
            val lang = LocalizationManager.loadLanguage(context)
            WidgetActionHandler.showToast(context, S.Widget.phoneRingToast(lang, delaySec, durationSec))
        }
    }

    companion object {
        private const val ACTION_RING_30S = "com.gadget.widget.ACTION_RING_30S"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(R.id.widget_action_icon, android.R.drawable.ic_menu_call)
            views.setTextViewText(R.id.widget_action_label, "Ring")

            val intent = Intent(context, PhoneRingWidgetProvider::class.java).apply {
                action = ACTION_RING_30S
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
