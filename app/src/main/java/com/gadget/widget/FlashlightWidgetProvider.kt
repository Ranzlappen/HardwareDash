package com.gadget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gadget.R
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S

class FlashlightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val prefs = context.getSharedPreferences("widget_flashlight", Context.MODE_PRIVATE)
            val currentlyOn = prefs.getBoolean("torch_on", false)
            val newState = !currentlyOn
            val lang = LocalizationManager.loadLanguage(context)
            val success = WidgetActionHandler.toggleTorch(context, newState)
            if (success) {
                prefs.edit().putBoolean("torch_on", newState).apply()
                WidgetActionHandler.showToast(context, if (newState) S.Widget.torchOn(lang) else S.Widget.torchOff(lang))
                // Update all flashlight widgets to reflect state
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, FlashlightWidgetProvider::class.java))
                for (id in ids) setupWidget(context, manager, id)
            } else {
                WidgetActionHandler.showToast(context, S.Widget.noFlashAvailable(lang))
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.gadget.widget.ACTION_FLASHLIGHT_TOGGLE"

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences("widget_flashlight", Context.MODE_PRIVATE)
            val isOn = prefs.getBoolean("torch_on", false)

            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(
                R.id.widget_action_icon,
                if (isOn) android.R.drawable.ic_dialog_info else android.R.drawable.ic_menu_view
            )
            views.setTextViewText(R.id.widget_action_label, if (isOn) "ON" else "Flash")

            val intent = Intent(context, FlashlightWidgetProvider::class.java).apply {
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
