package com.gadget.ui.screens

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gadget.localization.LocalizationManager
import com.gadget.permissions.PermissionsOnboardingCoordinator
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.root.ui.UsbDebuggingRootExtrasSection
import com.gadget.ui.components.DashCard
import com.gadget.ui.components.ResponsiveButtonText
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
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

private fun buildModeStatus(context: Context): List<Pair<String, String>> {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return listOf(
        "Ringer Mode" to when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Unknown"
        },
        "Do Not Disturb" to when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "Off"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "Total Silence"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only"
            else -> "Unknown"
        },
        "DND Policy Access" to if (nm.isNotificationPolicyAccessGranted) "Granted" else "Not Granted",
        "Battery Saver" to if (pm.isPowerSaveMode) "Active" else "Off",
        "Music Active" to if (am.isMusicActive) "Yes" else "No",
    )
}

@Composable
fun BugReportScreen() {
    val context = LocalContext.current
    val strings = S.bug

    val permissions = remember { buildPermissionList() }
    val permissionStatuses = remember { permissions.map { it.name to it.check(context) } }
    val modeStatuses = remember { buildModeStatus(context) }
    val deviceInfo = remember { buildDeviceInfo(context) }

    var bugDescription by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    ScreenAnnouncement(S.accessibility.bugReportScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
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
        DashCard(
            contentPadding = 0.dp,
            verticalArrangement = Arrangement.Top,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                ) {
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
        // SECTION 0.5 — Request all missing permissions (Batch 7)
        // ══════════════════════════════════════════════════════════════
        RequestAllMissingPermissionsButton()

        // ══════════════════════════════════════════════════════════════
        // SECTION 1 — Permission Status Table
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.permissionsTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        DashCard(
            contentPadding = 0.dp,
            verticalArrangement = Arrangement.Top,
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
        // SECTION 2 — System Modes
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.systemModesTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        DashCard(
            contentPadding = 0.dp,
            verticalArrangement = Arrangement.Top,
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        strings.modeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.5f),
                    )
                    Text(
                        strings.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.5f),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                modeStatuses.forEach { (name, value) ->
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
                            modifier = Modifier.weight(0.5f),
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (value) {
                                "Off", "Normal", "Granted", "No" -> Color(0xFF4CAF50)
                                "Active", "Silent", "Total Silence", "Not Granted" -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(0.5f),
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // SECTION 3 — Device Information
        // ══════════════════════════════════════════════════════════════
        Text(
            strings.deviceInfoTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        DashCard(
            contentPadding = 0.dp,
            verticalArrangement = Arrangement.Top,
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
            modifier = Modifier.sectionHeading(),
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
            ResponsiveButtonText(strings.createBugReport)
        }

        // ══════════════════════════════════════════════════════════════
        // SECTION 5 — Batch-9 USB Debugging root extras (rooted-only)
        // ══════════════════════════════════════════════════════════════
        UsbDebuggingRootExtrasSection()
    }

    // ── Bug Report Modal ─────────────────────────────────────────────
    if (showReportDialog) {
        val markdownReport = remember(permissionStatuses, modeStatuses, deviceInfo, bugDescription) {
            buildMarkdownReport(permissionStatuses, modeStatuses, deviceInfo, bugDescription)
        }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(strings.bugReportReady) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    OutlinedButton(
                        onClick = {
                            val mailto = Uri.parse(
                                "mailto:info@ranzlappen.com" +
                                    "?subject=" + Uri.encode(strings.title) +
                                    "&body=" + Uri.encode(markdownReport)
                            )
                            val intent = Intent(Intent.ACTION_SENDTO, mailto)
                            context.startActivity(Intent.createChooser(intent, strings.emailBugReport))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        ResponsiveButtonText(strings.emailBugReport)
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

@Composable
private fun RequestAllMissingPermissionsButton() {
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)
    var pendingSpecial by remember { mutableStateOf<List<com.gadget.permissions.SpecialPermissionStep>>(emptyList()) }
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        pendingSpecial = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
        stepIndex = 0
        status = if (pendingSpecial.isEmpty()) {
            S.PermissionsOnboarding.complete(lang)
        } else {
            S.PermissionsOnboarding.progress(lang, 1, pendingSpecial.size)
        }
        launchNextSpecialStep(context, pendingSpecial, stepIndex)
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val missing = PermissionsOnboardingCoordinator.missingRuntimePermissions(context)
                    if (missing.isEmpty()) {
                        pendingSpecial = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
                        stepIndex = 0
                        status = if (pendingSpecial.isEmpty()) {
                            S.PermissionsOnboarding.complete(lang)
                        } else {
                            S.PermissionsOnboarding.progress(lang, 1, pendingSpecial.size)
                        }
                        launchNextSpecialStep(context, pendingSpecial, stepIndex)
                    } else {
                        runtimeLauncher.launch(missing.toTypedArray())
                    }
                },
            ) {
                Text(S.PermissionsOnboarding.requestAllButton(lang))
            }
            status?.let { statusText ->
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }

    LaunchedEffect(stepIndex, pendingSpecial.size) {
        if (pendingSpecial.isNotEmpty() && stepIndex >= pendingSpecial.size) {
            status = S.PermissionsOnboarding.complete(lang)
        }
    }
}

private fun launchNextSpecialStep(
    context: Context,
    steps: List<com.gadget.permissions.SpecialPermissionStep>,
    index: Int,
) {
    if (index !in steps.indices) return
    val intent = steps[index].buildIntent(context) ?: return
    runCatching { context.startActivity(intent) }
        .onFailure {
            if (it is ActivityNotFoundException) {
                Toast.makeText(context, "Settings activity not found for ${steps[index].id}", Toast.LENGTH_SHORT).show()
            }
        }
}

private fun buildMarkdownReport(
    permissions: List<Pair<String, Boolean>>,
    modes: List<Pair<String, String>>,
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
    appendLine("## System Modes")
    appendLine("| Mode | Status |")
    appendLine("|---|---|")
    modes.forEach { (name, value) ->
        appendLine("| $name | $value |")
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
