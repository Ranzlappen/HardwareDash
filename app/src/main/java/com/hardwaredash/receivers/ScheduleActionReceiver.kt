package com.hardwaredash.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.hardwaredash.CallerScreenActivity
import com.hardwaredash.MainActivity

class ScheduleActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "com.hardwaredash.SCHEDULE_ACTION_FIRE"
        const val EXTRA_TYPE = "schedule_type"   // "notification", "lock", "ring"
        const val EXTRA_TITLE = "schedule_title"
        const val EXTRA_BODY = "schedule_body"
        const val EXTRA_ID = "schedule_id"

        private const val CH_SCHEDULE = "hwd_schedule"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val type = intent.getStringExtra(EXTRA_TYPE) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "HardwareDash"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val id = intent.getIntExtra(EXTRA_ID, 0)

        when (type) {
            "notification" -> fireNotification(context, title, body, id)
            "lock" -> fireLock(context)
            "ring" -> fireRing(context)
        }

        // Mark as fired in SharedPreferences
        markFired(context, id)
    }

    private fun fireNotification(context: Context, title: String, body: String, id: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(nm)
        val pi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CH_SCHEDULE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(5000 + id, n)
    }

    private fun fireLock(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, AdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
        }
    }

    private fun fireRing(context: Context) {
        val prefs = context.getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
        val durationSec = prefs.getInt("phone_ring_duration_seconds", 30)

        // Launch full-screen caller activity which handles ringing and stop button
        val callerIntent = Intent(context, CallerScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallerScreenActivity.EXTRA_DURATION, durationSec)
        }
        context.startActivity(callerIntent)
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CH_SCHEDULE, "Scheduled Actions", NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(channel)
    }

    private fun markFired(context: Context, id: Int) {
        val prefs = context.getSharedPreferences("schedule_actions", Context.MODE_PRIVATE)
        val json = prefs.getString("actions", "[]") ?: "[]"
        // Simple find-and-replace status in JSON
        val updated = json.replace("\"id\":$id,\"status\":\"pending\"", "\"id\":$id,\"status\":\"fired\"")
        prefs.edit().putString("actions", updated).apply()
    }
}
