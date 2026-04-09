package com.hardwaredash.ui.screens

import android.app.AlarmManager
import android.app.NotificationChannel
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
import com.hardwaredash.receivers.ScheduleActionReceiver
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
import com.hardwaredash.localization.S
import com.hardwaredash.ui.components.SliderWithInput
import com.hardwaredash.MainActivity
import com.hardwaredash.receivers.AdminReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Notification channel IDs ────────────────────────────────────────────────
private const val CH_LOCKSCREEN = "hwd_lockscreen"
private const val CH_DEFAULT    = "hwd_default"
private const val CH_HIGH       = "hwd_high"
private const val CH_PROGRESS   = "hwd_progress"
private const val CH_CUSTOM     = "hwd_custom"

private fun ensureAllChannels(nm: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    listOf(
        NotificationChannel(CH_LOCKSCREEN, "Lock Screen Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        },
        NotificationChannel(CH_DEFAULT,  "Default",         NotificationManager.IMPORTANCE_DEFAULT),
        NotificationChannel(CH_HIGH,     "High / Heads-Up", NotificationManager.IMPORTANCE_HIGH),
        NotificationChannel(CH_PROGRESS, "Progress",        NotificationManager.IMPORTANCE_LOW),
        NotificationChannel(CH_CUSTOM,   "Custom",          NotificationManager.IMPORTANCE_HIGH),
    ).forEach { nm.createNotificationChannel(it) }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LockScreenScreen() {
    val context = LocalContext.current
    val dpm     = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin   = ComponentName(context, AdminReceiver::class.java)
    val nm      = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    LaunchedEffect(Unit) { ensureAllChannels(nm) }

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

    // Lock screen notification designer state
    var lsTitle     by remember { mutableStateOf("Lock Screen Alert") }
    var lsBody      by remember { mutableStateOf("Custom lock screen notification") }
    var lsVisibility by remember { mutableIntStateOf(NotificationCompat.VISIBILITY_PUBLIC) }
    var lsPriority  by remember { mutableIntStateOf(NotificationCompat.PRIORITY_HIGH) }
    var lsCategory  by remember { mutableStateOf(NotificationCompat.CATEGORY_MESSAGE) }
    var lsDelayMin  by remember { mutableFloatStateOf(0f) }
    var lsScheduleStatus by remember { mutableStateOf("") }

    // Enhanced scheduling state
    var schedType    by remember { mutableStateOf("notification") } // notification, lock, ring
    var schedDate    by remember { mutableStateOf(LocalDate.now()) }
    var schedTime    by remember { mutableStateOf(LocalTime.now().plusMinutes(5).withSecond(0).withNano(0)) }
    var schedTitle   by remember { mutableStateOf("Scheduled Alert") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Schedule list
    val schedPrefs = remember { context.getSharedPreferences("schedule_actions", Context.MODE_PRIVATE) }
    var schedList by remember { mutableStateOf(loadScheduleList(schedPrefs)) }

    // Custom notification builder state
    var customTitle    by remember { mutableStateOf("HardwareDash") }
    var customBody     by remember { mutableStateOf("Custom notification") }
    var customPriority by remember { mutableIntStateOf(NotificationCompat.PRIORITY_DEFAULT) }
    var customVisibility by remember { mutableIntStateOf(NotificationCompat.VISIBILITY_PUBLIC) }
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
    var customActionCount by remember { mutableIntStateOf(0) }
    var customAction1 by remember { mutableStateOf("Open App") }
    var customAction2 by remember { mutableStateOf("Dismiss") }
    var customAction3 by remember { mutableStateOf("More") }
    var customShowProgress by remember { mutableStateOf(false) }
    var customProgressIndeterminate by remember { mutableStateOf(true) }
    var customProgressValue by remember { mutableFloatStateOf(50f) }
    var customOngoing by remember { mutableStateOf(false) }
    var customAutoCancel by remember { mutableStateOf(true) }
    var customDelaySec by remember { mutableFloatStateOf(0f) }
    var customStyleIdx by remember { mutableIntStateOf(0) } // 0=Normal, 1=BigText, 2=Inbox

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        // SECTION 1 — Notification Demos
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.notificationDemos, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        NotifDemoCard(
            title    = "1. Simple Notification",
            subtitle = "Basic icon + text, default priority",
            icon     = Icons.Default.NotificationsNone,
        ) {
            val n = NotificationCompat.Builder(context, CH_DEFAULT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("HardwareDash")
                .setContentText("Simple notification from HardwareDash!")
                .setAutoCancel(true)
                .build()
            if (notifGranted) nm.notify(1001, n)
        }

        NotifDemoCard(
            title    = "2. Heads-Up (High Priority)",
            subtitle = "Pops up on screen even when app is in background",
            icon     = Icons.Default.NotificationImportant,
        ) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pi = PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(context, CH_HIGH)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Heads-Up Alert")
                .setContentText("This notification pops over your current screen.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setFullScreenIntent(pi, false)
                .build()
            if (notifGranted) nm.notify(1002, n)
        }

        NotifDemoCard(
            title    = "3. With Action Buttons",
            subtitle = "Expandable notification with tappable action buttons",
            icon     = Icons.Default.TouchApp,
        ) {
            val pi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(context, CH_DEFAULT)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("HardwareDash Action")
                .setContentText("Tap an action below.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("This expanded notification shows two action buttons. You can add up to 3 actions per notification using NotificationCompat.Action."))
                .addAction(android.R.drawable.ic_media_play, "Open App", pi)
                .addAction(android.R.drawable.ic_delete,     "Dismiss",  pi)
                .setAutoCancel(true)
                .build()
            if (notifGranted) nm.notify(1003, n)
        }

        NotifDemoCard(
            title    = "4. Progress Bar",
            subtitle = "Indeterminate progress spinner notification",
            icon     = Icons.Default.Downloading,
        ) {
            val n = NotificationCompat.Builder(context, CH_PROGRESS)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("Processing...")
                .setContentText("HardwareDash is working")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .build()
            if (notifGranted) nm.notify(1004, n)
        }

        NotifDemoCard(
            title    = "5. Big Picture Style",
            subtitle = "Expandable notification with an image",
            icon     = Icons.Default.Image,
        ) {
            val bm = android.graphics.BitmapFactory.decodeResource(
                context.resources, android.R.drawable.ic_menu_gallery)
            val n = NotificationCompat.Builder(context, CH_DEFAULT)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("Photo Captured!")
                .setContentText("Expand to see the preview.")
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bm))
                .setAutoCancel(true)
                .build()
            if (notifGranted) nm.notify(1005, n)
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 2 — Custom Notification Builder
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.customNotifBuilder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        OutlinedTextField(
            value = customTitle,
            onValueChange = { customTitle = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = customBody,
            onValueChange = { customBody = it },
            label = { Text("Body") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

        Text("Lock Screen Visibility", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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

        Text("Accent Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
        Text(S.lock.actionButtons, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..3).forEach { count ->
                FilterChip(
                    selected = customActionCount == count,
                    onClick = { customActionCount = count },
                    label = { Text("$count", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
        if (customActionCount >= 1) {
            OutlinedTextField(value = customAction1, onValueChange = { customAction1 = it },
                label = { Text("Action 1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        if (customActionCount >= 2) {
            OutlinedTextField(value = customAction2, onValueChange = { customAction2 = it },
                label = { Text("Action 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        if (customActionCount >= 3) {
            OutlinedTextField(value = customAction3, onValueChange = { customAction3 = it },
                label = { Text("Action 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }

        // ── Progress Bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // ── Ongoing & Auto-cancel ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(S.lock.ongoing, style = MaterialTheme.typography.labelMedium)
            Switch(checked = customOngoing, onCheckedChange = { customOngoing = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(S.lock.autoCancel, style = MaterialTheme.typography.labelMedium)
            Switch(checked = customAutoCancel, onCheckedChange = { customAutoCancel = it })
        }

        // ── Delay ────────────────────────────────────────────────────────
        SliderWithInput(
            value = customDelaySec,
            onValueChange = { customDelaySec = it },
            valueRange = 0f..30f,
            formatValue = { "%.1f".format(it) },
            suffix = "min",
            label = "${S.lock.delay}: ${if (customDelaySec < 1f) "${(customDelaySec * 60).toInt()} sec" else "${"%.0f".format(customDelaySec)} min"}",
        )

        // ── Preview Card ─────────────────────────────────────────────────
        Text(S.lock.preview, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(customTitle, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(customBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                if (customShowProgress) {
                    if (customProgressIndeterminate) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { customProgressValue / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (customActionCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (customActionCount >= 1) TextButton(onClick = {}) { Text(customAction1, style = MaterialTheme.typography.labelSmall) }
                        if (customActionCount >= 2) TextButton(onClick = {}) { Text(customAction2, style = MaterialTheme.typography.labelSmall) }
                        if (customActionCount >= 3) TextButton(onClick = {}) { Text(customAction3, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        // ── Send Button ──────────────────────────────────────────────────
        Button(
            onClick = {
                val pi = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val channel = if (customPriority >= NotificationCompat.PRIORITY_HIGH) CH_HIGH else CH_CUSTOM
                val builder = NotificationCompat.Builder(context, channel)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(customTitle)
                    .setContentText(customBody)
                    .setPriority(customPriority)
                    .setVisibility(customVisibility)
                    .setColor(colorOptions[customColorIdx].first.toArgb())
                    .setContentIntent(pi)
                    .setAutoCancel(customAutoCancel)
                    .setOngoing(customOngoing)

                // Action buttons
                if (customActionCount >= 1) builder.addAction(0, customAction1, pi)
                if (customActionCount >= 2) builder.addAction(0, customAction2, pi)
                if (customActionCount >= 3) builder.addAction(0, customAction3, pi)

                // Progress bar
                if (customShowProgress) {
                    if (customProgressIndeterminate) {
                        builder.setProgress(0, 0, true)
                    } else {
                        builder.setProgress(100, customProgressValue.toInt(), false)
                    }
                }

                // Style
                when (customStyleIdx) {
                    1 -> builder.setStyle(NotificationCompat.BigTextStyle().bigText(customBody))
                    2 -> {
                        val inboxStyle = NotificationCompat.InboxStyle()
                        customBody.lines().forEach { inboxStyle.addLine(it) }
                        builder.setStyle(inboxStyle)
                    }
                }

                val notifId = 2000 + (System.currentTimeMillis() % 1000).toInt()

                if (customDelaySec > 0 && notifGranted) {
                    // Schedule with delay
                    val delayMs = (customDelaySec * 60 * 1000).toLong()
                    val schedIntent = Intent(context, com.hardwaredash.receivers.ScheduleActionReceiver::class.java).apply {
                        action = com.hardwaredash.receivers.ScheduleActionReceiver.ACTION_FIRE
                        putExtra(com.hardwaredash.receivers.ScheduleActionReceiver.EXTRA_TYPE, "notification")
                        putExtra(com.hardwaredash.receivers.ScheduleActionReceiver.EXTRA_TITLE, customTitle)
                        putExtra(com.hardwaredash.receivers.ScheduleActionReceiver.EXTRA_BODY, customBody)
                        putExtra(com.hardwaredash.receivers.ScheduleActionReceiver.EXTRA_ID, notifId)
                    }
                    val schedPi = PendingIntent.getBroadcast(
                        context, notifId, schedIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, schedPi)
                } else if (notifGranted) {
                    nm.notify(notifId, builder.build())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = notifGranted && customTitle.isNotBlank(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null)
            Spacer(Modifier.width(8.dp))
            Text(S.lock.sendCustomNotif)
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 3 — Emergency Alerts
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.emergencyAlerts, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wireless Emergency Alerts (WEA)", fontWeight = FontWeight.SemiBold)
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
                Text("What is possible without root:", fontWeight = FontWeight.SemiBold)
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
                            "Allows HardwareDash to lock the screen programmatically."
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
            "SYSTEM_ALERT_WINDOW lets HardwareDash draw a floating window " +
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

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 9 — Lock Screen Notification Designer
        // ══════════════════════════════════════════════════════════════════════
        Text(S.lock.lockScreenDesigner, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Design notifications that appear on the lock screen. " +
            "Visibility controls how much content is shown when the device is locked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        OutlinedTextField(
            value = lsTitle,
            onValueChange = { lsTitle = it },
            label = { Text("Notification Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = lsBody,
            onValueChange = { lsBody = it },
            label = { Text("Notification Body") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        Text("Lock Screen Visibility", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(
                "Public -- Full content visible on lock screen" to NotificationCompat.VISIBILITY_PUBLIC,
                "Private -- Icon only, content hidden" to NotificationCompat.VISIBILITY_PRIVATE,
                "Secret -- Completely hidden from lock screen" to NotificationCompat.VISIBILITY_SECRET,
            ).forEach { (label, vis) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = lsVisibility == vis,
                        onClick  = { lsVisibility = vis },
                    )
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "Low" to NotificationCompat.PRIORITY_LOW,
                "Default" to NotificationCompat.PRIORITY_DEFAULT,
                "High" to NotificationCompat.PRIORITY_HIGH,
            ).forEach { (label, prio) ->
                FilterChip(
                    selected = lsPriority == prio,
                    onClick  = { lsPriority = prio },
                    label    = { Text(label) },
                )
            }
        }

        Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            listOf(
                "Message" to NotificationCompat.CATEGORY_MESSAGE,
                "Alarm" to NotificationCompat.CATEGORY_ALARM,
                "Reminder" to NotificationCompat.CATEGORY_REMINDER,
                "Event" to NotificationCompat.CATEGORY_EVENT,
            ).forEach { (label, cat) ->
                FilterChip(
                    selected = lsCategory == cat,
                    onClick  = { lsCategory = cat },
                    label    = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // ── Enhanced Scheduling ──────────────────────────────────────────────
        Text(S.lock.scheduleAction, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

        // Schedule type
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "notification" to "Notification",
                "lock" to "Lock Screen",
                "ring" to "Phone Ring",
            ).forEach { (type, label) ->
                FilterChip(
                    selected = schedType == type,
                    onClick = { schedType = type },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Custom text for notification
        if (schedType == "notification") {
            OutlinedTextField(
                value = schedTitle,
                onValueChange = { schedTitle = it },
                label = { Text("Alert text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // Date + Time pickers
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = schedDate.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
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
                label = { Text("Time") },
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

        // Quick delay slider (alternative to exact date/time)
        SliderWithInput(
            value = lsDelayMin,
            onValueChange = { lsDelayMin = it },
            valueRange = 0f..60f,
            formatValue = { "%.1f".format(it) },
            suffix = "min",
            label = "Or quick delay: ${
                if (lsDelayMin < 1f) "${(lsDelayMin * 60).toInt()} sec"
                else "${"%.1f".format(lsDelayMin)} min"
            }",
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    sendLockScreenNotification(context, nm, lsTitle, lsBody, lsVisibility, lsPriority, lsCategory)
                    lsScheduleStatus = "Sent immediately"
                },
                modifier = Modifier.weight(1f),
                enabled = lsTitle.isNotBlank(),
            ) { Text(S.lock.sendNow) }

            Button(
                onClick = {
                    val scheduledLdt = if (lsDelayMin > 0) {
                        LocalDateTime.now().plusSeconds((lsDelayMin * 60).toLong())
                    } else {
                        LocalDateTime.of(schedDate, schedTime)
                    }
                    val triggerMillis = scheduledLdt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    if (triggerMillis <= System.currentTimeMillis()) {
                        lsScheduleStatus = "Time must be in the future"
                        return@Button
                    }

                    val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                    val title = if (schedType == "notification") schedTitle else schedType.replaceFirstChar { it.uppercase() }

                    // Schedule via AlarmManager
                    val alarmIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                        action = ScheduleActionReceiver.ACTION_FIRE
                        putExtra(ScheduleActionReceiver.EXTRA_TYPE, schedType)
                        putExtra(ScheduleActionReceiver.EXTRA_TITLE, title)
                        putExtra(ScheduleActionReceiver.EXTRA_BODY, lsBody)
                        putExtra(ScheduleActionReceiver.EXTRA_ID, id)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context, id, alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)

                    // Save to schedule list
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
                    lsScheduleStatus = "Scheduled ${schedType} at ${scheduledLdt.format(formatter)}"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) { Text(S.lock.schedule) }
        }

        if (lsScheduleStatus.isNotEmpty()) {
            Text(
                lsScheduleStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // ── Schedule List ────────────────────────────────────────────────────
        if (schedList.isNotEmpty()) {
            HorizontalDivider()
            Text("Scheduled Actions (${schedList.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                            // Cancel alarm
                            val cancelIntent = Intent(context, ScheduleActionReceiver::class.java).apply {
                                action = ScheduleActionReceiver.ACTION_FIRE
                            }
                            val cancelPi = PendingIntent.getBroadcast(
                                context, itemId, cancelIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )
                            val alm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alm.cancel(cancelPi)
                            // Remove from list
                            val arr = try { JSONArray(schedPrefs.getString("actions", "[]")) } catch (_: Exception) { JSONArray() }
                            val newArr = JSONArray()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                if (obj.optInt("id", -1) != itemId) newArr.put(obj)
                            }
                            schedPrefs.edit().putString("actions", newArr.toString()).apply()
                            schedList = loadScheduleList(schedPrefs)
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // HOW IT WORKS — detail card
        // ══════════════════════════════════════════════════════════════════════
        Card(shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("How \"show over lock screen\" works", fontWeight = FontWeight.SemiBold)
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
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
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
            title = { Text("Select Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    schedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}

private fun sendLockScreenNotification(
    context: Context,
    nm: NotificationManager,
    title: String,
    body: String,
    visibility: Int,
    priority: Int,
    category: String,
) {
    val pi = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val n = NotificationCompat.Builder(context, CH_LOCKSCREEN)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setVisibility(visibility)
        .setPriority(priority)
        .setCategory(category)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .build()

    nm.notify(3000 + (System.currentTimeMillis() % 1000).toInt(), n)
}

@Composable
private fun NotifDemoCard(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onFire: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            FilledTonalButton(onClick = onFire) { Text(S.lock.send) }
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
