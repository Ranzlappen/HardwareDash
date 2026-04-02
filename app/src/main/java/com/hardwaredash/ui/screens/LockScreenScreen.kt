// CHANGE: Added lock screen notification designer with visibility, priority, category, and scheduling
// REASON: Make lock screen notification panels fully designable and customizable with timing
// DATE: 2026-04-02

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.hardwaredash.MainActivity
import com.hardwaredash.receivers.AdminReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CH_LOCKSCREEN = "hwd_lockscreen"

private fun ensureLockChannel(nm: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CH_LOCKSCREEN, "Lock Screen Notifications", NotificationManager.IMPORTANCE_HIGH
    ).apply {
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }
    nm.createNotificationChannel(channel)
}

@Composable
fun LockScreenScreen() {
    val context = LocalContext.current
    val dpm     = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin   = ComponentName(context, AdminReceiver::class.java)
    val nm      = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    val scope   = rememberCoroutineScope()

    LaunchedEffect(Unit) { ensureLockChannel(nm) }

    var isAdmin    by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }

    // Re-check status every time the screen is composed
    LaunchedEffect(Unit) {
        isAdmin    = dpm.isAdminActive(admin)
        hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(context) else true
    }

    // Lock screen notification designer state
    var lsTitle     by remember { mutableStateOf("Lock Screen Alert") }
    var lsBody      by remember { mutableStateOf("Custom lock screen notification") }
    var lsVisibility by remember { mutableIntStateOf(NotificationCompat.VISIBILITY_PUBLIC) }
    var lsPriority  by remember { mutableIntStateOf(NotificationCompat.PRIORITY_HIGH) }
    var lsCategory  by remember { mutableStateOf(NotificationCompat.CATEGORY_MESSAGE) }
    var lsDelayMin  by remember { mutableFloatStateOf(0f) }
    var lsScheduleStatus by remember { mutableStateOf("") }

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
                "Lock Screen Control",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Capability overview ───────────────────────────────────────────────
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("What is possible without root:", fontWeight = FontWeight.SemiBold)
                listOf(
                    "✅  Lock the screen (Device Admin)",
                    "✅  Show this Activity over the lock screen",
                    "✅  Draw a persistent overlay on the lock screen",
                    "✅  Turn the screen on programmatically",
                    "✅  Design custom lock screen notifications",
                    "✅  Schedule timed lock screen notifications",
                    "❌  Replace / fully customise the lock screen UI",
                    "❌  Permanently disable the lock screen",
                    "❌  Read or bypass the PIN / password",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // STEP 1 — Device Admin
        // ══════════════════════════════════════════════════════════════════════
        Text("Step 1 — Activate Device Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
        // STEP 2 — Overlay permission
        // ══════════════════════════════════════════════════════════════════════
        Text("Step 2 — Overlay Permission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "SYSTEM_ALERT_WINDOW lets HardwareDash draw a floating window " +
            "directly on top of the lock screen.\n\n" +
            "⚠️  Xiaomi HyperOS: Settings → Apps → HardwareDash → " +
            "Other permissions → Display over other apps\n" +
            "⚠️  Huawei EMUI: Settings → Apps → HardwareDash → Permissions → " +
            "Float on top",
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
        // STEP 3 — Actions
        // ══════════════════════════════════════════════════════════════════════
        Text("Step 3 — Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        // Lock now
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
                "Complete Step 1 first to enable screen locking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // STEP 4 — Lock Screen Notification Designer
        // ══════════════════════════════════════════════════════════════════════
        Text("Step 4 — Lock Screen Notification Designer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        // Visibility
        Text("Lock Screen Visibility", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(
                "Public — Full content visible on lock screen" to NotificationCompat.VISIBILITY_PUBLIC,
                "Private — Icon only, content hidden" to NotificationCompat.VISIBILITY_PRIVATE,
                "Secret — Completely hidden from lock screen" to NotificationCompat.VISIBILITY_SECRET,
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

        // Priority
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

        // Category
        Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "Message" to NotificationCompat.CATEGORY_MESSAGE,
                "Alarm" to NotificationCompat.CATEGORY_ALARM,
                "Reminder" to NotificationCompat.CATEGORY_REMINDER,
                "Event" to NotificationCompat.CATEGORY_EVENT,
            ).forEach { (label, cat) ->
                FilterChip(
                    selected = lsCategory == cat,
                    onClick  = { lsCategory = cat },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
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
                    lsScheduleStatus = "✓ Sent immediately"
                },
                modifier = Modifier.weight(1f),
                enabled = lsTitle.isNotBlank(),
            ) { Text("Send Now") }

            Button(
                onClick = {
                    val delayMs = (lsDelayMin * 60 * 1000).toLong()
                    if (delayMs <= 0) {
                        sendLockScreenNotification(context, nm, lsTitle, lsBody, lsVisibility, lsPriority, lsCategory)
                        lsScheduleStatus = "✓ Sent immediately"
                    } else {
                        val label = if (lsDelayMin < 1f) "${(lsDelayMin * 60).toInt()}s"
                                    else "${"%.1f".format(lsDelayMin)}m"
                        lsScheduleStatus = "⏱ Scheduled in $label…"
                        // Capture values for the coroutine
                        val t = lsTitle; val b = lsBody; val v = lsVisibility
                        val p = lsPriority; val c = lsCategory
                        scope.launch {
                            delay(delayMs)
                            sendLockScreenNotification(context, nm, t, b, v, p, c)
                            lsScheduleStatus = "✓ Scheduled notification sent"
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
                color = if (lsScheduleStatus.startsWith("✓")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
            )
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════════
        // HOW IT WORKS — detail card
        // ══════════════════════════════════════════════════════════════════════
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
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

// ─── Small status badge row ───────────────────────────────────────────────────
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
                if (granted) "✓  Granted" else "✗  Not granted",
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
