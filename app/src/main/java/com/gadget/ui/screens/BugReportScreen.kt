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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gadget.permissions.PermissionsOnboardingCoordinator
import com.gadget.permissions.SpecialPermissionStep
import com.gadget.permissions.rememberPermissionsResumeAdvancer
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
import com.gadget.root.ui.DiagnosticsRootExtrasSection
import com.gadget.root.ui.UsbDebuggingRootExtrasSection
import com.gadget.ui.components.DashCard
import com.gadget.ui.components.ResponsiveButtonText
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import com.gadget.receivers.AdminReceiver

// ─── Permission descriptor ──────────────────────────────────────────────────
//
// Each row knows how to (1) check its own grant state and (2) build an Intent
// that takes the user to the matching Android Settings page. `openSettings`
// returns null for normal (auto-granted) permissions where there is nothing
// for the user to toggle — those rows render without a button.
private data class PermissionRow(
    val displayName: String,
    val isGranted: (Context) -> Boolean,
    val openSettings: (Context) -> Intent?,
)

private fun appDetailsIntent(ctx: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${ctx.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun runtimeRow(
    displayName: String,
    permission: String,
    minSdk: Int? = null,
): PermissionRow = PermissionRow(
    displayName = displayName,
    isGranted = { ctx ->
        if (minSdk != null && Build.VERSION.SDK_INT < minSdk) {
            true
        } else {
            ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
        }
    },
    openSettings = { ctx ->
        if (minSdk != null && Build.VERSION.SDK_INT < minSdk) null else appDetailsIntent(ctx)
    },
)

private fun normalRow(displayName: String, permission: String): PermissionRow = PermissionRow(
    displayName = displayName,
    isGranted = { ctx ->
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
    },
    openSettings = { null }, // normal perms are auto-granted; nothing to open
)

private fun buildPermissionList(): List<PermissionRow> = listOf(
    // ── Runtime / dangerous permissions ─────────────────────────────────
    runtimeRow("CAMERA", Manifest.permission.CAMERA),
    runtimeRow("RECORD_AUDIO", Manifest.permission.RECORD_AUDIO),
    runtimeRow("ACCESS_FINE_LOCATION", Manifest.permission.ACCESS_FINE_LOCATION),
    runtimeRow("ACCESS_COARSE_LOCATION", Manifest.permission.ACCESS_COARSE_LOCATION),
    runtimeRow("ACCESS_BACKGROUND_LOCATION", Manifest.permission.ACCESS_BACKGROUND_LOCATION),
    runtimeRow("POST_NOTIFICATIONS", Manifest.permission.POST_NOTIFICATIONS, Build.VERSION_CODES.TIRAMISU),
    runtimeRow("BLUETOOTH_CONNECT", Manifest.permission.BLUETOOTH_CONNECT, Build.VERSION_CODES.S),
    runtimeRow("BLUETOOTH_SCAN", Manifest.permission.BLUETOOTH_SCAN, Build.VERSION_CODES.S),
    runtimeRow("READ_PHONE_STATE", Manifest.permission.READ_PHONE_STATE),
    runtimeRow("ACTIVITY_RECOGNITION", Manifest.permission.ACTIVITY_RECOGNITION),
    runtimeRow("CHANGE_WIFI_STATE", Manifest.permission.CHANGE_WIFI_STATE),

    // ── Special-access permissions (per-row Settings page) ──────────────
    PermissionRow(
        displayName = "DND Policy Access",
        isGranted = { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.isNotificationPolicyAccessGranted
        },
        openSettings = { _ ->
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Battery Optimizations Ignored",
        isGranted = { ctx ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                true
            } else {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(ctx.packageName)
            }
        },
        openSettings = { ctx ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                null
            } else {
                @Suppress("BatteryLife")
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        },
    ),
    PermissionRow(
        displayName = "Draw Over Apps",
        isGranted = { ctx -> Settings.canDrawOverlays(ctx) },
        openSettings = { ctx ->
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .setData(Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Device Admin",
        isGranted = { ctx ->
            val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.isAdminActive(ComponentName(ctx, AdminReceiver::class.java))
        },
        openSettings = { ctx ->
            Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    ComponentName(ctx, AdminReceiver::class.java),
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Write Settings",
        isGranted = { ctx -> Settings.System.canWrite(ctx) },
        openSettings = { ctx ->
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Schedule Exact Alarm",
        isGranted = { ctx ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                true
            } else {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                am.canScheduleExactAlarms()
            }
        },
        openSettings = { ctx ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                null
            } else {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        },
    ),
    PermissionRow(
        displayName = "Usage Access",
        isGranted = { ctx -> SpecialPermissionStep.hasUsageAccess(ctx) },
        openSettings = { ctx ->
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Mock Location (Dev Options)",
        isGranted = { ctx -> SpecialPermissionStep.hasMockLocationAccess(ctx) },
        openSettings = { _ ->
            // Mock Location app is selected in Developer Options. Opening
            // dev options directly fails if Dev Options is disabled — the
            // ActivityNotFoundException branch in the row's onClick surfaces
            // a toast in that case.
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),
    PermissionRow(
        displayName = "Notifications Enabled",
        isGranted = { ctx -> NotificationManagerCompat.from(ctx).areNotificationsEnabled() },
        openSettings = { ctx ->
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    ),

    // ── Normal / auto-granted permissions (status only; no button) ──────
    normalRow("VIBRATE", Manifest.permission.VIBRATE),
    normalRow("INTERNET", Manifest.permission.INTERNET),
    normalRow("ACCESS_WIFI_STATE", Manifest.permission.ACCESS_WIFI_STATE),
    normalRow("ACCESS_NETWORK_STATE", Manifest.permission.ACCESS_NETWORK_STATE),
    normalRow("NFC", Manifest.permission.NFC),
    normalRow("FOREGROUND_SERVICE", Manifest.permission.FOREGROUND_SERVICE),
    normalRow("WAKE_LOCK", Manifest.permission.WAKE_LOCK),
    normalRow("RECEIVE_BOOT_COMPLETED", Manifest.permission.RECEIVE_BOOT_COMPLETED),
    normalRow("DISABLE_KEYGUARD", Manifest.permission.DISABLE_KEYGUARD),
    normalRow("ACCESS_NOTIFICATION_POLICY", Manifest.permission.ACCESS_NOTIFICATION_POLICY),
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
        "Battery Saver" to if (pm.isPowerSaveMode) "Active" else "Off",
        "Music Active" to if (am.isMusicActive) "Yes" else "No",
    )
}

@Composable
fun BugReportScreen() {
    val context = LocalContext.current
    val strings = S.bug

    val permissions = remember { buildPermissionList() }
    // Live status snapshot — refreshed on every ON_RESUME so toggling a
    // permission in Settings and returning flips the green/red badge.
    var permissionStatuses by remember {
        mutableStateOf(permissions.map { it.displayName to it.isGranted(context) })
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatuses = permissions.map { it.displayName to it.isGranted(context) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        strings.permissionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.55f),
                    )
                    Text(
                        strings.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.25f),
                    )
                    Spacer(Modifier.weight(0.20f))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                permissions.forEachIndexed { index, row ->
                    val granted = permissionStatuses.getOrNull(index)?.second ?: row.isGranted(context)
                    val intent = remember(row) { row.openSettings(context) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.55f),
                        )
                        Text(
                            if (granted) strings.granted else strings.notGranted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (granted) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.weight(0.25f),
                        )
                        if (intent != null) {
                            TextButton(
                                onClick = {
                                    runCatching { context.startActivity(intent) }
                                        .onFailure {
                                            if (it is ActivityNotFoundException) {
                                                Toast.makeText(
                                                    context,
                                                    strings.settingsPageUnavailable,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.weight(0.20f),
                            ) {
                                Text(
                                    strings.openSettings,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        } else {
                            Spacer(Modifier.weight(0.20f))
                        }
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

        // ══════════════════════════════════════════════════════════════
        // SECTION 6 — Batch-10 Diagnostics root extras (rooted-only)
        // ══════════════════════════════════════════════════════════════
        DiagnosticsRootExtrasSection()
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
    var pendingSpecial by remember { mutableStateOf<List<SpecialPermissionStep>>(emptyList()) }
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    val awaitingResume = rememberPermissionsResumeAdvancer(onResume = {
        val refreshed = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
        pendingSpecial = refreshed
        if (refreshed.isEmpty()) {
            status = S.PermissionsOnboarding.complete(lang)
        } else {
            stepIndex = 0
            status = S.PermissionsOnboarding.progress(lang, 1, refreshed.size)
            launchNextSpecialStep(context, refreshed, 0)
        }
    })

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
        if (pendingSpecial.isNotEmpty()) {
            launchNextSpecialStep(context, pendingSpecial, stepIndex)
            awaitingResume.value = true
        }
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
                        if (pendingSpecial.isNotEmpty()) {
                            launchNextSpecialStep(context, pendingSpecial, stepIndex)
                            awaitingResume.value = true
                        }
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
    steps: List<SpecialPermissionStep>,
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
