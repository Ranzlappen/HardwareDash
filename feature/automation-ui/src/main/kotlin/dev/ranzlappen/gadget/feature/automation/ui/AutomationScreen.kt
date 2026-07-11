package dev.ranzlappen.gadget.feature.automation.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.automation.RuleFireOutcome
import dev.ranzlappen.gadget.core.automation.RuleFireRecord
import dev.ranzlappen.gadget.core.automation.model.ComparisonOp
import dev.ranzlappen.gadget.core.automation.model.Edge
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.RuleAction
import dev.ranzlappen.gadget.core.automation.model.RuleTemplate
import dev.ranzlappen.gadget.core.automation.model.Trigger
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetDialog
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetFab
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.module.CapabilityAction
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.OsNote
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.feature.automation.ui.editor.RuleEditorSheet
import java.util.UUID

/**
 * Hilt entry point for the Automation screen: collects the rule list +
 * exact-alarm state, resolves the two enumeration seams into plain lists,
 * and handles the Android edges (snackbar events, ON_RESUME permission
 * refresh, the `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` deep link) — keeping
 * [AutomationScreenContent] (and its previews/tests) Hilt-free, per the
 * module blueprint (torch/sensors reference).
 */
@Composable
fun AutomationScreen(
    modifier: Modifier = Modifier,
    viewModel: AutomationViewModel = hiltViewModel(),
) {
    // stateIn-backed flows -> collectAsState (lifecycle-runtime-compose
    // isn't in the feature plugin's default set; see the CLAUDE.md pitfall).
    val rules by viewModel.rules.collectAsState()
    val exactAlarmAllowed by viewModel.exactAlarmAllowed.collectAsState()
    val fireHistory by viewModel.fireHistoryRecords.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is AutomationUiEvent.RanNow ->
                    if (event.dispatched > 0) {
                        context.getString(
                            R.string.automation_run_now_result,
                            event.ruleName,
                            event.dispatched,
                        )
                    } else {
                        context.getString(R.string.automation_run_now_nothing, event.ruleName)
                    }
                is AutomationUiEvent.TemplateAdded ->
                    context.getString(R.string.automation_template_added, event.templateName)
                is AutomationUiEvent.TestFired ->
                    if (event.wouldDispatch > 0) {
                        context.getString(R.string.automation_test_fire_result, event.wouldDispatch)
                    } else {
                        context.getString(R.string.automation_test_fire_nothing)
                    }
                is AutomationUiEvent.Exported ->
                    if (event.count > 0) {
                        context.getString(R.string.automation_export_result, event.count)
                    } else {
                        context.getString(R.string.automation_export_empty)
                    }
                is AutomationUiEvent.Imported ->
                    if (event.imported != null) {
                        context.getString(R.string.automation_import_result, event.imported)
                    } else {
                        context.getString(R.string.automation_import_failed, event.reason.orEmpty())
                    }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // Exact-alarm grant state is live: refreshed on ON_RESUME so the badge
    // clears after the settings round-trip (the ModulePermissionsSection
    // mechanism).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshExactAlarmStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onRequestExactAlarm: () -> Unit = onRequestExactAlarm@{
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@onRequestExactAlarm
        // Some OEM builds ship without a handler for this settings action —
        // swallow rather than crash; the badge simply stays visible.
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        }
    }

    AutomationScreenContent(
        rules = rules,
        signals = viewModel.signals,
        actionChoices = viewModel.actionChoices,
        exactAlarmAllowed = exactAlarmAllowed,
        templates = viewModel.templates,
        fireHistory = fireHistory,
        moduleInfo = automationModuleInfo(exactAlarmAllowed, onRequestExactAlarm),
        onSave = viewModel::saveRule,
        onDelete = viewModel::deleteRule,
        onSetEnabled = viewModel::setEnabled,
        onRunNow = viewModel::runNow,
        onTestFire = viewModel::testFire,
        onApplyTemplate = viewModel::applyTemplate,
        onExport = viewModel::exportRules,
        onImport = viewModel::importRules,
        onClearHistory = viewModel::clearHistory,
        onRequestExactAlarm = onRequestExactAlarm,
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    )
}

/**
 * The standard module-info block: no runtime permissions (alarm scheduling
 * uses a special permission surfaced as a capability row instead), the OS
 * notes from the design doc's exact-alarm degradation contract, and one
 * live tri-state capability row for `canScheduleExactAlarms()`.
 */
@Composable
private fun automationModuleInfo(
    exactAlarmAllowed: Boolean,
    onRequestExactAlarm: () -> Unit,
): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(
        minSdk = 29,
        notes = listOf(
            OsNote(sinceSdk = 31, text = stringResource(R.string.automation_os_note_31)),
            OsNote(sinceSdk = 34, text = stringResource(R.string.automation_os_note_34)),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.automation_capability_exact),
            detail = stringResource(R.string.automation_capability_exact_detail),
            status = {
                if (exactAlarmAllowed) {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Success,
                        message = stringResource(R.string.automation_capability_exact_ok),
                    )
                } else {
                    CapabilityStatus(
                        kind = GadgetStatusKind.Warning,
                        message = stringResource(R.string.automation_capability_exact_missing),
                        action = CapabilityAction.Custom(
                            label = stringResource(R.string.automation_capability_exact_action),
                            onClick = onRequestExactAlarm,
                        ),
                    )
                }
            },
        ),
    ),
)

/**
 * Stateless Automation screen content: the rules list (one [RuleCard] per
 * rule, [GadgetEmptyState] when none), a "New rule" [GadgetFab], and the
 * [RuleEditorSheet] / delete-confirm dialog driven by local transient
 * state. All persistence flows out through the hoisted callbacks.
 */
@Composable
internal fun AutomationScreenContent(
    rules: List<Rule>,
    signals: List<MetricDescriptor>,
    actionChoices: List<ActionChoice>,
    exactAlarmAllowed: Boolean,
    moduleInfo: ModuleInfo?,
    onSave: (Rule) -> Unit,
    onDelete: (String) -> Unit,
    onSetEnabled: (String, Boolean) -> Unit,
    onRunNow: (Rule) -> Unit,
    onRequestExactAlarm: () -> Unit,
    modifier: Modifier = Modifier,
    templates: List<RuleTemplate> = emptyList(),
    fireHistory: List<RuleFireRecord> = emptyList(),
    onTestFire: (Rule) -> Unit = {},
    onApplyTemplate: (RuleTemplate) -> Unit = {},
    onExport: () -> Unit = {},
    onImport: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
) {
    val spacing = LocalGadgetTheme.current.spacing
    val signalsByKey = remember(signals) { signals.associateBy { it.metricKey } }
    var editorRule by remember { mutableStateOf<Rule?>(null) }
    var pendingDelete by remember { mutableStateOf<Rule?>(null) }
    var showTemplates by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        ModuleScreenScaffold(
            title = stringResource(R.string.automation_title),
            moduleInfo = moduleInfo,
            functional = {
                AutomationToolbar(
                    onTemplates = { showTemplates = true },
                    onExport = onExport,
                    onImport = { showImport = true },
                    onHistory = { showHistory = true },
                )
                if (rules.isEmpty()) {
                    GadgetEmptyState(
                        title = stringResource(R.string.automation_empty_title),
                        subtitle = stringResource(R.string.automation_empty_subtitle),
                        icon = Icons.Outlined.Bolt,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    rules.forEach { rule ->
                        RuleCard(
                            rule = rule,
                            signalsByKey = signalsByKey,
                            actionChoices = actionChoices,
                            onClick = { editorRule = rule },
                            onSetEnabled = { enabled -> onSetEnabled(rule.id, enabled) },
                            onRunNow = { onRunNow(rule) },
                            onTestFire = { onTestFire(rule) },
                            onDeleteRequest = { pendingDelete = rule },
                        )
                    }
                }
                // Keep the last card's controls clear of the overlaid FAB.
                Spacer(modifier = Modifier.height(FabClearance))
            },
        )

        GadgetFab(
            onClick = { editorRule = newRule() },
            icon = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.automation_new_rule),
            text = stringResource(R.string.automation_new_rule),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(spacing.large),
        )

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            snackbarHost()
        }
    }

    editorRule?.let { editing ->
        RuleEditorSheet(
            initial = editing,
            isNew = rules.none { it.id == editing.id },
            signals = signals,
            actionChoices = actionChoices,
            exactAlarmAllowed = exactAlarmAllowed,
            onRequestExactAlarm = onRequestExactAlarm,
            onDismiss = { editorRule = null },
            onSave = { saved ->
                onSave(saved)
                editorRule = null
            },
        )
    }

    pendingDelete?.let { doomed ->
        GadgetDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.automation_delete_confirm_title),
            text = stringResource(R.string.automation_delete_confirm_text, doomed.name),
            confirmButton = {
                GadgetPrimaryButton(
                    onClick = {
                        onDelete(doomed.id)
                        pendingDelete = null
                    },
                    text = stringResource(R.string.automation_delete_confirm_yes),
                )
            },
            dismissButton = {
                GadgetTertiaryButton(
                    onClick = { pendingDelete = null },
                    text = stringResource(R.string.automation_cancel),
                )
            },
        )
    }

    if (showTemplates) {
        TemplatesSheet(
            templates = templates,
            onPick = { template ->
                onApplyTemplate(template)
                showTemplates = false
            },
            onDismiss = { showTemplates = false },
        )
    }

    if (showImport) {
        ImportSheet(
            onImport = { json ->
                onImport(json)
                showImport = false
            },
            onDismiss = { showImport = false },
        )
    }

    if (showHistory) {
        HistorySheet(
            records = fireHistory,
            onClear = onClearHistory,
            onDismiss = { showHistory = false },
        )
    }
}

/** The Templates / Export / Import / History action row above the rule list. */
@Composable
private fun AutomationToolbar(
    onTemplates: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        GadgetTertiaryButton(
            onClick = onTemplates,
            text = stringResource(R.string.automation_templates),
            leadingIcon = Icons.Outlined.Dashboard,
        )
        GadgetTertiaryButton(
            onClick = onHistory,
            text = stringResource(R.string.automation_history),
            leadingIcon = Icons.Outlined.History,
        )
        GadgetIconButton(
            onClick = onExport,
            icon = Icons.Outlined.Upload,
            contentDescription = stringResource(R.string.automation_export),
        )
        GadgetIconButton(
            onClick = onImport,
            icon = Icons.Outlined.Download,
            contentDescription = stringResource(R.string.automation_import),
        )
    }
}

/** Bottom sheet listing the built-in [RuleTemplate]s to drop in. */
@Composable
private fun TemplatesSheet(
    templates: List<RuleTemplate>,
    onPick: (RuleTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.automation_templates_title),
    ) {
        templates.forEach { template ->
            DashCard(
                modifier = Modifier.fillMaxWidth(),
                title = template.name,
                icon = Icons.Outlined.Dashboard,
                onClick = { onPick(template) },
            ) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(spacing.small))
        }
    }
}

/** Paste-a-JSON-document import bottom sheet. */
@Composable
private fun ImportSheet(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.automation_import_title),
    ) {
        GadgetTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.automation_import),
            placeholder = stringResource(R.string.automation_import_hint),
            singleLine = false,
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        GadgetPrimaryButton(
            onClick = { onImport(text) },
            text = stringResource(R.string.automation_import_confirm),
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Bottom sheet showing the rule firing-history audit trail. */
@Composable
private fun HistorySheet(
    records: List<RuleFireRecord>,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.automation_history_title),
    ) {
        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.automation_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            records.forEach { record -> HistoryRow(record) }
            Spacer(modifier = Modifier.height(spacing.small))
            GadgetTertiaryButton(
                onClick = onClear,
                text = stringResource(R.string.automation_history_clear),
                leadingIcon = Icons.Outlined.Delete,
            )
        }
    }
}

@Composable
private fun HistoryRow(record: RuleFireRecord, modifier: Modifier = Modifier) {
    val outcomeLabel = when (record.outcome) {
        RuleFireOutcome.Fired -> stringResource(R.string.automation_history_outcome_fired)
        RuleFireOutcome.Skipped -> stringResource(R.string.automation_history_outcome_skipped)
        RuleFireOutcome.Throttled -> stringResource(R.string.automation_history_outcome_throttled)
    }
    val prefix = if (record.dryRun) {
        stringResource(R.string.automation_history_dry_run) + " · "
    } else {
        ""
    }
    val subtitle = prefix + outcomeLabel + " · " +
        stringResource(R.string.automation_history_dispatched, record.dispatched)
    CompactCard(
        modifier = modifier.fillMaxWidth(),
        title = record.ruleName,
        subtitle = subtitle,
    )
}

/** A fresh draft for the "New rule" FAB; Manual is the no-param default. */
private fun newRule(): Rule = Rule(
    id = UUID.randomUUID().toString(),
    name = "",
    trigger = Trigger.Manual,
)

/**
 * One rule in the list: name + trigger/actions summaries, an enable
 * switch, and run-now / delete affordances. Tapping the card opens the
 * editor.
 */
@Composable
private fun RuleCard(
    rule: Rule,
    signalsByKey: Map<String, MetricDescriptor>,
    actionChoices: List<ActionChoice>,
    onClick: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onTestFire: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = rule.name,
        icon = rule.trigger.icon(),
        onClick = onClick,
    ) {
        Column {
            Text(
                text = triggerSummary(rule.trigger, signalsByKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = actionsSummary(rule, actionChoices),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val toggleDescription = stringResource(R.string.automation_rule_enabled_toggle)
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onSetEnabled,
                    modifier = Modifier.semantics { contentDescription = toggleDescription },
                )
                Spacer(modifier = Modifier.weight(1f))
                GadgetIconButton(
                    onClick = onTestFire,
                    icon = Icons.Outlined.Science,
                    contentDescription = stringResource(R.string.automation_test_fire),
                )
                GadgetIconButton(
                    onClick = onRunNow,
                    icon = Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.automation_run_now),
                )
                GadgetIconButton(
                    onClick = onDeleteRequest,
                    icon = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.automation_delete_rule),
                )
            }
        }
    }
}

/** Per-trigger-kind leading glyph for the rule card header. */
private fun Trigger.icon(): ImageVector = when (this) {
    is Trigger.MetricThreshold -> Icons.Outlined.Sensors
    is Trigger.Schedule -> Icons.Outlined.Schedule
    is Trigger.SystemEvent -> Icons.Outlined.Power
    is Trigger.Manual -> Icons.Outlined.TouchApp
}

/**
 * Fixed clearance so the scrolled column's last row isn't hidden under the
 * overlaid 56 dp FAB (+ its padding) — a layout constant, not a theme
 * spacing step, hence the sanctioned per-file dp literal.
 */
private val FabClearance = 88.dp

// ─── Previews ───────────────────────────────────────────────────────────

private val previewSignals = listOf(
    MetricDescriptor(metricKey = "proximity", displayName = "Proximity", unit = "cm", max = 10f),
    MetricDescriptor(metricKey = "light", displayName = "Ambient light", unit = "lx", max = 40_000f),
)

private val previewChoices = listOf(
    ActionChoice(featureId = "torch", action = ModuleAction(key = "torch_off", label = "Off")),
    ActionChoice(featureId = "torch", action = ModuleAction(key = "torch_on", label = "On")),
)

private val previewRules = listOf(
    Rule(
        id = "1",
        name = "Pocket torch guard",
        trigger = Trigger.MetricThreshold(
            metricKey = "proximity",
            op = ComparisonOp.Lt,
            value = 5f,
            edge = Edge.Rising,
            clearValue = 8f,
        ),
        actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_off")),
    ),
    Rule(
        id = "2",
        name = "Morning lamp",
        trigger = Trigger.Schedule(timeOfDayMinutes = 540, exact = true),
        actions = listOf(RuleAction(featureId = "torch", actionKey = "torch_on")),
        enabled = false,
    ),
    Rule(id = "3", name = "My scene", trigger = Trigger.Manual),
)

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun AutomationScreenPreview() = GadgetThemedPreview {
    AutomationScreenContent(
        rules = previewRules,
        signals = previewSignals,
        actionChoices = previewChoices,
        exactAlarmAllowed = false,
        moduleInfo = null,
        onSave = {},
        onDelete = {},
        onSetEnabled = { _, _ -> },
        onRunNow = {},
        onRequestExactAlarm = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun AutomationScreenEmptyPreview() = GadgetThemedPreview {
    AutomationScreenContent(
        rules = emptyList(),
        signals = previewSignals,
        actionChoices = previewChoices,
        exactAlarmAllowed = true,
        moduleInfo = null,
        onSave = {},
        onDelete = {},
        onSetEnabled = { _, _ -> },
        onRunNow = {},
        onRequestExactAlarm = {},
    )
}
