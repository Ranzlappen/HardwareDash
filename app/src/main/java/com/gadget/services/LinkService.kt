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
import android.media.AudioAttributes
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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import com.gadget.ui.link.*
import com.gadget.ui.logbook.LogbookEntry
import com.gadget.ui.logbook.LogbookRepository
import com.gadget.widget.DrawnPatternUtils
import com.gadget.widget.WidgetMetric
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
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
                try {
                    evaluateRules()
                } catch (e: Exception) {
                    Timber.e(e, "Rule evaluation cycle failed")
                }
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
        var rulesChanged = false
        var statsChanged = false

        // Load current stats
        val statsJson = prefs.getString(KEY_STATS, "") ?: ""
        val stats = loadLinkStats(statsJson).toMutableMap()

        val updatedRules = rules.map { rule ->
            if (!rule.enabled) return@map rule

            // Cooldown check first (matches original order — avoids unnecessary sensor reads)
            val inCooldown = now - rule.lastTriggeredMs < rule.cooldownSec * 1000L

            val metric = WidgetMetric.fromKey(rule.metricKey) ?: return@map rule
            val value = try { metric.fetch(this) } catch (e: Exception) { Timber.e(e, "Failed to fetch metric %s", rule.metricKey); return@map rule }

            val conditionMet = evaluateCondition(value, rule.operator, rule.threshold, rule.thresholdHigh)

            if (inCooldown) {
                if (conditionMet) {
                    val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                    stats[rule.id] = prev.copy(
                        cooldownBlockCount = prev.cooldownBlockCount + 1,
                        lastCooldownIso = Instant.now().toString(),
                    )
                    statsChanged = true
                }
                return@map rule
            }

            if (conditionMet) {
                try {
                    executeAction(rule)
                } catch (e: Exception) {
                    Timber.e(e, "Action execution failed for rule: %s", rule.name)
                }
                rulesChanged = true
                val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                stats[rule.id] = prev.copy(
                    triggerCount = prev.triggerCount + 1,
                    lastTriggeredIso = Instant.now().toString(),
                )
                statsChanged = true
                rule.copy(lastTriggeredMs = now)
            } else {
                rule
            }
        }

        // Only write to disk when something actually changed
        if (rulesChanged || statsChanged) {
            val editor = prefs.edit()
            if (rulesChanged) editor.putString("rules", saveRules(updatedRules))
            if (statsChanged) editor.putString(KEY_STATS, saveLinkStats(stats))
            editor.apply()
        }

        // Also evaluate V2 rules
        try {
            evaluateRulesV2()
        } catch (e: Exception) {
            Timber.e(e, "V2 rule evaluation cycle failed")
        }
    }

    // ─── V2 compound condition evaluation ─────────────────────────────────

    /**
     * Evaluate a [CompoundCondition] against live sensor data.
     * AND → all sub-conditions must be true.
     * OR  → at least one sub-condition must be true.
     */
    private fun evaluateCompoundCondition(
        compound: CompoundCondition,
        context: Context,
    ): Boolean {
        if (compound.conditions.isEmpty()) return false
        val results = compound.conditions.map { cond ->
            val metric = WidgetMetric.fromKey(cond.metricKey) ?: return@map false
            val value = try {
                metric.fetch(context)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch metric %s", cond.metricKey)
                return@map false
            }
            evaluateCondition(value, cond.operator, cond.threshold, cond.thresholdHigh)
        }
        return when (compound.logic) {
            LogicOperator.AND -> results.all { it }
            LogicOperator.OR -> results.any { it }
        }
    }

    /**
     * Execute a chain of [ActionStep]s sequentially, respecting per-step delays.
     * Must be called from a coroutine context.
     */
    private suspend fun executeActionChain(actions: List<ActionStep>) {
        for (step in actions) {
            if (step.delayMs > 0) {
                delay(step.delayMs)
            }
            try {
                executeActionStep(step)
            } catch (e: Exception) {
                Timber.e(e, "Action step execution failed: %s", step.actionType)
            }
        }
    }

    /**
     * Execute a single [ActionStep] by delegating to the existing action
     * infrastructure.
     */
    private fun executeActionStep(step: ActionStep) {
        // Build a lightweight LinkRule so we can reuse executeAction()
        val syntheticRule = LinkRule(
            actionType = step.actionType,
            actionConfig = step.actionConfig,
        )
        executeAction(syntheticRule)
    }

    /**
     * Check whether the current wall-clock time falls within a [TimeSchedule].
     * Returns true when no schedule is set (i.e. always active).
     */
    private fun isWithinSchedule(schedule: TimeSchedule?): Boolean {
        if (schedule == null) return true

        // Day-of-week check (Calendar.DAY_OF_WEEK: Sun=1 .. Sat=7)
        if (schedule.daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (today !in schedule.daysOfWeek) return false
        }

        // Time-of-day window check
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalTime.now()
        val start = try { LocalTime.parse(schedule.startTime, formatter) } catch (_: Exception) { LocalTime.MIN }
        val end = try { LocalTime.parse(schedule.endTime, formatter) } catch (_: Exception) { LocalTime.MAX }

        return if (start <= end) {
            // Normal window (e.g. 08:00 – 22:00)
            now in start..end
        } else {
            // Overnight window (e.g. 22:00 – 06:00)
            now >= start || now <= end
        }
    }

    // ─── V2 rule evaluation loop ────────────────────────────────────────────

    private fun evaluateRulesV2() {
        val prefs = getSharedPreferences("link_rules", Context.MODE_PRIVATE)
        val json = prefs.getString("rules_v2", "") ?: ""

        // If no V2 rules stored yet, try migrating V1 rules
        val rules: List<LinkRuleV2> = if (json.isBlank()) {
            val v1Json = prefs.getString("rules", "") ?: ""
            val migrated = loadRulesV2(v1Json)
            if (migrated.isNotEmpty()) {
                prefs.edit().putString("rules_v2", saveRulesV2(migrated)).apply()
            }
            migrated
        } else {
            loadRulesV2(json)
        }

        val now = System.currentTimeMillis()
        var rulesChanged = false
        var statsChanged = false

        val statsJson = prefs.getString(KEY_STATS, "") ?: ""
        val stats = loadLinkStats(statsJson).toMutableMap()

        val updatedRules = rules.map { rule ->
            if (!rule.enabled) return@map rule

            // Schedule gate
            if (!isWithinSchedule(rule.schedule)) return@map rule

            // Cooldown check
            val inCooldown = now - rule.lastTriggeredMs < rule.cooldownSec * 1000L

            // Compound condition evaluation
            val conditionMet = evaluateCompoundCondition(rule.conditions, this)

            if (inCooldown) {
                if (conditionMet) {
                    val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                    stats[rule.id] = prev.copy(
                        cooldownBlockCount = prev.cooldownBlockCount + 1,
                        lastCooldownIso = Instant.now().toString(),
                    )
                    statsChanged = true
                }
                return@map rule
            }

            if (conditionMet) {
                // Launch action chain in the service coroutine scope
                scope.launch {
                    executeActionChain(rule.actions)
                }
                rulesChanged = true
                val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                stats[rule.id] = prev.copy(
                    triggerCount = prev.triggerCount + 1,
                    lastTriggeredIso = Instant.now().toString(),
                )
                statsChanged = true
                rule.copy(lastTriggeredMs = now)
            } else {
                rule
            }
        }

        if (rulesChanged || statsChanged) {
            val editor = prefs.edit()
            if (rulesChanged) editor.putString("rules_v2", saveRulesV2(updatedRules))
            if (statsChanged) editor.putString(KEY_STATS, saveLinkStats(stats))
            editor.apply()
        }
    }

    private fun executeAction(rule: LinkRule) {
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
            LinkActionType.LOG_ENTRY -> logToLogbook(rule)
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
        } catch (e: Exception) { Timber.e(e, "Torch toggle failed") }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val hasAmplitude = vibrator.hasAmplitudeControl()
        val effect = try {
            val activePattern = DrawnPatternUtils.getActiveDrawnPattern(this)
            if (activePattern != null) {
                val (points, loop) = activePattern
                val (t, a) = DrawnPatternUtils.toWaveformArrays(points, hasAmplitude)
                if (t.isEmpty()) throw IllegalStateException("empty pattern")
                VibrationEffect.createWaveform(t, a, if (loop) 0 else -1)
            } else {
                null
            }
        } catch (e: Exception) { Timber.w(e, "Custom vibration pattern failed, using fallback"); null }
            ?: VibrationEffect.createOneShot(500, 255)

        val bypassDnd = getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
            .getBoolean("bypass_dnd", false)
        if (bypassDnd) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            vibrator.vibrate(effect)
        }
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
        } catch (e: Exception) { Timber.e(e, "Lock screen failed") }
    }

    private fun logToLogbook(rule: LinkRule) {
        try {
            val text = rule.actionConfig["logText"]?.ifBlank { null }
                ?: "Link triggered: ${rule.name.ifBlank { "Link Rule" }}"
            val metrics = try { WidgetMetric.snapshotEnabled(this) } catch (e: Exception) { Timber.e(e, "Metric snapshot failed"); emptyMap() }
            val entry = LogbookEntry(
                isoDate = Instant.now().toString(),
                text = text,
                custom = false,
                tags = listOf("link"),
                metrics = metrics,
            )
            scope.launch {
                try {
                    val repo = LogbookRepository(applicationContext)
                    val store = repo.storeFlow.firstOrNull() ?: return@launch
                    repo.save(store.copy(entries = listOf(entry) + store.entries))
                } catch (e: Exception) { Timber.e(e, "Failed to save logbook entry from Link rule") }
            }
        } catch (e: Exception) { Timber.e(e, "logToLogbook failed for rule: %s", rule.name) }
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
        val bypassDnd = getSharedPreferences("widget_settings", Context.MODE_PRIVATE)
            .getBoolean("bypass_dnd", false)
        nm.createNotificationChannel(
            NotificationChannel(CH_LINK, "Link Service", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_LINK_ACTION, "Link Actions", NotificationManager.IMPORTANCE_HIGH).apply {
                setBypassDnd(bypassDnd)
            }
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
        private const val KEY_STATS = "link_stats"

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
