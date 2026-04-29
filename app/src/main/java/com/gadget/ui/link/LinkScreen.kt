package com.gadget.ui.link

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import com.gadget.services.LinkService
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.SliderWithInput
import com.gadget.widget.WidgetMetric
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val PREFS_NAME = "link_rules"
private const val KEY_RULES_V2 = "rules_v2"
private const val KEY_RULES_V1 = "rules"
private const val KEY_STATS = "link_stats"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen() {
    val context = LocalContext.current
    val strings = S.link
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var rules by remember { mutableStateOf(loadInitialRules(prefs)) }
    var editingRule by remember { mutableStateOf<LinkRuleV2?>(null) }
    var deleteTarget by remember { mutableStateOf<LinkRuleV2?>(null) }
    var serviceRunning by remember { mutableStateOf(LinkService.isRunning) }

    fun persist(updated: List<LinkRuleV2>) {
        rules = updated
        prefs.edit().putString(KEY_RULES_V2, saveRulesV2(updated)).apply()
    }

    ScreenAnnouncement(S.accessibility.linkScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(strings.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

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
                    Text(strings.monitoring, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        OutlinedButton(
            onClick = { editingRule = newBlankRule() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.addLink)
        }

        if (rules.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(strings.noLinksYet, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        rules.forEach { rule ->
            LinkRuleCard(
                rule = rule,
                onToggle = { enabled ->
                    persist(rules.map { if (it.id == rule.id) it.copy(enabled = enabled) else it })
                },
                onEdit = { editingRule = rule },
                onDelete = { deleteTarget = rule },
            )
        }

        if (rules.isNotEmpty()) {
            val statsJson = remember { prefs.getString(KEY_STATS, "") ?: "" }
            val stats = remember(statsJson) { loadLinkStats(statsJson) }
            val hasAnyStats = stats.values.any { it.triggerCount > 0 || it.cooldownBlockCount > 0 }

            if (hasAnyStats) {
                Text(strings.statsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                rules.forEach { rule ->
                    val s = stats[rule.id] ?: return@forEach
                    if (s.triggerCount == 0 && s.cooldownBlockCount == 0) return@forEach
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(rule.name.ifBlank { "Link Rule" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(strings.triggered, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${s.triggerCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(strings.cooldownBlocked, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${s.cooldownBlockCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            if (s.lastTriggeredIso.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("${strings.lastTriggered}: ${formatIso(s.lastTriggeredIso)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { prefs.edit().putString(KEY_STATS, "").apply() },
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

    editingRule?.let { rule ->
        LinkEditorDialog(
            initialRule = rule,
            strings = strings,
            onDismiss = { editingRule = null },
            onSave = { saved ->
                val existing = rules.indexOfFirst { it.id == saved.id }
                val updated = if (existing >= 0) rules.toMutableList().also { it[existing] = saved }
                else rules + saved
                persist(updated)
                editingRule = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(strings.deleteLink) },
            text = { Text(strings.deleteConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    persist(rules.filter { it.id != target.id })
                    deleteTarget = null
                }) { Text(strings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            },
        )
    }
}

private fun loadInitialRules(prefs: android.content.SharedPreferences): List<LinkRuleV2> {
    val v2 = prefs.getString(KEY_RULES_V2, "") ?: ""
    if (v2.isNotBlank()) return loadRulesV2(v2)
    val v1 = prefs.getString(KEY_RULES_V1, "") ?: ""
    val migrated = loadRulesV2(v1)
    if (migrated.isNotEmpty()) {
        prefs.edit().putString(KEY_RULES_V2, saveRulesV2(migrated)).remove(KEY_RULES_V1).apply()
    }
    return migrated
}

private fun newBlankRule(): LinkRuleV2 = LinkRuleV2(
    root = ConditionNode.Group(
        logic = LogicOperator.AND,
        children = listOf(ConditionNode.Leaf(metricKey = "", operator = LinkOperator.GREATER_THAN.key, threshold = "0")),
    ),
    actions = listOf(ActionStep(actionType = LinkActionType.NOTIFICATION.key, actionConfig = mapOf("title" to "Link Alert"))),
)

// ─── Helpers ───────────────────────────────────────────────────────────────

private val displayFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")

internal fun formatIso(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(displayFmt)
} catch (_: Exception) { iso }

// ─── Rule card (summary view) ──────────────────────────────────────────────

@Composable
private fun LinkRuleCard(
    rule: LinkRuleV2,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val condSummary = remember(rule.root) { summarizeCondition(rule.root) }
    val actionSummary = remember(rule.actions) {
        if (rule.actions.isEmpty()) S.link.noActions
        else rule.actions.joinToString(" → ") { LinkActionType.fromKey(it.actionType).label }
    }

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
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${S.link.ifLabel} ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(condSummary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${S.link.thenLabel} ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(actionSummary, style = MaterialTheme.typography.bodyMedium)
            }
            if (rule.triggerDelaySec > 0 || rule.schedule != null) {
                Spacer(Modifier.height(4.dp))
                val parts = buildList {
                    if (rule.triggerDelaySec > 0) add("⏱ +${rule.triggerDelaySec}s")
                    if (rule.schedule != null) add("📅 ${rule.schedule.startTime}–${rule.schedule.endTime}")
                }
                Text(
                    parts.joinToString("   "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

private fun summarizeCondition(node: ConditionNode): String {
    val text = when (node) {
        is ConditionNode.Leaf -> {
            val name = WidgetMetric.fromKey(node.metricKey)?.displayName ?: node.metricKey.ifBlank { "?" }
            val op = LinkOperator.fromKey(node.operator)
            val tail = if (op.isRange) "${node.threshold}–${node.thresholdHigh}" else node.threshold
            val core = "$name ${op.symbol} $tail"
            if (node.sustainSec > 0) "$core (${node.sustainSec}s)" else core
        }
        is ConditionNode.Group -> {
            if (node.children.isEmpty()) S.link.ruleSummaryEmpty
            else node.children.joinToString(" ${node.logic.name} ") { summarizeCondition(it) }
                .let { if (node.children.size > 1) "($it)" else it }
        }
    }
    return if (node.negate) "${S.link.notLabel} $text" else text
}

// ─── Editor dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkEditorDialog(
    initialRule: LinkRuleV2,
    strings: S.Link,
    onDismiss: () -> Unit,
    onSave: (LinkRuleV2) -> Unit,
) {
    var name by remember { mutableStateOf(initialRule.name) }
    var root by remember { mutableStateOf(initialRule.root) }
    var actions by remember { mutableStateOf(initialRule.actions) }
    var cooldown by remember { mutableFloatStateOf(initialRule.cooldownSec.toFloat()) }
    var triggerDelay by remember { mutableFloatStateOf(initialRule.triggerDelaySec.toFloat()) }
    var cancelIfFalse by remember { mutableStateOf(initialRule.cancelDelayIfFalse) }
    var schedule by remember { mutableStateOf(initialRule.schedule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialRule.name.isBlank() && root.allLeaves().all { it.metricKey.isBlank() })
                    strings.addLink else strings.editLink,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.ruleName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    strings.ifLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                ConditionNodeEditor(
                    node = root,
                    depth = 0,
                    isRoot = true,
                    onChange = { root = it },
                    onDelete = { /* root cannot be deleted */ },
                )

                Text(
                    strings.thenLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                ActionChainEditor(
                    actions = actions,
                    onChange = { actions = it },
                )

                Text(
                    strings.whenLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                ScheduleEditor(schedule = schedule, onChange = { schedule = it })

                // Trigger delay + cooldown
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.triggerDelay, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(strings.triggerDelayDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SliderWithInput(
                            value = triggerDelay,
                            onValueChange = { triggerDelay = it },
                            valueRange = 0f..600f,
                            suffix = "s",
                            label = "${triggerDelay.toInt()} ${strings.seconds}",
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = cancelIfFalse, onCheckedChange = { cancelIfFalse = it })
                            Text(strings.cancelIfFalse, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(strings.cooldown, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        SliderWithInput(
                            value = cooldown,
                            onValueChange = { cooldown = it },
                            valueRange = 5f..300f,
                            suffix = "s",
                            label = "${cooldown.toInt()} ${strings.seconds}",
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initialRule.copy(
                            name = name,
                            root = root,
                            actions = actions,
                            cooldownSec = cooldown.toInt(),
                            triggerDelaySec = triggerDelay.toInt(),
                            cancelDelayIfFalse = cancelIfFalse,
                            schedule = schedule,
                        )
                    )
                },
                enabled = root.allLeaves().any { it.metricKey.isNotBlank() } && actions.isNotEmpty(),
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

// ─── Recursive condition tree editor ───────────────────────────────────────

private const val MAX_TREE_DEPTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionNodeEditor(
    node: ConditionNode,
    depth: Int,
    isRoot: Boolean,
    onChange: (ConditionNode) -> Unit,
    onDelete: () -> Unit,
) {
    when (node) {
        is ConditionNode.Leaf -> LeafEditor(node, isRoot, onChange, onDelete)
        is ConditionNode.Group -> GroupEditor(node, depth, isRoot, onChange, onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupEditor(
    group: ConditionNode.Group,
    depth: Int,
    isRoot: Boolean,
    onChange: (ConditionNode) -> Unit,
    onDelete: () -> Unit,
) {
    val strings = S.link
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header: AND/OR toggle + NOT toggle + delete (if not root)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = group.logic == LogicOperator.AND,
                    onClick = { onChange(group.copy(logic = LogicOperator.AND)) },
                    label = { Text(strings.andLogic) },
                )
                Spacer(Modifier.width(4.dp))
                FilterChip(
                    selected = group.logic == LogicOperator.OR,
                    onClick = { onChange(group.copy(logic = LogicOperator.OR)) },
                    label = { Text(strings.orLogic) },
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = group.negate,
                    onClick = { onChange(group.copy(negate = !group.negate)) },
                    label = { Text(strings.notLabel) },
                )
                Spacer(Modifier.weight(1f))
                if (!isRoot) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Close, contentDescription = strings.removeCondition, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Children
            group.children.forEachIndexed { i, child ->
                ConditionNodeEditor(
                    node = child,
                    depth = depth + 1,
                    isRoot = false,
                    onChange = { updated ->
                        val newChildren = group.children.toMutableList().also { it[i] = updated }
                        onChange(group.copy(children = newChildren))
                    },
                    onDelete = {
                        val newChildren = group.children.toMutableList().also { it.removeAt(i) }
                        onChange(group.copy(children = newChildren))
                    },
                )
            }

            // Add buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    onChange(group.copy(children = group.children + blankLeaf()))
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.addCondition)
                }
                if (depth + 1 < MAX_TREE_DEPTH) {
                    TextButton(onClick = {
                        onChange(group.copy(children = group.children + ConditionNode.Group(children = listOf(blankLeaf()))))
                    }) {
                        Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.addSubgroup)
                    }
                }
            }
        }
    }
}

private fun blankLeaf() = ConditionNode.Leaf(
    metricKey = "",
    operator = LinkOperator.GREATER_THAN.key,
    threshold = "0",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeafEditor(
    leaf: ConditionNode.Leaf,
    isRoot: Boolean,
    onChange: (ConditionNode) -> Unit,
    onDelete: () -> Unit,
) {
    val strings = S.link
    var metricExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }
    var allowedExpanded by remember { mutableStateOf(false) }

    val metric = WidgetMetric.fromKey(leaf.metricKey)
    val metadata = MetricMetadataRegistry.get(leaf.metricKey)
    val isCategorical = metadata?.isCategorical == true
    val metricGroups = remember { WidgetMetric.grouped() }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = leaf.negate,
                    onClick = { onChange(leaf.copy(negate = !leaf.negate)) },
                    label = { Text(strings.notLabel) },
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    // Wrap leaf in a single-child AND group
                    onChange(ConditionNode.Group(logic = LogicOperator.AND, children = listOf(leaf)))
                }) {
                    Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.convertToGroup, style = MaterialTheme.typography.labelMedium)
                }
                if (!isRoot) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Close, contentDescription = strings.removeCondition, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Metric picker
            ExposedDropdownMenuBox(
                expanded = metricExpanded,
                onExpandedChange = { metricExpanded = it },
            ) {
                OutlinedTextField(
                    value = metric?.displayName ?: strings.selectMetric,
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
                        metrics.forEach { m ->
                            DropdownMenuItem(
                                text = { Text("${m.displayName}${if (m.unit.isNotBlank()) " (${m.unit})" else ""}") },
                                onClick = {
                                    val newMeta = MetricMetadataRegistry.get(m.key)
                                    onChange(
                                        leaf.copy(
                                            metricKey = m.key,
                                            threshold = newMeta?.let {
                                                if (it.isInteger) it.defaultThreshold.toLong().toString()
                                                else it.defaultThreshold.toString()
                                            } ?: leaf.threshold,
                                        )
                                    )
                                    metricExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Operator picker
            val applicableOps = if (isCategorical) {
                listOf(LinkOperator.EQUAL, LinkOperator.NOT_EQUAL)
            } else LinkOperator.entries
            ExposedDropdownMenuBox(
                expanded = operatorExpanded,
                onExpandedChange = { operatorExpanded = it },
            ) {
                OutlinedTextField(
                    value = LinkOperator.fromKey(leaf.operator).symbol,
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
                    applicableOps.forEach { op ->
                        DropdownMenuItem(
                            text = { Text("${op.symbol}  (${op.name.lowercase().replace('_', ' ')})") },
                            onClick = {
                                onChange(leaf.copy(operator = op.key))
                                operatorExpanded = false
                            },
                        )
                    }
                }
            }

            // Threshold input — categorical (dropdown of allowedValues, or free text) vs numeric
            if (isCategorical && metadata?.allowedValues?.isNotEmpty() == true) {
                ExposedDropdownMenuBox(
                    expanded = allowedExpanded,
                    onExpandedChange = { allowedExpanded = it },
                ) {
                    OutlinedTextField(
                        value = leaf.threshold,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.threshold) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(allowedExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = allowedExpanded,
                        onDismissRequest = { allowedExpanded = false },
                    ) {
                        metadata.allowedValues.forEach { v ->
                            DropdownMenuItem(
                                text = { Text(v) },
                                onClick = { onChange(leaf.copy(threshold = v)); allowedExpanded = false },
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = leaf.threshold,
                    onValueChange = { onChange(leaf.copy(threshold = it)) },
                    label = {
                        Text(
                            if (LinkOperator.fromKey(leaf.operator).isRange) strings.thresholdLow
                            else strings.threshold,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = if (isCategorical) KeyboardOptions.Default
                    else KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (LinkOperator.fromKey(leaf.operator).isRange) {
                    OutlinedTextField(
                        value = leaf.thresholdHigh,
                        onValueChange = { onChange(leaf.copy(thresholdHigh = it)) },
                        label = { Text(strings.thresholdHigh) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Preset chips
            if (metadata != null && metadata.presets.isNotEmpty()) {
                Text(strings.applyPreset, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    metadata.presets.forEach { p ->
                        AssistChip(
                            onClick = {
                                onChange(
                                    leaf.copy(
                                        operator = p.operator.key,
                                        threshold = formatPresetValue(p.low, metadata),
                                        thresholdHigh = if (p.operator.isRange) formatPresetValue(p.high, metadata) else "",
                                    )
                                )
                            },
                            label = { Text(p.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }

            // Recommended hint
            val hint = metadata?.hintString() ?: recommendedThresholds(leaf.metricKey)
            if (hint != null) {
                Text(
                    "${strings.recommended}: $hint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Sustain
            Text(strings.sustainFor, style = MaterialTheme.typography.labelSmall)
            SliderWithInput(
                value = leaf.sustainSec.toFloat(),
                onValueChange = { onChange(leaf.copy(sustainSec = it.toInt())) },
                valueRange = 0f..600f,
                suffix = "s",
                label = "${leaf.sustainSec} ${strings.seconds}",
            )
        }
    }
}

private fun formatPresetValue(v: Double, meta: MetricMetadata): String =
    if (meta.isInteger || v == v.toLong().toDouble()) v.toLong().toString()
    else v.toString()

// ─── Action chain editor ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChainEditor(
    actions: List<ActionStep>,
    onChange: (List<ActionStep>) -> Unit,
) {
    val strings = S.link
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEachIndexed { i, step ->
            ActionStepCard(
                index = i,
                step = step,
                onChange = { updated ->
                    onChange(actions.toMutableList().also { it[i] = updated })
                },
                onDelete = {
                    onChange(actions.toMutableList().also { it.removeAt(i) })
                },
                onMoveUp = if (i > 0) {{
                    val list = actions.toMutableList()
                    list[i] = actions[i - 1]; list[i - 1] = step
                    onChange(list)
                }} else null,
                onMoveDown = if (i < actions.size - 1) {{
                    val list = actions.toMutableList()
                    list[i] = actions[i + 1]; list[i + 1] = step
                    onChange(list)
                }} else null,
            )
        }
        OutlinedButton(
            onClick = {
                onChange(actions + ActionStep(actionType = LinkActionType.NOTIFICATION.key))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.addAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionStepCard(
    index: Int,
    step: ActionStep,
    onChange: (ActionStep) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val strings = S.link
    var actionExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "#${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.weight(1f))
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Close, contentDescription = strings.removeAction, modifier = Modifier.size(20.dp))
                }
            }

            ExposedDropdownMenuBox(
                expanded = actionExpanded,
                onExpandedChange = { actionExpanded = it },
            ) {
                OutlinedTextField(
                    value = LinkActionType.fromKey(step.actionType).label,
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
                    LinkActionType.entries.forEach { a ->
                        DropdownMenuItem(
                            text = { Text(a.label) },
                            onClick = { onChange(step.copy(actionType = a.key)); actionExpanded = false },
                        )
                    }
                }
            }

            // Per-action config
            when (LinkActionType.fromKey(step.actionType)) {
                LinkActionType.NOTIFICATION -> {
                    OutlinedTextField(
                        value = step.actionConfig["title"] ?: "",
                        onValueChange = { onChange(step.copy(actionConfig = step.actionConfig + ("title" to it))) },
                        label = { Text(strings.notifTitle) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = step.actionConfig["body"] ?: "",
                        onValueChange = { onChange(step.copy(actionConfig = step.actionConfig + ("body" to it))) },
                        label = { Text(strings.notifBody) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                LinkActionType.LOG_ENTRY -> {
                    OutlinedTextField(
                        value = step.actionConfig["logText"] ?: "",
                        onValueChange = { onChange(step.copy(actionConfig = step.actionConfig + ("logText" to it))) },
                        label = { Text(strings.logEntryText) },
                        placeholder = { Text(strings.logEntryPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> { /* no config */ }
            }

            // Per-step delay (in seconds for the UI; stored as ms)
            Text(strings.actionDelay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SliderWithInput(
                value = (step.delayMs / 1000f).coerceAtMost(60f),
                onValueChange = { onChange(step.copy(delayMs = (it * 1000f).toLong())) },
                valueRange = 0f..60f,
                suffix = "s",
                label = "${(step.delayMs / 1000)} ${strings.seconds}",
            )
        }
    }
}

// ─── Schedule editor ───────────────────────────────────────────────────────

@Composable
private fun ScheduleEditor(
    schedule: TimeSchedule?,
    onChange: (TimeSchedule?) -> Unit,
) {
    val strings = S.link
    val active = schedule != null

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.alwaysActive, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = !active,
                    onCheckedChange = { alwaysActive ->
                        onChange(if (alwaysActive) null else TimeSchedule())
                    },
                )
            }
            if (schedule != null) {
                // Days of week chips (Sun=1 .. Sat=7)
                val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { i, label ->
                        val day = i + 1
                        FilterChip(
                            selected = day in schedule.daysOfWeek,
                            onClick = {
                                val newDays = schedule.daysOfWeek.toMutableSet()
                                if (day in newDays) newDays.remove(day) else newDays.add(day)
                                onChange(schedule.copy(daysOfWeek = newDays))
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = schedule.startTime,
                        onValueChange = { onChange(schedule.copy(startTime = it)) },
                        label = { Text(strings.startTime) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = schedule.endTime,
                        onValueChange = { onChange(schedule.copy(endTime = it)) },
                        label = { Text(strings.endTime) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
