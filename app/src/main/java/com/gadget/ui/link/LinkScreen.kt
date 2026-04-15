package com.gadget.ui.link

import android.content.Context
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.services.LinkService
import com.gadget.ui.components.SliderWithInput
import com.gadget.widget.WidgetMetric

// ─── Preferences key ────────────────────────────────────────────────────────
private const val PREFS_NAME = "link_rules"
private const val KEY_RULES = "rules"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen() {
    val context = LocalContext.current
    val strings = S.link
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ── State ────────────────────────────────────────────────────────────
    var rules by remember {
        mutableStateOf(loadRules(prefs.getString(KEY_RULES, "") ?: ""))
    }
    var editingRule by remember { mutableStateOf<LinkRule?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var serviceRunning by remember { mutableStateOf(LinkService.isRunning) }
    var deleteTarget by remember { mutableStateOf<LinkRule?>(null) }

    fun persist(updated: List<LinkRule>) {
        rules = updated
        prefs.edit().putString(KEY_RULES, saveRules(updated)).apply()
    }

    ScreenAnnouncement(S.accessibility.linkScreen)

    // ── UI ───────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                strings.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // Monitoring toggle
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (serviceRunning) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        strings.monitoring,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (serviceRunning) strings.stopMonitoring else strings.startMonitoring,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = serviceRunning,
                    onCheckedChange = {
                        LinkService.toggle(context)
                        serviceRunning = LinkService.isRunning
                    },
                )
            }
        }

        // Add button
        OutlinedButton(
            onClick = {
                editingRule = LinkRule()
                showEditor = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.addLink)
        }

        // Rule list
        if (rules.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        strings.noLinksYet,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        rules.forEach { rule ->
            LinkRuleCard(
                rule = rule,
                onToggle = { enabled ->
                    persist(rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it })
                },
                onEdit = {
                    editingRule = rule
                    showEditor = true
                },
                onDelete = { deleteTarget = rule },
            )
        }

        // ── Statistics overview ──────────────────────────────────────────
        if (rules.isNotEmpty()) {
            val statsJson = remember { prefs.getString("link_stats", "") ?: "" }
            val stats = remember(statsJson) { loadLinkStats(statsJson) }
            val hasAnyStats = stats.values.any { it.triggerCount > 0 || it.cooldownBlockCount > 0 }

            if (hasAnyStats) {
                Text(
                    strings.statsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                rules.forEach { rule ->
                    val s = stats[rule.id] ?: return@forEach
                    if (s.triggerCount == 0 && s.cooldownBlockCount == 0) return@forEach
                    val metricName = WidgetMetric.fromKey(rule.metricKey)?.displayName ?: rule.metricKey

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                rule.name.ifBlank { metricName },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        strings.triggered,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${s.triggerCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column {
                                    Text(
                                        strings.cooldownBlocked,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${s.cooldownBlockCount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            if (s.lastTriggeredIso.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${strings.lastTriggered}: ${formatIso(s.lastTriggeredIso)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Reset stats button
                OutlinedButton(
                    onClick = {
                        prefs.edit().putString("link_stats", "").apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.resetStats)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // ── Editor dialog ────────────────────────────────────────────────────
    if (showEditor && editingRule != null) {
        LinkEditorDialog(
            rule = editingRule!!,
            strings = strings,
            onDismiss = {
                showEditor = false
                editingRule = null
            },
            onSave = { saved ->
                val existing = rules.indexOfFirst { it.id == saved.id }
                val updated = if (existing >= 0) {
                    rules.toMutableList().also { it[existing] = saved }
                } else {
                    rules + saved
                }
                persist(updated)
                showEditor = false
                editingRule = null
            },
        )
    }

    // ── Delete confirmation ──────────────────────────────────────────────
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.deleteLink) },
            text = { Text(strings.deleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    persist(rules.filter { it.id != deleteTarget!!.id })
                    deleteTarget = null
                }) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(strings.cancel)
                }
            },
        )
    }
}

// ─── Rule card ──────────────────────────────────────────────────────────────
@Composable
private fun LinkRuleCard(
    rule: LinkRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val metricName = WidgetMetric.fromKey(rule.metricKey)?.displayName ?: rule.metricKey
    val op = LinkOperator.fromKey(rule.operator)
    val actionLabel = LinkActionType.fromKey(rule.actionType).label

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    rule.name.ifBlank { "Link Rule" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }

            Spacer(Modifier.height(8.dp))

            // IF summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "IF ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (op.isRange)
                        "$metricName ${op.symbol} ${rule.threshold} \u2013 ${rule.thresholdHigh}"
                    else
                        "$metricName ${op.symbol} ${rule.threshold}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(4.dp))

            // THEN summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "THEN ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(actionLabel, style = MaterialTheme.typography.bodyMedium)
                if (rule.actionType == LinkActionType.NOTIFICATION.key) {
                    val title = rule.actionConfig["title"] ?: ""
                    if (title.isNotBlank()) {
                        Text(
                            " \u2014 \"$title\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S.link.editLink)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S.link.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ─── Editor dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkEditorDialog(
    rule: LinkRule,
    strings: S.Link,
    onDismiss: () -> Unit,
    onSave: (LinkRule) -> Unit,
) {
    var name by remember { mutableStateOf(rule.name) }
    var metricKey by remember { mutableStateOf(rule.metricKey) }
    var operator by remember { mutableStateOf(rule.operator) }
    var threshold by remember { mutableStateOf(rule.threshold) }
    var thresholdHigh by remember { mutableStateOf(rule.thresholdHigh) }
    var actionType by remember { mutableStateOf(rule.actionType) }
    var notifTitle by remember { mutableStateOf(rule.actionConfig["title"] ?: "") }
    var notifBody by remember { mutableStateOf(rule.actionConfig["body"] ?: "") }
    var logText by remember { mutableStateOf(rule.actionConfig["logText"] ?: "") }
    var cooldown by remember { mutableFloatStateOf(rule.cooldownSec.toFloat()) }

    var metricExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }

    val metricGroups = remember { WidgetMetric.grouped() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (rule.name.isBlank() && rule.metricKey.isBlank()) strings.addLink else strings.editLink,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rule name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.ruleName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── IF block ─────────────────────────────────────────────
                Text(
                    strings.ifLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Metric selector
                        ExposedDropdownMenuBox(
                            expanded = metricExpanded,
                            onExpandedChange = { metricExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = WidgetMetric.fromKey(metricKey)?.displayName ?: strings.selectMetric,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.metric) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(metricExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = metricExpanded,
                                onDismissRequest = { metricExpanded = false },
                            ) {
                                metricGroups.forEach { (category, metrics) ->
                                    Text(
                                        category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    )
                                    metrics.forEach { metric ->
                                        DropdownMenuItem(
                                            text = { Text("${metric.displayName} (${metric.unit})") },
                                            onClick = {
                                                metricKey = metric.key
                                                metricExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // Operator selector
                        ExposedDropdownMenuBox(
                            expanded = operatorExpanded,
                            onExpandedChange = { operatorExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = LinkOperator.fromKey(operator).symbol,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.operator) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(operatorExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = operatorExpanded,
                                onDismissRequest = { operatorExpanded = false },
                            ) {
                                LinkOperator.entries.forEach { op ->
                                    DropdownMenuItem(
                                        text = { Text("${op.symbol}  (${op.name.lowercase().replace('_', ' ')})") },
                                        onClick = {
                                            operator = op.key
                                            operatorExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Threshold input
                        OutlinedTextField(
                            value = threshold,
                            onValueChange = { threshold = it },
                            label = {
                                Text(
                                    if (LinkOperator.fromKey(operator).isRange)
                                        strings.thresholdLow
                                    else
                                        strings.threshold,
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Upper bound for range operators (between / outside)
                        if (LinkOperator.fromKey(operator).isRange) {
                            OutlinedTextField(
                                value = thresholdHigh,
                                onValueChange = { thresholdHigh = it },
                                label = { Text(strings.thresholdHigh) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Recommended threshold hint
                        val hint = recommendedThresholds(metricKey)
                        if (hint != null) {
                            Text(
                                "${strings.recommended}: $hint",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── THEN block ───────────────────────────────────────────
                Text(
                    strings.thenLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Action selector
                        ExposedDropdownMenuBox(
                            expanded = actionExpanded,
                            onExpandedChange = { actionExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = LinkActionType.fromKey(actionType).label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.action) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actionExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = actionExpanded,
                                onDismissRequest = { actionExpanded = false },
                            ) {
                                LinkActionType.entries.forEach { act ->
                                    DropdownMenuItem(
                                        text = { Text(act.label) },
                                        onClick = {
                                            actionType = act.key
                                            actionExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Notification-specific config
                        if (actionType == LinkActionType.NOTIFICATION.key) {
                            OutlinedTextField(
                                value = notifTitle,
                                onValueChange = { notifTitle = it },
                                label = { Text(strings.notifTitle) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = notifBody,
                                onValueChange = { notifBody = it },
                                label = { Text(strings.notifBody) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Log entry-specific config
                        if (actionType == LinkActionType.LOG_ENTRY.key) {
                            OutlinedTextField(
                                value = logText,
                                onValueChange = { logText = it },
                                label = { Text(strings.logEntryText) },
                                placeholder = { Text(strings.logEntryPlaceholder) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // Cooldown slider
                SliderWithInput(
                    value = cooldown,
                    onValueChange = { cooldown = it },
                    valueRange = 5f..300f,
                    suffix = "s",
                    label = strings.cooldown,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val config = mutableMapOf<String, String>()
                    if (actionType == LinkActionType.NOTIFICATION.key) {
                        config["title"] = notifTitle.ifBlank { "Link Alert" }
                        config["body"] = notifBody
                    }
                    if (actionType == LinkActionType.LOG_ENTRY.key) {
                        config["logText"] = logText
                    }
                    onSave(
                        rule.copy(
                            name = name,
                            metricKey = metricKey,
                            operator = operator,
                            threshold = threshold,
                            thresholdHigh = thresholdHigh,
                            actionType = actionType,
                            actionConfig = config,
                            cooldownSec = cooldown.toInt(),
                        )
                    )
                },
                enabled = metricKey.isNotBlank(),
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

// ─── Helpers ───────────────────────────────────────────────────────────────

private val displayFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun formatIso(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(displayFmt)
} catch (_: Exception) {
    iso
}
