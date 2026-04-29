package com.gadget.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.backup.BackupManager
import com.gadget.localization.Language
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.SliderWithInput
import com.gadget.ui.components.sectionHeading
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.LocalAccessibilityPreferences
import com.gadget.widget.WidgetMetric
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupManagerEntryPoint {
    fun backupManager(): BackupManager
}

private const val WIDGET_PREFS = "widget_settings"
private const val KEY_RING_DELAY = "phone_ring_delay_seconds"
private const val KEY_RING_DURATION = "phone_ring_duration_seconds"
private const val KEY_NOTIFY_DELAY = "notify_delay_seconds"

const val DEFAULT_RING_DELAY = 0
const val DEFAULT_RING_DURATION = 30
const val DEFAULT_NOTIFY_DELAY = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val strings = S.settings
    val backupStrings = S.backup
    val accessibilityPrefs = LocalAccessibilityPreferences.current
    val coroutineScope = rememberCoroutineScope()

    ScreenAnnouncement(S.accessibility.settingsScreen)

    // BackupManager via Hilt entry point
    val backupManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupManagerEntryPoint::class.java,
        ).backupManager()
    }

    // SAF launchers for backup/restore
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupManager.createBackup(outputStream)
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(context, backupStrings.backupSuccess, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, backupStrings.backupFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    backupManager.restoreBackup(inputStream)
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(context, backupStrings.restoreSuccess, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(context, backupStrings.restoreFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Language state
    val currentLang by LocalizationManager.currentLanguage

    // Widget settings from SharedPreferences
    val widgetPrefs = remember { context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE) }
    var ringDelay by remember {
        mutableFloatStateOf(widgetPrefs.getInt(KEY_RING_DELAY, DEFAULT_RING_DELAY).toFloat())
    }
    var ringDuration by remember {
        mutableFloatStateOf(widgetPrefs.getInt(KEY_RING_DURATION, DEFAULT_RING_DURATION).toFloat())
    }
    var notifyDelay by remember {
        mutableFloatStateOf(widgetPrefs.getInt(KEY_NOTIFY_DELAY, DEFAULT_NOTIFY_DELAY).toFloat())
    }

    // Language dropdown expanded state
    var langExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(
                Icons.Default.Settings, null,
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

        // ══════════════════════════════════════════════════════════════════
        // SECTION 0 — Onboarding
        // ══════════════════════════════════════════════════════════════════
        OutlinedButton(
            onClick = {
                context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("has_seen_onboarding", false)
                    .apply()
                Toast.makeText(context, "Restart the app to see onboarding", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(S.onboarding.showOnboarding)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ══════════════════════════════════════════════════════════════════
        // SECTION 1 — Language
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.language,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )
        Text(
            strings.languageDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = { langExpanded = it },
        ) {
            OutlinedTextField(
                value = currentLang.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false },
            ) {
                Language.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(lang.displayName)
                                if (lang == currentLang) {
                                    Icon(
                                        Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        onClick = {
                            LocalizationManager.setLanguage(context, lang)
                            langExpanded = false
                        },
                    )
                }
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 2 — Widget Customizer
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.widgetCustomizer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        // ── Phone Ring Delay ─────────────────────────────────────────────
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
                        Icons.Default.HourglassBottom, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.phoneRingDelay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.phoneRingDelayDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                SliderWithInput(
                    value = ringDelay,
                    onValueChange = { ringDelay = it },
                    onValueChangeFinished = {
                        widgetPrefs.edit().putInt(KEY_RING_DELAY, ringDelay.roundToInt()).apply()
                    },
                    valueRange = 0f..300f,
                    formatValue = { "%.0f".format(it) },
                    suffix = "s",
                    label = "${ringDelay.roundToInt()} ${strings.seconds}",
                )
            }
        }

        // ── Phone Ring Duration ──────────────────────────────────────────
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
                        Icons.Default.PhoneInTalk, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.phoneRingDuration,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.phoneRingDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                SliderWithInput(
                    value = ringDuration,
                    onValueChange = { ringDuration = it },
                    onValueChangeFinished = {
                        widgetPrefs.edit().putInt(KEY_RING_DURATION, ringDuration.roundToInt()).apply()
                    },
                    valueRange = 5f..120f,
                    formatValue = { "%.0f".format(it) },
                    suffix = "s",
                    label = "${ringDuration.roundToInt()} ${strings.seconds}",
                )
            }
        }

        // ── Notify Delay ─────────────────────────────────────────────────
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
                        Icons.Default.Notifications, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        strings.notifyDelay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    strings.notifyDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                SliderWithInput(
                    value = notifyDelay,
                    onValueChange = { notifyDelay = it },
                    onValueChangeFinished = {
                        widgetPrefs.edit().putInt(KEY_NOTIFY_DELAY, notifyDelay.roundToInt()).apply()
                    },
                    valueRange = 5f..300f,
                    formatValue = { "%.0f".format(it) },
                    suffix = "s",
                    label = "${notifyDelay.roundToInt()} ${strings.seconds}",
                )
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 3 — DND Bypass
        // ══════════════════════════════════════════════════════════════════
        var bypassDnd by remember {
            mutableStateOf(widgetPrefs.getBoolean("bypass_dnd", false))
        }
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.bypassDnd,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.sectionHeading(),
                    )
                    Text(
                        strings.bypassDndDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = bypassDnd,
                    onCheckedChange = {
                        bypassDnd = it
                        widgetPrefs.edit().putBoolean("bypass_dnd", it).apply()
                    },
                )
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION — Accessibility
        // ══════════════════════════════════════════════════════════════════
        Text(
            S.accessibility.accessibilityTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        // High Contrast
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        S.accessibility.highContrast,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        S.accessibility.highContrastDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = accessibilityPrefs.highContrast,
                    onCheckedChange = {
                        AccessibilityPreferencesManager.setHighContrast(context, it)
                    },
                )
            }
        }

        // Large Text
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        S.accessibility.largeText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        S.accessibility.largeTextDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = accessibilityPrefs.largeText,
                    onCheckedChange = {
                        AccessibilityPreferencesManager.setLargeText(context, it)
                    },
                )
            }
        }

        // Reduced Motion
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        S.accessibility.reducedMotion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        S.accessibility.reducedMotionDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = accessibilityPrefs.reducedMotion,
                    onCheckedChange = {
                        AccessibilityPreferencesManager.setReducedMotion(context, it)
                    },
                )
            }
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 4 — Metric Logging
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.metricLogging,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )
        Text(
            strings.metricLoggingDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        val grouped = remember { WidgetMetric.grouped() }
        grouped.forEach { (category, metrics) ->
            MetricCategoryCard(
                category = category,
                metrics = metrics,
                prefs = widgetPrefs,
            )
        }

        HorizontalDivider()

        // ══════════════════════════════════════════════════════════════════
        // SECTION 5 — Backup & Restore
        // ══════════════════════════════════════════════════════════════════
        Text(
            backupStrings.backup + " & " + backupStrings.restore,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.sectionHeading(),
        )

        // Backup button
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
                        Icons.Default.CloudUpload, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        backupStrings.backup,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    backupStrings.backupDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                FilledTonalButton(
                    onClick = {
                        backupLauncher.launch("gadget_backup.zip")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(backupStrings.backup)
                }
            }
        }

        // Restore button
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
                        Icons.Default.CloudDownload, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        backupStrings.restore,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    backupStrings.restoreDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
                OutlinedButton(
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(backupStrings.restore)
                }
            }
        }
    }
}

@Composable
private fun MetricCategoryCard(
    category: String,
    metrics: List<WidgetMetric>,
    prefs: android.content.SharedPreferences,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category header (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) S.accessibility.collapseSection else S.accessibility.expandSection,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                // Show count of enabled metrics
                val enabledCount = metrics.count {
                    prefs.getBoolean("metric_log_${it.key}", false)
                }
                if (enabledCount > 0) {
                    Text(
                        "$enabledCount/${metrics.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val reducedMotion = LocalAccessibilityPreferences.current.reducedMotion
            AnimatedVisibility(
                visible = expanded,
                enter = if (reducedMotion) EnterTransition.None else expandVertically() + fadeIn(),
                exit = if (reducedMotion) ExitTransition.None else shrinkVertically() + fadeOut(),
            ) {
                Column {
                    metrics.forEach { metric ->
                        val prefKey = "metric_log_${metric.key}"
                        var checked by remember {
                            mutableStateOf(prefs.getBoolean(prefKey, false))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) { }
                                .clickable {
                                    checked = !checked
                                    prefs.edit().putBoolean(prefKey, checked).apply()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    checked = value
                                    prefs.edit().putBoolean(prefKey, value).apply()
                                },
                            )
                            Column {
                                Text(
                                    metric.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (metric.unit.isNotBlank()) {
                                    Text(
                                        metric.unit,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
