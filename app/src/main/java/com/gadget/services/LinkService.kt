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
import com.gadget.ui.link.ActionStep
import com.gadget.ui.link.ConditionNode
import com.gadget.ui.link.LinkActionType
import com.gadget.ui.link.LinkRuleStats
import com.gadget.ui.link.LinkRuleV2
import com.gadget.ui.link.LogicOperator
import com.gadget.ui.link.TimeSchedule
import com.gadget.ui.link.evaluateCondition
import com.gadget.ui.link.loadLinkStats
import com.gadget.ui.link.loadRulesV2
import com.gadget.ui.link.saveLinkStats
import com.gadget.ui.link.saveRulesV2
import com.gadget.ui.logbook.LogbookEntry
import com.gadget.ui.logbook.LogbookRepository
import com.gadget.widget.DrawnPatternUtils
import com.gadget.widget.WidgetMetric
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
class LinkService : Service() {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Per-leaf "condition first became true at" timestamps; keyed by ruleId|tree-path. */
    private val sustainStartMs = mutableMapOf<String, Long>()

    /** Outstanding trigger-delay coroutines, one per ruleId. */
    private val pendingFires = mutableMapOf<String, Job>()

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
        pendingFires.values.forEach { it.cancel() }
        pendingFires.clear()
        sustainStartMs.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ─── Evaluation loop ──────────────────────────────────────────────────

    private fun evaluateRules() {
        val prefs = getSharedPreferences("link_rules", Context.MODE_PRIVATE)

        // Load V2 (kept-on-disk) rules. If absent, fall back to migrating V1.
        val v2Json = prefs.getString(KEY_RULES_V2, "") ?: ""
        val rules: List<LinkRuleV2> = if (v2Json.isBlank()) {
            val v1Json = prefs.getString(KEY_RULES_V1, "") ?: ""
            val migrated = loadRulesV2(v1Json)
            if (migrated.isNotEmpty()) {
                prefs.edit()
                    .putString(KEY_RULES_V2, saveRulesV2(migrated))
                    .remove(KEY_RULES_V1)
                    .apply()
            }
            migrated
        } else {
            loadRulesV2(v2Json)
        }

        if (rules.isEmpty()) return

        val now = System.currentTimeMillis()
        val statsJson = prefs.getString(KEY_STATS, "") ?: ""
        val stats = loadLinkStats(statsJson).toMutableMap()
        var rulesChanged = false
        var statsChanged = false

        val updatedRules = rules.map { rule ->
            if (!rule.enabled) return@map rule
            if (!isWithinSchedule(rule.schedule)) return@map rule

            val rootTrue = try {
                evaluateNode(rule.root, rule.id, "0", this, now)
            } catch (e: Exception) {
                Timber.e(e, "Tree evaluation failed for rule %s", rule.name)
                false
            }

            val inCooldown = now - rule.lastTriggeredMs < rule.cooldownSec * 1000L

            if (inCooldown) {
                if (rootTrue) {
                    val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                    stats[rule.id] = prev.copy(
                        cooldownBlockCount = prev.cooldownBlockCount + 1,
                        lastCooldownIso = Instant.now().toString(),
                    )
                    statsChanged = true
                }
                return@map rule
            }

            if (rootTrue) {
                if (rule.triggerDelaySec <= 0) {
                    fireRule(rule)
                    val prev = stats[rule.id] ?: LinkRuleStats(ruleId = rule.id)
                    stats[rule.id] = prev.copy(
                        triggerCount = prev.triggerCount + 1,
                        lastTriggeredIso = Instant.now().toString(),
                    )
                    statsChanged = true
                    rulesChanged = true
                    rule.copy(lastTriggeredMs = now)
                } else if (pendingFires[rule.id] == null) {
                    schedulePendingFire(rule, prefs)
                    rule
                } else {
                    rule
                }
            } else {
                rule
            }
        }

        if (rulesChanged || statsChanged) {
            val editor = prefs.edit()
            if (rulesChanged) editor.putString(KEY_RULES_V2, saveRulesV2(updatedRules))
            if (statsChanged) editor.putString(KEY_STATS, saveLinkStats(stats))
            editor.apply()
        }
    }

    /**
     * Schedule a coroutine that waits [LinkRuleV2.triggerDelaySec] seconds and then
     * fires the action chain. Re-checks the condition at fire time when
     * [LinkRuleV2.cancelDelayIfFalse] is true. Updates persisted lastTriggeredMs and
     * stats only if the rule actually fires (so cancelled fires don't burn cooldown).
     */
    private fun schedulePendingFire(rule: LinkRuleV2, prefs: android.content.SharedPreferences) {
        pendingFires[rule.id] = scope.launch {
            try {
                delay(rule.triggerDelaySec * 1000L)
                val fireTime = System.currentTimeMillis()
                val stillTrue = !rule.cancelDelayIfFalse ||
                    runCatching { evaluateNode(rule.root, rule.id, "0", this@LinkService, fireTime) }
                        .getOrDefault(false)
                if (stillTrue) {
                    fireRule(rule)
                    persistFire(prefs, rule.id, fireTime)
                }
            } catch (e: Exception) {
                Timber.e(e, "Pending fire failed for rule %s", rule.name)
            } finally {
                pendingFires.remove(rule.id)
            }
        }
    }

    private fun persistFire(prefs: android.content.SharedPreferences, ruleId: String, firedAtMs: Long) {
        // Re-load to avoid stomping concurrent edits, then write back.
        val rules = loadRulesV2(prefs.getString(KEY_RULES_V2, "") ?: "")
        val updated = rules.map { r -> if (r.id == ruleId) r.copy(lastTriggeredMs = firedAtMs) else r }
        val statsJson = prefs.getString(KEY_STATS, "") ?: ""
        val stats = loadLinkStats(statsJson).toMutableMap()
        val prev = stats[ruleId] ?: LinkRuleStats(ruleId = ruleId)
        stats[ruleId] = prev.copy(
            triggerCount = prev.triggerCount + 1,
            lastTriggeredIso = Instant.now().toString(),
        )
        prefs.edit()
            .putString(KEY_RULES_V2, saveRulesV2(updated))
            .putString(KEY_STATS, saveLinkStats(stats))
            .apply()
    }

    private fun fireRule(rule: LinkRuleV2) {
        scope.launch {
            try {
                executeActionChain(rule.actions)
            } catch (e: Exception) {
                Timber.e(e, "Action chain failed for rule %s", rule.name)
            }
        }
    }

    // ─── Tree evaluation ───────────────────────────────────────────────────

    /**
     * Recursively evaluate a [ConditionNode]. The optional [path] tracks the node's
     * position in the tree so that per-leaf sustain state is keyed uniquely.
     */
    private fun evaluateNode(
        node: ConditionNode,
        ruleId: String,
        path: String,
        ctx: Context,
        now: Long,
    ): Boolean {
        val raw = when (node) {
            is ConditionNode.Leaf -> evaluateLeaf(node, ruleId, path, ctx, now)
            is ConditionNode.Group -> {
                if (node.children.isEmpty()) false
                else {
                    val childResults = node.children.mapIndexed { i, child ->
                        evaluateNode(child, ruleId, "$path.$i", ctx, now)
                    }
                    when (node.logic) {
                        LogicOperator.AND -> childResults.all { it }
                        LogicOperator.OR -> childResults.any { it }
                    }
                }
            }
        }
        return if (node.negate) !raw else raw
    }

    private fun evaluateLeaf(
        leaf: ConditionNode.Leaf,
        ruleId: String,
        path: String,
        ctx: Context,
        now: Long,
    ): Boolean {
        val metric = WidgetMetric.fromKey(leaf.metricKey) ?: return false
        val value = try {
            metric.fetch(ctx)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch metric %s", leaf.metricKey)
            return false
        }
        val instant = evaluateCondition(value, leaf.operator, leaf.threshold, leaf.thresholdHigh)
        if (leaf.sustainSec <= 0) {
            return instant
        }
        val key = "$ruleId|$path"
        return if (instant) {
            val first = sustainStartMs.getOrPut(key) { now }
            now - first >= leaf.sustainSec * 1000L
        } else {
            sustainStartMs.remove(key)
            false
        }
    }

    // ─── Action execution ──────────────────────────────────────────────────

    /** Execute a chain of [ActionStep]s sequentially, respecting per-step delays. */
    private suspend fun executeActionChain(actions: List<ActionStep>) {
        for (step in actions) {
            if (step.delayMs > 0) {
                delay(step.delayMs)
            }
            try {
                executeActionStep(step)
            } catch (e: Exception) {
                Timber.e(e, "Action step failed: %s", step.actionType)
            }
        }
    }

    private fun executeActionStep(step: ActionStep) {
        when (LinkActionType.fromKey(step.actionType)) {
            LinkActionType.TORCH_ON -> setTorch(true)
            LinkActionType.TORCH_OFF -> setTorch(false)
            LinkActionType.STROBE_START -> { if (!StrobeService.isRunning) StrobeService.toggle(this) }
            LinkActionType.STROBE_STOP -> { if (StrobeService.isRunning) StrobeService.toggle(this) }
            LinkActionType.VIBRATE -> vibrate()
            LinkActionType.NOTIFICATION -> sendNotification(step)
            LinkActionType.LOCK -> lockScreen()
            LinkActionType.RING -> ringPhone()
            LinkActionType.LOG_ENTRY -> logToLogbook(step)
        }
    }

    // ─── Schedule gate ─────────────────────────────────────────────────────

    /** Returns true when no schedule is set, or current wall-clock time is within it. */
    private fun isWithinSchedule(schedule: TimeSchedule?): Boolean {
        if (schedule == null) return true

        if (schedule.daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (today !in schedule.daysOfWeek) return false
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalTime.now()
        val start = try { LocalTime.parse(schedule.startTime, formatter) } catch (_: Exception) { LocalTime.MIN }
        val end = try { LocalTime.parse(schedule.endTime, formatter) } catch (_: Exception) { LocalTime.MAX }

        return if (start <= end) {
            now in start..end
        } else {
            now >= start || now <= end
        }
    }

    // ─── Action implementations ───────────────────────────────────────────

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
                if (t.isEmpty()) error("empty pattern")
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

    private fun sendNotification(step: ActionStep) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = step.actionConfig["title"] ?: "Link Alert"
        val body = step.actionConfig["body"] ?: ""
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
        // Use a hash of the title so repeated identical alerts collapse.
        nm.notify(NOTIF_ID_ACTION_BASE + (title.hashCode() and 0xFFF), n)
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

    private fun logToLogbook(step: ActionStep) {
        try {
            val text = step.actionConfig["logText"]?.ifBlank { null }
                ?: "Link triggered"
            val metrics = try { WidgetMetric.snapshotEnabled(this) }
            catch (e: Exception) { Timber.e(e, "Metric snapshot failed"); emptyMap() }
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
        } catch (e: Exception) { Timber.e(e, "logToLogbook failed") }
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
        pendingFires.values.forEach { it.cancel() }
        pendingFires.clear()
        sustainStartMs.clear()
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
        private const val KEY_RULES_V1 = "rules"
        private const val KEY_RULES_V2 = "rules_v2"

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
