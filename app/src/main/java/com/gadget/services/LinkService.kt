package com.gadget.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.gadget.CallerScreenActivity
import com.gadget.MainActivity
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.receivers.AdminReceiver
import com.gadget.ui.link.*
import com.gadget.widget.WidgetMetric
import kotlinx.coroutines.*

class LinkService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        ensureChannels()
        val lang = LocalizationManager.loadLanguage(this)
        val notification = Notification.Builder(this, CH_LINK)
            .setContentTitle(S.Services.linkMonitoring(lang))
            .setContentText(S.Services.linkTapToStop(lang))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .build()
        startForeground(NOTIF_ID, notification)

        job?.cancel()
        job = scope.launch {
            while (isActive) {
                evaluateRules()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        job?.cancel()
        job = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun evaluateRules() {
        val prefs = getSharedPreferences("link_rules", Context.MODE_PRIVATE)
        val json = prefs.getString("rules", "") ?: ""
        val rules = loadRules(json)
        val now = System.currentTimeMillis()
        var changed = false

        val updatedRules = rules.map { rule ->
            if (!rule.enabled) return@map rule

            // Cooldown check
            if (now - rule.lastTriggeredMs < rule.cooldownSec * 1000L) return@map rule

            val metric = WidgetMetric.fromKey(rule.metricKey) ?: return@map rule
            val value = try { metric.fetch(this) } catch (_: Exception) { return@map rule }

            if (evaluateCondition(value, rule.operator, rule.threshold, rule.thresholdHigh)) {
                executeAction(rule)
                changed = true
                rule.copy(lastTriggeredMs = now)
            } else {
                rule
            }
        }

        if (changed) {
            prefs.edit().putString("rules", saveRules(updatedRules)).apply()
        }
    }

    private fun executeAction(rule: LinkRule) {
        val lang = LocalizationManager.loadLanguage(this)
        when (LinkActionType.fromKey(rule.actionType)) {
            LinkActionType.TORCH_ON -> setTorch(true)
            LinkActionType.TORCH_OFF -> setTorch(false)
            LinkActionType.STROBE_START -> {
                if (!StrobeService.isRunning) StrobeService.toggle(this)
            }
            LinkActionType.STROBE_STOP -> {
                if (StrobeService.isRunning) StrobeService.toggle(this)
            }
            LinkActionType.VIBRATE -> vibrate()
            LinkActionType.NOTIFICATION -> sendNotification(rule)
            LinkActionType.LOCK -> lockScreen()
            LinkActionType.RING -> ringPhone()
        }
    }

    private fun setTorch(on: Boolean) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cid = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cm.setTorchMode(cid, on)
        } catch (_: Exception) {}
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun sendNotification(rule: LinkRule) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = rule.actionConfig["title"] ?: "Link Alert"
        val body = rule.actionConfig["body"] ?: ""
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(this, CH_LINK_ACTION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID_ACTION_BASE + rule.id.hashCode().and(0xFFF), n)
    }

    private fun lockScreen() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, AdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow()
            }
        } catch (_: Exception) {}
    }

    private fun ringPhone() {
        val prefs = getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
        val durationSec = prefs.getInt("phone_ring_duration_seconds", 30)
        val callerIntent = Intent(this, CallerScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(CallerScreenActivity.EXTRA_DURATION, durationSec)
        }
        startActivity(callerIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.cancel()
        isRunning = false
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_LINK, "Link Service", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_LINK_ACTION, "Link Actions", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val ACTION_START = "com.gadget.LINK_START"
        const val ACTION_STOP = "com.gadget.LINK_STOP"
        private const val CH_LINK = "hwd_link"
        private const val CH_LINK_ACTION = "hwd_link_action"
        private const val NOTIF_ID = 7003
        private const val NOTIF_ID_ACTION_BASE = 8000
        private const val POLL_INTERVAL_MS = 3000L

        var isRunning = false
            private set

        fun toggle(context: Context): Boolean {
            return if (!isRunning) {
                val intent = Intent(context, LinkService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                true
            } else {
                context.startService(Intent(context, LinkService::class.java).apply { action = ACTION_STOP })
                isRunning = false
                false
            }
        }
    }
}
