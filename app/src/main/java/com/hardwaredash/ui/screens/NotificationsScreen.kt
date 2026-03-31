package com.hardwaredash.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.google.accompanist.permissions.*
import com.hardwaredash.MainActivity

// Channel IDs
private const val CH_DEFAULT  = "hwd_default"
private const val CH_HIGH     = "hwd_high"
private const val CH_PROGRESS = "hwd_progress"

private fun ensureChannels(nm: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    listOf(
        NotificationChannel(CH_DEFAULT,  "Default",         NotificationManager.IMPORTANCE_DEFAULT),
        NotificationChannel(CH_HIGH,     "High / Heads-Up", NotificationManager.IMPORTANCE_HIGH),
        NotificationChannel(CH_PROGRESS, "Progress",        NotificationManager.IMPORTANCE_LOW),
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
