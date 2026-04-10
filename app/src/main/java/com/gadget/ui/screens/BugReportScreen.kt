package com.gadget.ui.screens

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gadget.localization.S
import com.gadget.receivers.AdminReceiver

// ─── Permission descriptor ──────────────────────────────────────────────────
private data class PermissionEntry(
    val name: String,
    val check: (Context) -> Boolean,
)

private fun buildPermissionList(): List<PermissionEntry> = listOf(
    // Runtime permissions
    PermissionEntry("CAMERA") { ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("RECORD_AUDIO") { ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("ACCESS_FINE_LOCATION") { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("ACCESS_COARSE_LOCATION") { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("POST_NOTIFICATIONS") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(it, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
    },
    PermissionEntry("BLUETOOTH_CONNECT") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        else true
    },
    PermissionEntry("BLUETOOTH_SCAN") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ContextCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        else true
    },
    PermissionEntry("READ_PHONE_STATE") { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("ACTIVITY_RECOGNITION") { ContextCompat.checkSelfPermission(it, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("VIBRATE") { ContextCompat.checkSelfPermission(it, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("INTERNET") { ContextCompat.checkSelfPermission(it, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("ACCESS_WIFI_STATE") { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("ACCESS_NETWORK_STATE") { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("NFC") { ContextCompat.checkSelfPermission(it, Manifest.permission.NFC) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("FOREGROUND_SERVICE") { ContextCompat.checkSelfPermission(it, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED },
    PermissionEntry("SCHEDULE_EXACT_ALARM") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = it.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.canScheduleExactAlarms()
        } else true
    },
    // Special permissions
    PermissionEntry("Draw Over Apps (SYSTEM_ALERT_WINDOW)") { Settings.canDrawOverlays(it) },
    PermissionEntry("Device Admin") {
        val dpm = it.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        dpm.isAdminActive(ComponentName(it, AdminReceiver::class.java))
    },
    PermissionEntry("Write Settings") { Settings.System.canWrite(it) },
    PermissionEntry("Notifications Enabled") { NotificationManagerCompat.from(it).areNotificationsEnabled() },
)

// ─── Device info descriptor ─────────────────────────────────────────────────
private fun buildDeviceInfo(context: Context): List<Pair<String, String>> {
    val dm = context.resources.displayMetrics
    val appVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pInfo.versionName} (${pInfo.longVersionCode})"
    } catch (_: Exception) { "unknown" }

    return listOf(
        "Device Model" to Build.MODEL,
        "Manufacturer" to Build.MANUFACTURER,
        "Brand" to Build.BRAND,
        "Device" to Build.DEVICE,
        "Hardware" to Build.HARDWARE,
        "Android Version" to Build.VERSION.RELEASE,
        "API Level" to Build.VERSION.SDK_INT.toString(),
        "Build Display" to Build.DISPLAY,
        "Screen Resolution" to "${dm.widthPixels} x ${dm.heightPixels}",
        "Screen Density" to "${dm.densityDpi} dpi (${dm.density}x)",
        "Gadget Version" to appVersion,
    )
}

@Composable
fun BugReportScreen() {
    val context = LocalContext.current
    val strings = S.bug

    val permissions = remember { buildPermissionList() }
    val permissionStatuses = remember { permissions.map { it.name to it.check(context) } }
    val deviceInfo = remember { buildDeviceInfo(context) }

    var bugDescription by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.BugReport, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                strings.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ══════════════════════════════════════════════════════════════
        // DISCLAIMER
        // ══════════════════════════════════════════════════════════════
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.disclaimerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.disclaimerBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ══════════════════════════════════════════════════════════════
        // SECTION 1 — Permission Status Table
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.permissionsTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        strings.permissionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.65f),
                    )
                    Text(
                        strings.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.35f),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                permissionStatuses.forEach { (name, granted) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.65f),
                        )
                        Text(
                            if (granted) strings.granted else strings.notGranted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (granted) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.weight(0.35f),
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // SECTION 2 — Device Information
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.deviceInfoTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                deviceInfo.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // SECTION 3 — Bug Description
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.describeBug,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = bugDescription,
            onValueChange = { bugDescription = it },
            placeholder = { Text(strings.describeBugHint) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp),
            maxLines = 12,
        )

        // ══════════════════════════════════════════════════════════════
        // SECTION 4 — Create Bug Report Button
        // ══════════════════════════════════════════════════════════════
        Button(
            onClick = { showReportDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.BugReport, null)
            Spacer(Modifier.width(8.dp))
            Text(strings.createBugReport)
        }
    }

    // ── Bug Report Modal ─────────────────────────────────────────────
    if (showReportDialog) {
        val markdownReport = remember(permissionStatuses, deviceInfo, bugDescription) {
            buildMarkdownReport(permissionStatuses, deviceInfo, bugDescription)
        }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(strings.bugReportReady) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    // GitHub link
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ranzlappen/gadget/issues"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.openGithubIssue)
                    }

                    // Markdown report text field
                    OutlinedTextField(
                        value = markdownReport,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        maxLines = 30,
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Bug Report", markdownReport))
                        Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.copyText)
                    }
                    TextButton(onClick = { showReportDialog = false }) {
                        Text(S.common.close)
                    }
                }
            },
        )
    }
}

private fun buildMarkdownReport(
    permissions: List<Pair<String, Boolean>>,
    deviceInfo: List<Pair<String, String>>,
    description: String,
): String = buildString {
    appendLine("## Permissions")
    appendLine("| Permission | Status |")
    appendLine("|---|---|")
    permissions.forEach { (name, granted) ->
        appendLine("| $name | ${if (granted) "Granted" else "Not Granted"} |")
    }
    appendLine()
    appendLine("## Device Info")
    appendLine("| Property | Value |")
    appendLine("|---|---|")
    deviceInfo.forEach { (label, value) ->
        appendLine("| $label | $value |")
    }
    appendLine()
    if (description.isNotBlank()) {
        appendLine("## Bug Description")
        appendLine(description)
    }
}
