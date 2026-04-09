package com.hardwaredash.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import com.hardwaredash.R
import com.hardwaredash.localization.LocalizationManager
import com.hardwaredash.localization.S

class VibrationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) setupWidget(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            val lang = LocalizationManager.loadLanguage(context)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentlyVibrating = prefs.getBoolean(KEY_VIBRATING, false)

            if (currentlyVibrating) {
                // Stop vibration
                getVibrator(context).cancel()
                prefs.edit().putBoolean(KEY_VIBRATING, false).apply()
                WidgetActionHandler.showToast(context, S.Widget.vibrationOff(lang))
            } else {
                // Start looping vibration
                val vibrator = getVibrator(context)
                val timings = longArrayOf(0, 400, 200, 400) // wait, vib, wait, vib
                val amplitudes = intArrayOf(0, 200, 0, 200)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0) // repeat at index 0
                vibrator.vibrate(effect)
                prefs.edit().putBoolean(KEY_VIBRATING, true).apply()
                WidgetActionHandler.showToast(context, S.Widget.vibrationOn(lang))
            }

            // Update all vibration widgets to reflect state
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VibrationWidgetProvider::class.java))
            for (id in ids) setupWidget(context, manager, id)
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.hardwaredash.widget.ACTION_VIBRATION_TOGGLE"
        private const val PREFS_NAME = "widget_vibration"
        private const val KEY_VIBRATING = "vibrating"

        private fun getVibrator(context: Context): Vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                    .defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        private fun setupWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isOn = prefs.getBoolean(KEY_VIBRATING, false)

            val views = RemoteViews(context.packageName, R.layout.widget_action)
            views.setImageViewResource(
                R.id.widget_action_icon,
                if (isOn) android.R.drawable.ic_lock_silent_mode_off
                else android.R.drawable.ic_lock_silent_mode
            )
            views.setTextViewText(R.id.widget_action_label, if (isOn) "ON" else "Vibrate")

            val intent = Intent(context, VibrationWidgetProvider::class.java).apply {
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
