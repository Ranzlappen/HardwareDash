package com.hardwaredash.ui.screens

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
import com.hardwaredash.receivers.AdminReceiver

@Composable
fun LockScreenScreen() {
    val context = LocalContext.current
    val dpm     = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin   = ComponentName(context, AdminReceiver::class.java)

    var isAdmin    by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }

    // Re-check status every time the screen is composed (user may have just
    // come back from the Settings screen after granting a permission)
    LaunchedEffect(Unit) {
        isAdmin    = dpm.isAdminActive(admin)
        hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            Settings.canDrawOverlays(context) else true
    }

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
