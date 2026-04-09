package com.hardwaredash.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hardwaredash.localization.Language
import com.hardwaredash.localization.LocalizationManager
import com.hardwaredash.localization.S
import com.hardwaredash.ui.components.SliderWithInput
import com.hardwaredash.widget.WidgetMetric
import kotlin.math.roundToInt

private const val WIDGET_PREFS = "widget_settings"
private const val KEY_RING_DURATION = "phone_ring_duration_seconds"
private const val KEY_NOTIFY_DELAY = "notify_delay_seconds"

const val DEFAULT_RING_DURATION = 30
const val DEFAULT_NOTIFY_DELAY = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val strings = S.settings

    // Language state
    val currentLang by LocalizationManager.currentLanguage

    // Widget settings from SharedPreferences
    val widgetPrefs = remember { context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE) }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        // SECTION 1 — Language
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.language,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
        )

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
        // SECTION 3 — Metric Logging
        // ══════════════════════════════════════════════════════════════════
        Text(
            strings.metricLogging,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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
                    contentDescription = null,
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

            AnimatedVisibility(visible = expanded) {
                Column {
                    metrics.forEach { metric ->
                        val prefKey = "metric_log_${metric.key}"
                        var checked by remember {
                            mutableStateOf(prefs.getBoolean(prefKey, false))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
