package com.hardwaredash.ui.screens

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.core.app.NotificationCompat
import com.google.accompanist.permissions.*
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LockScreenScreen() {
    val context = LocalContext.current
    val dpm     = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin   = ComponentName(context, AdminReceiver::class.java)
    val nm      = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val scope   = rememberCoroutineScope()

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
                "Lock Screen & Notifications",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Notification permission ──────────────────────────────────────────
        if (!notifGranted) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("POST_NOTIFICATIONS permission required (Android 13+)",
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { notifPerm?.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 1 — Notification Demos
        // ══════════════════════════════════════════════════════════════════════
        Text("Notification Demos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

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
        Text("Custom Notification Builder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

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

        Button(
            onClick = {
                val pi = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val channel = if (customPriority >= NotificationCompat.PRIORITY_HIGH) CH_HIGH else CH_CUSTOM
                val n = NotificationCompat.Builder(context, channel)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(customTitle)
                    .setContentText(customBody)
                    .setPriority(customPriority)
                    .setVisibility(customVisibility)
                    .setColor(colorOptions[customColorIdx].first.toArgb())
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
                if (notifGranted) nm.notify(2000 + (System.currentTimeMillis() % 1000).toInt(), n)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = notifGranted && customTitle.isNotBlank(),
        ) {
            Icon(Icons.Default.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("Send Custom Notification")
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 3 — Emergency Alerts
        // ══════════════════════════════════════════════════════════════════════
        Text("Emergency Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    Text("Open Emergency Alert Settings")
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
            Text("Cancel All Notifications")
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 5 — Capability overview
        // ══════════════════════════════════════════════════════════════════════
        Text("Lock Screen Capabilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
        Text("Device Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Text("Activate Device Admin")
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
                Text("Deactivate Device Admin")
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 7 — Overlay permission
        // ══════════════════════════════════════════════════════════════════════
        Text("Overlay Permission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Text("Open Overlay Permission Settings")
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // SECTION 8 — Actions
        // ══════════════════════════════════════════════════════════════════════
        Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Button(
            enabled  = isAdmin,
            onClick  = { dpm.lockNow() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(Icons.Default.LockClock, null)
            Spacer(Modifier.width(8.dp))
            Text("Lock Screen Now", style = MaterialTheme.typography.titleMedium)
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
        Text("Lock Screen Notification Designer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        // Scheduling
        Text("Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Delay: ${
                if (lsDelayMin < 1f) "${(lsDelayMin * 60).toInt()} sec"
                else "${"%.1f".format(lsDelayMin)} min"
            }",
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = lsDelayMin,
            onValueChange = { lsDelayMin = it },
            valueRange = 0f..60f,
        )

        // Send buttons
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
            ) { Text("Send Now") }

            Button(
                onClick = {
                    val delayMs = (lsDelayMin * 60 * 1000).toLong()
                    if (delayMs <= 0) {
                        sendLockScreenNotification(context, nm, lsTitle, lsBody, lsVisibility, lsPriority, lsCategory)
                        lsScheduleStatus = "Sent immediately"
                    } else {
                        val label = if (lsDelayMin < 1f) "${(lsDelayMin * 60).toInt()}s"
                                    else "${"%.1f".format(lsDelayMin)}m"
                        lsScheduleStatus = "Scheduled in $label..."
                        val t = lsTitle; val b = lsBody; val v = lsVisibility
                        val p = lsPriority; val c = lsCategory
                        scope.launch {
                            delay(delayMs)
                            sendLockScreenNotification(context, nm, t, b, v, p, c)
                            lsScheduleStatus = "Scheduled notification sent"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = lsTitle.isNotBlank() && lsDelayMin > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) { Text("Schedule") }
        }

        if (lsScheduleStatus.isNotEmpty()) {
            Text(
                lsScheduleStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
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
            FilledTonalButton(onClick = onFire) { Text("Send") }
        }
    }
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
