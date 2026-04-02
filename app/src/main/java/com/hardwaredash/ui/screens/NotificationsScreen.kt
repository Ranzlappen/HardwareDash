// CHANGE: Added custom notification builder, brightness control, emergency alerts info, WiFi signal
// REASON: Enable full customization, brightness control, emergency alerts, and WiFi monitoring
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.delay

// Channel IDs
private const val CH_DEFAULT  = "hwd_default"
private const val CH_HIGH     = "hwd_high"
private const val CH_PROGRESS = "hwd_progress"
private const val CH_CUSTOM   = "hwd_custom"

private fun ensureChannels(nm: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    listOf(
        NotificationChannel(CH_DEFAULT,  "Default",         NotificationManager.IMPORTANCE_DEFAULT),
        NotificationChannel(CH_HIGH,     "High / Heads-Up", NotificationManager.IMPORTANCE_HIGH),
        NotificationChannel(CH_PROGRESS, "Progress",        NotificationManager.IMPORTANCE_LOW),
        NotificationChannel(CH_CUSTOM,   "Custom",          NotificationManager.IMPORTANCE_HIGH),
    ).forEach { nm.createNotificationChannel(it) }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val nm      = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    LaunchedEffect(Unit) { ensureChannels(nm) }

    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    else null

    val granted = notifPerm?.status?.isGranted ?: true

    // WiFi info for quick reference
    var wifiSsid    by remember { mutableStateOf("—") }
    var wifiRssi    by remember { mutableIntStateOf(0) }
    var wifiLinkSpeed by remember { mutableIntStateOf(0) }

    // Brightness
    var brightness     by remember { mutableFloatStateOf(0.5f) }
    var autoBrightness by remember { mutableStateOf(false) }
    var hasWriteSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            // WiFi
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val connInfo = wm.connectionInfo
            wifiSsid = connInfo?.ssid?.removeSurrounding("\"") ?: "—"
            wifiRssi = connInfo?.rssi ?: 0
            wifiLinkSpeed = connInfo?.linkSpeed ?: 0

            // Brightness
            hasWriteSettings = Settings.System.canWrite(context)
            if (hasWriteSettings) {
                try {
                    val curBrightness = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        128
                    )
                    brightness = curBrightness / 255f
                    val mode = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                    autoBrightness = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                } catch (_: Exception) { }
            }

            delay(2000L)
        }
    }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        if (!granted) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("POST_NOTIFICATIONS permission required (Android 13+)",
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { notifPerm?.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        // ── Demo cards ────────────────────────────────────────────────────────
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
            if (granted) nm.notify(1001, n)
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
                .setContentTitle("⚡ Heads-Up Alert")
                .setContentText("This notification pops over your current screen.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setFullScreenIntent(pi, false)
                .build()
            if (granted) nm.notify(1002, n)
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
            if (granted) nm.notify(1003, n)
        }

        NotifDemoCard(
            title    = "4. Progress Bar",
            subtitle = "Indeterminate progress spinner notification",
            icon     = Icons.Default.Downloading,
        ) {
            val n = NotificationCompat.Builder(context, CH_PROGRESS)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("Processing…")
                .setContentText("HardwareDash is working")
                .setProgress(0, 0, true)   // indeterminate
                .setOngoing(true)
                .build()
            if (granted) nm.notify(1004, n)
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
            if (granted) nm.notify(1005, n)
        }

        HorizontalDivider()

        // ── Custom Notification Builder ───────────────────────────────────────
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

        // Priority
        Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Visibility
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

        // Color picker
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
                if (granted) nm.notify(2000 + (System.currentTimeMillis() % 1000).toInt(), n)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = granted && customTitle.isNotBlank(),
        ) {
            Icon(Icons.Default.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("Send Custom Notification")
        }

        HorizontalDivider()

        // ── Emergency Alerts Info ─────────────────────────────────────────────
        Text("Emergency Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
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
                        // Open emergency alert settings
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

        // ── Brightness Control ────────────────────────────────────────────────
        Text("Display Brightness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (!hasWriteSettings) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WRITE_SETTINGS permission needed to control brightness",
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    }) { Text("Grant Write Settings") }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Auto Brightness", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = autoBrightness, onCheckedChange = { auto ->
                    autoBrightness = auto
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                })
            }

            Text("Brightness: ${"%.0f".format(brightness * 100)}%", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = brightness,
                onValueChange = { v ->
                    brightness = v
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        (v * 255).toInt(),
                    )
                },
                enabled = !autoBrightness,
            )
        }

        HorizontalDivider()

        // ── WiFi Signal Quick Reference ───────────────────────────────────────
        Text("WiFi Signal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("SSID: $wifiSsid", style = MaterialTheme.typography.bodyMedium)
                Text("RSSI: $wifiRssi dBm  ·  Link Speed: $wifiLinkSpeed Mbps",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                val signalPercent = WifiManager.calculateSignalLevel(wifiRssi, 100) / 100f
                LinearProgressIndicator(
                    progress = { signalPercent },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
        }

        HorizontalDivider()

        // ── Cancel all ────────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = { nm.cancelAll() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ClearAll, null)
            Spacer(Modifier.width(8.dp))
            Text("Cancel All Notifications")
        }
    }
}

@Composable
private fun NotifDemoCard(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onFire: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
