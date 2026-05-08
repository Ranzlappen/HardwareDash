package com.gadget.ui.screens

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.gadget.receivers.ScheduleActionReceiver
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.app.NotificationCompat
import com.google.accompanist.permissions.*
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.root.ui.NotificationRootExtrasSection
import com.gadget.ui.components.ActionEntrySelector
import com.gadget.ui.components.LabeledOption
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.SectionHeader
import com.gadget.ui.components.SliderWithInput
import com.gadget.ui.screens.notifications.BuilderPresetStore
import com.gadget.ui.screens.notifications.NotifActionEntry
import com.gadget.ui.screens.notifications.NotifSpec
import com.gadget.ui.screens.notifications.NotifStyle
import com.gadget.ui.screens.notifications.NotificationPreviewCard
import com.gadget.ui.screens.notifications.ProgressMode
import com.gadget.ui.screens.notifications.buildNotification
import com.gadget.ui.screens.notifications.ensureAllChannels
import com.gadget.receivers.AdminReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Channel IDs and ensureAllChannels live in NotificationChannels.kt.

// NotifActionEntry, NotifSpec, ProgressMode and buildNotification live in
// ui/screens/notifications/NotificationModels.kt.

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LockScreenScreen() {
    val context = LocalContext.current
    val dpm     = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin   = ComponentName(context, AdminReceiver::class.java)
    val nm      = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    LaunchedEffect(Unit) { ensureAllChannels(context) }

    var isAdmin    by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAdmin    = dpm.isAdminActive(admin)
        hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(context) else true
    }

    // Notification permission
    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    else null
    val notifGranted = notifPerm?.status?.isGranted ?: true

    // Delivery / scheduling state for the merged builder.
    var deliveryStatus by remember { mutableStateOf("") }
    var schedType    by remember { mutableStateOf("notification") } // notification, lock, ring
    var schedDate    by remember { mutableStateOf(LocalDate.now()) }
    var schedTime    by remember { mutableStateOf(LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Schedule list + preset list (both backed by "schedule_actions" prefs).
    val schedPrefs = remember { context.getSharedPreferences("schedule_actions", Context.MODE_PRIVATE) }
    var schedList by remember { mutableStateOf(loadScheduleList(schedPrefs)) }
    var presetName by remember { mutableStateOf("") }
    var presetList by remember { mutableStateOf(BuilderPresetStore.load(context)) }
    var controlsExpanded by remember { mutableStateOf(false) }

    // Custom notification builder state
    var customTitle    by remember { mutableStateOf("Gadget") }
    var customBody     by remember { mutableStateOf("Custom notification") }
    var customPriority by remember { mutableIntStateOf(NotificationCompat.PRIORITY_DEFAULT) }
    var customVisibility by remember { mutableIntStateOf(NotificationCompat.VISIBILITY_PUBLIC) }
    var customCategory by remember { mutableStateOf(NotificationCompat.CATEGORY_MESSAGE) }
    var customColorIdx by remember { mutableIntStateOf(0) }
    val colorOptions = listOf(
        Color(0xFF2196F3) to "Blue",
        Color(0xFF4CAF50) to "Green",
        Color(0xFFFFC107) to "Amber",
        Color(0xFFF44336) to "Red",
        Color(0xFF9C27B0) to "Purple",
        Color(0xFF00BCD4) to "Cyan",
    )
    // Enhanced builder state
    var customSubtext by remember { mutableStateOf("") }
    var customActionCount by remember { mutableIntStateOf(0) }
    val customActions = remember { mutableStateListOf(NotifActionEntry.OPEN_APP, NotifActionEntry.OPEN_APP, NotifActionEntry.OPEN_APP) }
    var customQuickReply by remember { mutableStateOf("") }
    var customShowProgress by remember { mutableStateOf(false) }
    var customProgressIndeterminate by remember { mutableStateOf(true) }
    var customProgressValue by remember { mutableFloatStateOf(50f) }
    var customOngoing by remember { mutableStateOf(false) }
    var customAutoCancel by remember { mutableStateOf(true) }
    var customSound by remember { mutableStateOf(true) }
    var customVibrate by remember { mutableStateOf(true) }
    var customTimeoutSec by remember { mutableFloatStateOf(0f) }
    var customBadge by remember { mutableIntStateOf(0) }
    var customDelaySec by remember { mutableFloatStateOf(0f) }
    var customStyleIdx by remember { mutableIntStateOf(0) } // 0=Normal, 1=BigText, 2=Inbox

    ScreenAnnouncement(S.accessibility.lockScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(
                Icons.Default.Lock, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                S.lock.title,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Notification permission ──────────────────────────────────────────
        if (!notifGranted) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(S.lock.grantPermission,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { notifPerm?.launchPermissionRequest() }) {
                        Text(S.lock.grantPermission)
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // Compose Notification (merged builder + scheduler)
        // Field-map note: the old Section 9 (Lock Screen Notification Designer)
        // mapped onto NotifSpec / scheduling state as follows:
        //   lsTitle/lsBody    → customTitle/customBody
        //   lsVisibility      → customVisibility (chip-based, same enum)
        //   lsPriority (3lvl) → customPriority (5lvl, superset)
        //   lsCategory        → customCategory
        //   lsDelayMin (0-60) → customDelaySec (now 0-60 minutes)
        //   schedDate/schedTime/schedType → unchanged, same names
        //   sendLockScreenNotification(...) → buildNotification(spec) on CH_HIGH
        // ══════════════════════════════════════════════════════════════════════
        SectionHeader(S.lock.composeNotification)

        LabeledOption(S.lock.titleLabel, S.lock.titleHelp) {
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        LabeledOption(S.lock.bodyLabel, S.lock.bodyHelp) {
            OutlinedTextField(
                value = customBody,
                onValueChange = { customBody = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
        }
        LabeledOption(S.lock.subtextLabel, S.lock.subtextHelp) {
            OutlinedTextField(
                value = customSubtext,
                onValueChange = { customSubtext = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Text(S.lock.priority, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            listOf(
                "Min" to NotificationCompat.PRIORITY_MIN,
                "Low" to NotificationCompat.PRIORITY_LOW,
                "Default" to NotificationCompat.PRIORITY_DEFAULT,
                "High" to NotificationCompat.PRIORITY_HIGH,
                "Max" to NotificationCompat.PRIORITY_MAX,
            ).forEach { (label, prio) ->
                FilterChip(
                    selected = customPriority == prio,
                    onClick  = { customPriority = prio },
                    label    = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        LabeledOption(S.lock.lockScreenVisibility, S.lock.visibilityHelp) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "Public" to NotificationCompat.VISIBILITY_PUBLIC,
                    "Private" to NotificationCompat.VISIBILITY_PRIVATE,
                    "Secret" to NotificationCompat.VISIBILITY_SECRET,
                ).forEach { (label, vis) ->
                    FilterChip(
                        selected = customVisibility == vis,
                        onClick  = { customVisibility = vis },
                        label    = { Text(label) },
                    )
                }
            }
        }

        LabeledOption(S.lock.categoryLabel, S.lock.categoryHelp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                listOf(
                    S.lock.message to NotificationCompat.CATEGORY_MESSAGE,
                    S.lock.alarm to NotificationCompat.CATEGORY_ALARM,
                    S.lock.reminder to NotificationCompat.CATEGORY_REMINDER,
                    S.lock.event to NotificationCompat.CATEGORY_EVENT,
                ).forEach { (label, cat) ->
                    FilterChip(
                        selected = customCategory == cat,
                        onClick = { customCategory = cat },
                        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        Text(S.lock.accentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colorOptions.forEachIndexed { idx, (color, _) ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (idx == customColorIdx) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { customColorIdx = idx }
                )
            }
        }

        // ── Action Buttons ────────────────────────────────────────────────
        LabeledOption(S.lock.actionButtons, S.lock.actionsHelp) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..3).forEach { count ->
                    FilterChip(
                        selected = customActionCount == count,
                        onClick = { customActionCount = count },
                        label = { Text("$count", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
        for (i in 0 until customActionCount) {
            ActionEntrySelector(
                label = "Action ${i + 1}",
                value = customActions[i],
                onChange = { customActions[i] = it },
            )
        }
        if (customActionCount >= 1) {
            LabeledOption(S.lock.quickReplyLabel, S.lock.quickReplyHelp) {
                OutlinedTextField(
                    value = customQuickReply,
                    onValueChange = { customQuickReply = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // ── Progress Bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(S.lock.enableProgress, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Switch(checked = customShowProgress, onCheckedChange = { customShowProgress = it })
        }
        if (customShowProgress) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = customProgressIndeterminate,
                    onClick = { customProgressIndeterminate = true },
                    label = { Text(S.lock.indeterminate, style = MaterialTheme.typography.labelSmall) },
                )
                FilterChip(
                    selected = !customProgressIndeterminate,
                    onClick = { customProgressIndeterminate = false },
                    label = { Text(S.lock.determinate, style = MaterialTheme.typography.labelSmall) },
                )
            }
            if (!customProgressIndeterminate) {
                SliderWithInput(
                    value = customProgressValue,
                    onValueChange = { customProgressValue = it },
                    valueRange = 0f..100f,
                    formatValue = { "%.0f".format(it) },
                    suffix = "%",
                )
            }
        }

        // ── Style ────────────────────────────────────────────────────────
        Text(S.lock.style, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(S.lock.normal, S.lock.bigText, S.lock.inbox).forEachIndexed { idx, label ->
                FilterChip(
                    selected = customStyleIdx == idx,
                    onClick = { customStyleIdx = idx },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // ── Flags & extras ───────────────────────────────────────────────
        SwitchRow(S.lock.ongoing, S.lock.ongoingHelp, customOngoing) { customOngoing = it }
        SwitchRow(S.lock.autoCancel, S.lock.autoCancelHelp, customAutoCancel) { customAutoCancel = it }
        SwitchRow(S.lock.soundLabel, S.lock.soundHelp, customSound) { customSound = it }
        SwitchRow(S.lock.vibrateLabel, S.lock.vibrateHelp, customVibrate) { customVibrate = it }

        LabeledOption(S.lock.timeoutLabel, S.lock.timeoutHelp) {
            SliderWithInput(
                value = customTimeoutSec,
                onValueChange = { customTimeoutSec = it },
                valueRange = 0f..120f,
                formatValue = { "%.0f".format(it) },
                suffix = "s",
                label = "${customTimeoutSec.toInt()} s",
            )
        }
        LabeledOption(S.lock.badgeLabel, S.lock.badgeHelp) {
            SliderWithInput(
                value = customBadge.toFloat(),
                onValueChange = { customBadge = it.toInt() },
                valueRange = 0f..99f,
                formatValue = { "%.0f".format(it) },
                suffix = "",
                label = "$customBadge",
            )
        }

        // ── Delay ────────────────────────────────────────────────────────
        LabeledOption(S.lock.delay, S.lock.delayHelp) {
            SliderWithInput(
                value = customDelaySec,
                onValueChange = { customDelaySec = it },
                valueRange = 0f..60f,
                formatValue = { "%.1f".format(it) },
                suffix = "min",
                label = if (customDelaySec < 1f) "${(customDelaySec * 60).toInt()} sec" else "${"%.0f".format(customDelaySec)} min",
            )
        }

        // ── Preview Card ─────────────────────────────────────────────────
        Text(S.lock.preview, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        val previewSpec = NotifSpec(
            title = customTitle,
            body = customBody,
            subtext = customSubtext,
            priority = customPriority,
            visibility = customVisibility,
            category = customCategory,
            accentColor = colorOptions[customColorIdx].first.toArgb(),
            actions = customActions.take(customActionCount),
            progressMode = when {
                !customShowProgress -> ProgressMode.OFF
                customProgressIndeterminate -> ProgressMode.INDETERMINATE
                else -> ProgressMode.DETERMINATE
            },
            progressValue = customProgressValue.toInt(),
            style = when (customStyleIdx) {
                1 -> NotifStyle.BIG_TEXT
                2 -> NotifStyle.INBOX
                else -> NotifStyle.NORMAL
            },
            ongoing = customOngoing,
            autoCancel = customAutoCancel,
            sound = customSound,
            vibrate = customVibrate,
            timeoutSec = customTimeoutSec.toInt(),
            badge = customBadge,
            quickReplyHint = customQuickReply,
        )
        NotificationPreviewCard(previewSpec)

        // ── Delivery: schedule type, exact date/time, Send Now / Schedule ──
        LabeledOption(S.lock.scheduleTypeLabel, S.lock.scheduleTypeHelp) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "notification" to S.lock.notification,
                    "lock" to S.lock.lockScreen,
                    "ring" to S.lock.phoneRing,
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = schedType == type,
                        onClick = { schedType = type },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        LabeledOption(S.lock.scheduleForLabel, S.lock.scheduleForHelp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = schedDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(S.lock.dateLabel) },
                    modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                    textStyle = MaterialTheme.typography.bodySmall,
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp)) },
                    shape = MaterialTheme.shapes.small,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
                        LaunchedEffect(source) {
                            source.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showDatePicker = true
                                }
                            }
                        }
                    },
                )
                OutlinedTextField(
                    value = "%02d:%02d".format(schedTime.hour, schedTime.minute),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(S.lock.timeLabel) },
                    modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                    textStyle = MaterialTheme.typography.bodySmall,
                    leadingIcon = { Icon(Icons.Default.Schedule, null, Modifier.size(18.dp)) },
                    shape = MaterialTheme.shapes.small,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
                        LaunchedEffect(source) {
                            source.interactions.collect { interaction ->
                                if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                    showTimePicker = true
                                }
                            }
                        }
                    },
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val notifId = 2000 + (System.currentTimeMillis() % 1000).toInt()
                    if (customDelaySec > 0) {
                        val delayMs = (customDelaySec * 60 * 1000).toLong()
                        val schedIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                            action = ScheduleActionReceiver.ACTION_FIRE
                            putExtra(ScheduleActionReceiver.EXTRA_TYPE, "notification")
                            putExtra(ScheduleActionReceiver.EXTRA_TITLE, customTitle)
                            putExtra(ScheduleActionReceiver.EXTRA_BODY, customBody)
                            putExtra(ScheduleActionReceiver.EXTRA_ID, notifId)
                        }
                        val schedPi = PendingIntent.getBroadcast(
                            context, notifId, schedIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, schedPi)
                        deliveryStatus = "Sent in ${"%.1f".format(customDelaySec)} min"
                    } else {
                        nm.notify(notifId, buildNotification(context, previewSpec))
                        deliveryStatus = "Sent immediately"
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = notifGranted && customTitle.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
                Spacer(Modifier.width(8.dp))
                Text(S.lock.sendNow)
            }

            Button(
                onClick = {
                    val scheduledLdt = LocalDateTime.of(schedDate, schedTime)
                    val triggerMillis = scheduledLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    if (triggerMillis <= System.currentTimeMillis()) {
                        deliveryStatus = "Time must be in the future"
                        return@Button
                    }

                    val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                    val title = if (schedType == "notification") customTitle else schedType.replaceFirstChar { it.uppercase() }

                    val alarmIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                        action = ScheduleActionReceiver.ACTION_FIRE
                        putExtra(ScheduleActionReceiver.EXTRA_TYPE, schedType)
                        putExtra(ScheduleActionReceiver.EXTRA_TITLE, title)
                        putExtra(ScheduleActionReceiver.EXTRA_BODY, customBody)
                        putExtra(ScheduleActionReceiver.EXTRA_ID, id)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)

                    val entry = JSONObject().apply {
                        put("id", id)
                        put("type", schedType)
                        put("title", title)
                        put("scheduledAt", scheduledLdt.toString())
                        put("status", "pending")
                    }
                    val arr = try { JSONArray(schedPrefs.getString("actions", "[]")) } catch (_: Exception) { JSONArray() }
                    arr.put(entry)
                    schedPrefs.edit().putString("actions", arr.toString()).apply()
                    schedList = loadScheduleList(schedPrefs)

                    val formatter = DateTimeFormatter.ofPattern("HH:mm, MMM d")
                    deliveryStatus = "Scheduled $schedType at ${scheduledLdt.format(formatter)}"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = customTitle.isNotBlank(),
            ) { Text(S.lock.schedule) }
        }

        if (deliveryStatus.isNotEmpty()) {
            Text(deliveryStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        // ── Scheduled Actions list ───────────────────────────────────────────
        if (schedList.isNotEmpty()) {
            HorizontalDivider()
            Text(S.lock.scheduledActions + " (${schedList.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            schedList.forEach { item ->
                val itemId = item.optInt("id", 0)
                val itemType = item.optString("type", "notification")
                val itemTitle = item.optString("title", "")
                val itemScheduledAt = item.optString("scheduledAt", "")
                val itemStatus = item.optString("status", "pending")
                val icon = when (itemType) {
                    "ring" -> Icons.Default.PhoneInTalk
                    "lock" -> Icons.Default.Lock
                    else -> Icons.Default.Notifications
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, null, modifier = Modifier.size(20.dp),
                            tint = if (itemStatus == "fired") Color.Gray else MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(itemTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(itemScheduledAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            itemStatus.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (itemStatus == "fired") Color.Gray else MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = {
                            val cancelIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                                action = ScheduleActionReceiver.ACTION_FIRE
                            }
                            val cancelPi = PendingIntent.getBroadcast(
                                context, itemId, cancelIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                            val alm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alm.cancel(cancelPi)
                            val arr = try { JSONArray(schedPrefs.getString("actions", "[]")) } catch (_: Exception) { JSONArray() }
                            val newArr = JSONArray()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                if (obj.optInt("id", -1) != itemId) newArr.put(obj)
                            }
                            schedPrefs.edit().putString("actions", newArr.toString()).apply()
                            schedList = loadScheduleList(schedPrefs)
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // ── Save as Template ─────────────────────────────────────────────────
        SectionHeader(S.lock.saveAsTemplate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text(S.lock.presetName) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            FilledTonalButton(
                onClick = {
                    BuilderPresetStore.save(context, presetName, previewSpec)
                    presetList = BuilderPresetStore.load(context)
                    presetName = ""
                },
                enabled = presetName.isNotBlank(),
            ) { Text(S.lock.savePreset) }
        }
        if (presetList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presetList.forEach { preset ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(preset.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = {
                                customTitle = preset.spec.title
                                customBody = preset.spec.body
                                customSubtext = preset.spec.subtext
                                customPriority = preset.spec.priority
                                customVisibility = preset.spec.visibility
                                preset.spec.category?.let { customCategory = it }
                                customColorIdx = preset.spec.accentColor
                                    ?.let { argb -> colorOptions.indexOfFirst { it.first.toArgb() == argb }.takeIf { it >= 0 } }
                                    ?: customColorIdx
                                customActionCount = preset.spec.actions.size.coerceIn(0, 3)
                                preset.spec.actions.forEachIndexed { i, a -> if (i < customActions.size) customActions[i] = a }
                                customQuickReply = preset.spec.quickReplyHint
                                customShowProgress = preset.spec.progressMode != ProgressMode.OFF
                                customProgressIndeterminate = preset.spec.progressMode == ProgressMode.INDETERMINATE
                                customProgressValue = preset.spec.progressValue.toFloat()
                                customStyleIdx = when (preset.spec.style) {
                                    NotifStyle.NORMAL -> 0
                                    NotifStyle.BIG_TEXT -> 1
                                    NotifStyle.INBOX -> 2
                                }
                                customOngoing = preset.spec.ongoing
                                customAutoCancel = preset.spec.autoCancel
                                customSound = preset.spec.sound
                                customVibrate = preset.spec.vibrate
                                customTimeoutSec = preset.spec.timeoutSec.toFloat()
                                customBadge = preset.spec.badge
                            }) { Text(S.lock.loadPreset) }
                            IconButton(onClick = {
                                BuilderPresetStore.delete(context, preset.name)
                                presetList = BuilderPresetStore.load(context)
                            }) {
                                Icon(Icons.Default.Delete, S.lock.deletePreset, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // Lock Screen Controls (collapsible)
        // Cancel All / Capabilities / Device Admin / Overlay / Lock Now /
        // Emergency Alerts. Default collapsed - these are device-wide
        // controls separate from the notification builder above.
        // ══════════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { controlsExpanded = !controlsExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(S.lock.lockScreenControls, modifier = Modifier.weight(1f))
            Icon(
                if (controlsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        }

        if (controlsExpanded) { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 3 — Emergency Alerts
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.emergencyAlerts, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.lock.wirelessEmergencyAlerts, fontWeight = FontWeight.SemiBold)
                Text(
                    "WEA alerts (AMBER, severe weather, presidential) are managed at the system level. " +
                    "Third-party apps cannot read or send emergency alerts without privileged access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Warning, null)
                    Spacer(Modifier.width(8.dp))
                    Text(S.lock.openEmergencySettings)
                }
            }
        }

        HorizontalDivider()

        NotificationRootExtrasSection()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 4 — Cancel All Notifications
        // ══════════════════════════════════════════════════════════════════════
        OutlinedButton(
            onClick  = { nm.cancelAll() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ClearAll, null)
            Spacer(Modifier.width(8.dp))
            Text(S.lock.cancelAllNotif)
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 5 — Capability overview
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.capabilities, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(S.lock.whatIsPossibleWithoutRoot, fontWeight = FontWeight.SemiBold)
                listOf(
                    "OK  Lock the screen (Device Admin)",
                    "OK  Show this Activity over the lock screen",
                    "OK  Draw a persistent overlay on the lock screen",
                    "OK  Turn the screen on programmatically",
                    "OK  Design custom lock screen notifications",
                    "OK  Schedule timed lock screen notifications",
                    "NO  Replace / fully customise the lock screen UI",
                    "NO  Permanently disable the lock screen",
                    "NO  Read or bypass the PIN / password",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 6 — Device Admin
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.deviceAdmin, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Required to call DevicePolicyManager.lockNow(). " +
            "Android will show its own confirmation dialog.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        PermissionStatusRow("Device Admin active", isAdmin)

        if (!isAdmin) {
            Button(
                onClick = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Allows Gadget to lock the screen programmatically."
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.AdminPanelSettings, null)
                Spacer(Modifier.width(8.dp))
                Text(S.lock.activateDeviceAdmin)
            }
        } else {
            OutlinedButton(
                onClick = {
                    dpm.removeActiveAdmin(admin)
                    isAdmin = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
            ) {
                Icon(Icons.Default.RemoveModerator, null)
                Spacer(Modifier.width(8.dp))
                Text(S.lock.deactivateDeviceAdmin)
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 7 — Overlay permission
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.overlayPermission, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "SYSTEM_ALERT_WINDOW lets Gadget draw a floating window " +
            "directly on top of the lock screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        PermissionStatusRow("Draw over other apps", hasOverlay)

        if (!hasOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Layers, null)
                Spacer(Modifier.width(8.dp))
                Text(S.lock.openOverlaySettings)
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 8 — Actions
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.actions, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Button(
            enabled  = isAdmin,
            onClick  = { dpm.lockNow() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Default.LockClock, null)
            Spacer(Modifier.width(8.dp))
            Text(S.lock.lockScreenNow, style = MaterialTheme.typography.titleMedium)
        }

        if (!isAdmin) {
            Text(
                "Activate Device Admin first to enable screen locking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        } } // close `if (controlsExpanded) { Column { ... } }`

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // HOW IT WORKS — detail card
        // ══════════════════════════════════════════════════════════════════════
        Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.lock.howShowOverLockScreenWorks, fontWeight = FontWeight.SemiBold)
                Text(
                    "MainActivity is declared with android:showWhenLocked=\"true\" and " +
                    "android:turnScreenOn=\"true\" in AndroidManifest.xml. " +
                    "This means any notification action or shortcut that launches the app will " +
                    "display it over the lock screen without requiring the user to unlock first.\n\n" +
                    "Combined with SYSTEM_ALERT_WINDOW you can also start a floating overlay " +
                    "Service that stays visible on the lock screen.\n\n" +
                    "These are the maximum lock-screen capabilities available on " +
                    "unrooted Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = schedDate.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        schedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text(S.lock.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(S.lock.cancel) }
            },
        ) { DatePicker(state = pickerState) }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = schedTime.hour,
            initialMinute = schedTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(S.lock.selectTime) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    schedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text(S.lock.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(S.lock.cancel) }
            },
        )
    }
}

@Composable
private fun SwitchRow(label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    LabeledOption(label, description) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

private fun loadScheduleList(prefs: android.content.SharedPreferences): List<JSONObject> {
    val json = prefs.getString("actions", "[]") ?: "[]"
    val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
    val list = mutableListOf<JSONObject>()
    for (i in 0 until arr.length()) {
        list.add(arr.getJSONObject(i))
    }
    return list.sortedByDescending { it.optString("scheduledAt", "") }
}

@Composable
private fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (granted) MaterialTheme.colorScheme.secondary
                    else         MaterialTheme.colorScheme.error,
        ) {
            Text(
                if (granted) "Granted" else "Not granted",
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
