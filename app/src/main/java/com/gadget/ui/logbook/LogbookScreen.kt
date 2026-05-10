package com.gadget.ui.logbook

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import com.gadget.widget.WidgetMetric
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// ═══════════════════════════════════════════════════════════════════════════════
//  Main entry point
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(vm: LogbookViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val store      by vm.store.collectAsState()
    val activeTab  by vm.activeTab.collectAsState()
    val viewMode   by vm.viewMode.collectAsState()

    // Log tab state
    val entries         by vm.filteredEntries.collectAsState()
    val entryTypeFilter by vm.entryTypeFilter.collectAsState()
    val entrySearch     by vm.entrySearch.collectAsState()
    val entryDateFilter by vm.entryDateFilter.collectAsState()
    val entrySortField  by vm.entrySortField.collectAsState()
    val entrySortDir    by vm.entrySortDir.collectAsState()

    // Processes tab state
    val processes      by vm.filteredProcesses.collectAsState()
    val procTypeFilter by vm.procTypeFilter.collectAsState()
    val procSearch     by vm.procSearch.collectAsState()
    val procDateFilter by vm.procDateFilter.collectAsState()
    val procSortField  by vm.procSortField.collectAsState()
    val procSortDir    by vm.procSortDir.collectAsState()
    val overdueCount   by vm.overdueCount.collectAsState()

    // ── Bottom-sheet state (used in 4D) ─────────────────────────────
    var sheetType by remember { mutableStateOf<SheetType?>(null) }
    var sheetTargetId by remember { mutableStateOf("") }
    var sheetCpIdx by remember { mutableIntStateOf(-1) }
    var sheetTab by remember { mutableStateOf(ActiveTab.LOG) }

    // Capture composable string accessors for use inside non-composable callbacks
    val logbookStrings = S.logbook

    // ── Import / Export launchers ────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = vm.buildExportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            Toast.makeText(context, logbookStrings.exportedSuccessfully, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, logbookStrings.exportFailed + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
            val (newE, newP) = vm.importJson(json)
            Toast.makeText(context, logbookStrings.imported(newE, newP), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, logbookStrings.importFailed + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    ScreenAnnouncement(S.accessibility.logbookScreen)

    // ── Main layout ─────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        // Header
        LogbookHeader(
            entryCount = store.entries.size,
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onExport = {
                val fileName = "ticked-export-${LocalDate.now()}.json"
                exportLauncher.launch(fileName)
            },
        )

        Spacer(Modifier.height(8.dp))

        // Tab row
        LogbookTabRow(
            activeTab = activeTab,
            entryCount = store.entries.size,
            processCount = store.processes.size,
            overdueCount = overdueCount,
            onTabSelected = { vm.setActiveTab(it) },
        )

        Spacer(Modifier.height(12.dp))

        // Tab content
        when (activeTab) {
            ActiveTab.LOG -> LogTabContent(
                vm = vm,
                entries = entries,
                store = store,
                viewMode = viewMode,
                typeFilter = entryTypeFilter,
                search = entrySearch,
                dateFilter = entryDateFilter,
                sortField = entrySortField,
                sortDir = entrySortDir,
                onOpenSheet = { type, id, tab ->
                    sheetType = type
                    sheetTargetId = id
                    sheetTab = tab
                },
            )

            ActiveTab.PROCESSES -> ProcessesTabContent(
                vm = vm,
                processes = processes,
                store = store,
                typeFilter = procTypeFilter,
                search = procSearch,
                dateFilter = procDateFilter,
                sortField = procSortField,
                sortDir = procSortDir,
                onOpenSheet = { type, id, tab, cpIdx ->
                    sheetType = type
                    sheetTargetId = id
                    sheetTab = tab
                    sheetCpIdx = cpIdx
                },
            )
        }
    }

    // ── Bottom sheets (added in 4D) ─────────────────────────────────
    LogbookBottomSheets(
        sheetType = sheetType,
        targetId = sheetTargetId,
        cpIdx = sheetCpIdx,
        tab = sheetTab,
        vm = vm,
        store = store,
        onDismiss = { sheetType = null },
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Sheet type enum (used across phases)
// ═══════════════════════════════════════════════════════════════════════════════

enum class SheetType {
    CHECKPOINT_DETAIL,
    TEXT_EDITOR,
    TIME_EDITOR,
    COLOR_PICKER_BG,
    COLOR_PICKER_BORDER,
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Header
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LogbookHeader(
    entryCount: Int,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Logbook",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (entryCount > 0) {
            Spacer(Modifier.width(8.dp))
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(entryCount.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onImport) {
            Icon(Icons.Filled.FileUpload, "Import", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onExport) {
            Icon(Icons.Filled.FileDownload, "Export", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Tab row
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LogbookTabRow(
    activeTab: ActiveTab,
    entryCount: Int,
    processCount: Int,
    overdueCount: Int,
    onTabSelected: (ActiveTab) -> Unit,
) {
    TabRow(
        selectedTabIndex = if (activeTab == ActiveTab.LOG) 0 else 1,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                val idx = if (activeTab == ActiveTab.LOG) 0 else 1
                val currentTabWidth by animateDpAsState(
                    targetValue = tabPositions[idx].width,
                    label = "tabWidth",
                )
                val indicatorOffset by animateDpAsState(
                    targetValue = tabPositions[idx].left,
                    label = "tabOffset",
                )
                TabRowDefaults.SecondaryIndicator(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.BottomStart)
                        .offset(x = indicatorOffset)
                        .width(currentTabWidth),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        divider = {},
        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        // Log tab
        Tab(
            selected = activeTab == ActiveTab.LOG,
            onClick = { onTabSelected(ActiveTab.LOG) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(S.logbook.logTab)
                    if (entryCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Badge(
                            containerColor = if (activeTab == ActiveTab.LOG)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeTab == ActiveTab.LOG)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(entryCount.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Processes tab
        Tab(
            selected = activeTab == ActiveTab.PROCESSES,
            onClick = { onTabSelected(ActiveTab.PROCESSES) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(S.logbook.processesTab)
                    if (processCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Badge(
                            containerColor = if (activeTab == ActiveTab.PROCESSES)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (activeTab == ActiveTab.PROCESSES)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ) {
                            Text(processCount.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (overdueCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                            Text(overdueCount.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            selectedContentColor = MaterialTheme.colorScheme.primary,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Log tab content
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LogTabContent(
    vm: LogbookViewModel,
    entries: List<LogbookEntry>,
    store: LogbookStore,
    viewMode: ViewMode,
    typeFilter: EntryTypeFilter,
    search: String,
    dateFilter: String,
    sortField: SortField,
    sortDir: SortDirection,
    onOpenSheet: (SheetType, String, ActiveTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Input card (added in 4B)
        LogInputCard(vm = vm)

        Spacer(Modifier.height(8.dp))

        // Filter + sort toolbar (added in 4B)
        LogToolbar(
            vm = vm,
            viewMode = viewMode,
            typeFilter = typeFilter,
            search = search,
            dateFilter = dateFilter,
            sortField = sortField,
            sortDir = sortDir,
            totalCount = store.entries.size,
            filteredCount = entries.size,
            onClearAll = { vm.clearAllEntries() },
        )

        Spacer(Modifier.height(8.dp))

        // List / Timeline
        if (entries.isEmpty()) {
            LogbookEmptyState(
                icon = Icons.Outlined.EditNote,
                message = if (store.entries.isEmpty()) "No entries yet" else "No matches",
            )
        } else {
            when (viewMode) {
                ViewMode.LIST -> EntryListView(
                    entries = entries,
                    onDelete = { vm.deleteEntry(it) },
                    onOpenSheet = onOpenSheet,
                )
                ViewMode.TIMELINE -> TimelineView(entries = entries)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Processes tab content
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProcessesTabContent(
    vm: LogbookViewModel,
    processes: List<LogbookProcess>,
    store: LogbookStore,
    typeFilter: ProcessTypeFilter,
    search: String,
    dateFilter: String,
    sortField: SortField,
    sortDir: SortDirection,
    onOpenSheet: (SheetType, String, ActiveTab, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Input card (added in 4B)
        ProcessInputCard(vm = vm)

        Spacer(Modifier.height(8.dp))

        // Filter + sort toolbar (added in 4B)
        ProcessToolbar(
            vm = vm,
            typeFilter = typeFilter,
            search = search,
            dateFilter = dateFilter,
            sortField = sortField,
            sortDir = sortDir,
            totalCount = store.processes.size,
            filteredCount = processes.size,
            onClearAll = { vm.clearAllProcesses() },
        )

        Spacer(Modifier.height(8.dp))

        // List
        if (processes.isEmpty()) {
            LogbookEmptyState(
                icon = Icons.Outlined.Loop,
                message = if (store.processes.isEmpty()) "No processes yet" else "No matches",
            )
        } else {
            ProcessListView(
                processes = processes,
                vm = vm,
                onDelete = { vm.deleteProcess(it) },
                onOpenSheet = onOpenSheet,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Empty state
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LogbookEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Hex colour helper (used across phases)
// ═══════════════════════════════════════════════════════════════════════════════

fun parseHexColor(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        val cleaned = hex.removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> (0xFF000000 or cleaned.toLong(16))
            8 -> cleaned.toLong(16)
            else -> return null
        }
        Color(argb.toInt())
    } catch (_: Exception) {
        null
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  STUBS — replaced in phases 4B, 4C, 4D
// ═══════════════════════════════════════════════════════════════════════════════

// ── 4B stubs: input cards & toolbars ────────────────────────────────

@Composable
private fun LogInputCard(vm: LogbookViewModel) {
    var text by remember { mutableStateOf("") }
    var showCustom by remember { mutableStateOf(false) }
    var customDate by remember { mutableStateOf(LocalDate.now()) }
    var customTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main input row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(S.logbook.optionalNote, style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(8.dp))
                // Custom timestamp toggle
                IconButton(
                    onClick = { showCustom = !showCustom },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (showCustom) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(Icons.Filled.EditCalendar, "Custom timestamp")
                }
                // Quick-log button
                Button(
                    onClick = {
                        vm.addEntry(text)
                        text = ""
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(S.logbook.log, style = MaterialTheme.typography.labelLarge)
                }
            }

            // Custom timestamp panel
            AnimatedVisibility(visible = showCustom) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Date field (native picker)
                        DatePickerField(
                            label = "Date",
                            date = customDate,
                            onDateSelected = { customDate = it },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        // Time field (native picker)
                        TimePickerField(
                            label = "Time",
                            time = customTime,
                            onTimeSelected = { customTime = it },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                vm.addCustomEntry(text, customDate, customTime)
                                text = ""
                                showCustom = false
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        ) {
                            Text("\u2726 " + S.logbook.log, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogToolbar(
    vm: LogbookViewModel,
    viewMode: ViewMode,
    typeFilter: EntryTypeFilter,
    search: String,
    dateFilter: String,
    sortField: SortField,
    sortDir: SortDirection,
    totalCount: Int,
    filteredCount: Int,
    onClearAll: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val hasActiveFilter = typeFilter != EntryTypeFilter.ALL || search.isNotBlank() || dateFilter.isNotBlank()

    Column {
        // Top row: result count + filter toggle + view toggle + clear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Result count
            Text(
                text = if (hasActiveFilter) "$filteredCount / $totalCount" else "$totalCount entries",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))

            // Filter toggle
            IconButton(onClick = { showFilters = !showFilters }) {
                BadgedBox(
                    badge = {
                        if (hasActiveFilter) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp),
                            ) {}
                        }
                    }
                ) {
                    Icon(
                        if (showFilters) Icons.Filled.FilterList else Icons.Outlined.FilterList,
                        "Filters",
                        tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // View mode toggle
            IconButton(onClick = {
                vm.setViewMode(if (viewMode == ViewMode.LIST) ViewMode.TIMELINE else ViewMode.LIST)
            }) {
                Icon(
                    if (viewMode == ViewMode.LIST) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.ViewTimeline,
                    "Toggle view",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Clear all
            if (totalCount > 0) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Outlined.DeleteSweep, "Clear all", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }

        // Filter panel
        AnimatedVisibility(visible = showFilters) {
            FilterPanel(
                searchValue = search,
                onSearchChange = { vm.setEntrySearch(it) },
                dateValue = dateFilter,
                onDateChange = { vm.setEntryDateFilter(it) },
                filterChips = {
                    EntryTypeFilter.entries.forEach { f ->
                        FilterChip(
                            selected = typeFilter == f,
                            onClick = { vm.setEntryTypeFilter(f) },
                            label = { Text(f.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                },
                onClearFilters = { vm.clearEntryFilters() },
                hasActiveFilter = hasActiveFilter,
            )
        }

        // Sort controls
        SortRow(
            sortField = sortField,
            sortDir = sortDir,
            onToggleSort = { vm.toggleEntrySort(it) },
            enabled = viewMode == ViewMode.LIST,
        )
    }

    // Clear-all confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(S.logbook.clearAllEntries) },
            text = { Text(S.logbook.clearAllEntriesMsg(totalCount)) },
            confirmButton = {
                TextButton(
                    onClick = { onClearAll(); showClearConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(S.logbook.deleteAll) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(S.common.cancel) }
            },
        )
    }
}

@Composable
private fun ProcessInputCard(vm: LogbookViewModel) {
    var text by remember { mutableStateOf("") }
    var showTemplates by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(S.logbook.processNamePlaceholder, style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(8.dp))

                // Template dropdown
                Box {
                    IconButton(onClick = { showTemplates = !showTemplates }) {
                        Icon(
                            Icons.Filled.Dashboard,
                            "Templates",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showTemplates,
                        onDismissRequest = { showTemplates = false },
                    ) {
                        ProcessTemplate.entries.forEach { tpl ->
                            DropdownMenuItem(
                                text = { Text(tpl.baseName) },
                                onClick = {
                                    vm.addProcessFromTemplate(tpl)
                                    showTemplates = false
                                },
                                leadingIcon = {
                                    Icon(
                                        when (tpl) {
                                            ProcessTemplate.DAILY_ROUTINE -> Icons.Filled.WbSunny
                                            ProcessTemplate.CONTENT_CREATION -> Icons.Filled.Create
                                            ProcessTemplate.BUG_FIX -> Icons.Filled.BugReport
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        vm.addProcess(text)
                        text = ""
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(S.logbook.add, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProcessToolbar(
    vm: LogbookViewModel,
    typeFilter: ProcessTypeFilter,
    search: String,
    dateFilter: String,
    sortField: SortField,
    sortDir: SortDirection,
    totalCount: Int,
    filteredCount: Int,
    onClearAll: () -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val hasActiveFilter = typeFilter != ProcessTypeFilter.ALL || search.isNotBlank() || dateFilter.isNotBlank()

    Column {
        // Top row: result count + filter toggle + clear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (hasActiveFilter) "$filteredCount / $totalCount" else "$totalCount processes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))

            // Filter toggle
            IconButton(onClick = { showFilters = !showFilters }) {
                BadgedBox(
                    badge = {
                        if (hasActiveFilter) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp),
                            ) {}
                        }
                    }
                ) {
                    Icon(
                        if (showFilters) Icons.Filled.FilterList else Icons.Outlined.FilterList,
                        "Filters",
                        tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (totalCount > 0) {
                IconButton(onClick = { showClearConfirm = true }) {
                    Icon(Icons.Outlined.DeleteSweep, "Clear all", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }

        // Filter panel
        AnimatedVisibility(visible = showFilters) {
            FilterPanel(
                searchValue = search,
                onSearchChange = { vm.setProcSearch(it) },
                dateValue = dateFilter,
                onDateChange = { vm.setProcDateFilter(it) },
                filterChips = {
                    ProcessTypeFilter.entries.forEach { f ->
                        FilterChip(
                            selected = typeFilter == f,
                            onClick = { vm.setProcTypeFilter(f) },
                            label = { Text(f.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (f == ProcessTypeFilter.OVERDUE)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = if (f == ProcessTypeFilter.OVERDUE)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.height(32.dp),
                        )
                    }
                },
                onClearFilters = { vm.clearProcFilters() },
                hasActiveFilter = hasActiveFilter,
            )
        }

        // Sort controls
        SortRow(
            sortField = sortField,
            sortDir = sortDir,
            onToggleSort = { vm.toggleProcSort(it) },
            enabled = true,
        )
    }

    // Clear-all confirmation dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(S.logbook.clearAllProcesses) },
            text = { Text(S.logbook.clearAllProcessesMsg(totalCount)) },
            confirmButton = {
                TextButton(
                    onClick = { onClearAll(); showClearConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(S.logbook.deleteAll) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(S.common.cancel) }
            },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Shared: FilterPanel + SortRow (used by both tabs)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FilterPanel(
    searchValue: String,
    onSearchChange: (String) -> Unit,
    dateValue: String,
    onDateChange: (String) -> Unit,
    filterChips: @Composable () -> Unit,
    onClearFilters: () -> Unit,
    hasActiveFilter: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Search
            OutlinedTextField(
                value = searchValue,
                onValueChange = onSearchChange,
                placeholder = { Text(S.logbook.searchPlaceholder, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchValue.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                shape = MaterialTheme.shapes.small,
            )

            Spacer(Modifier.height(8.dp))

            // Date filter (native picker)
            DatePickerField(
                label = "Filter by date",
                date = if (dateValue.isNotBlank()) try { LocalDate.parse(dateValue) } catch (_: Exception) { null } else null,
                onDateSelected = { onDateChange(it.toString()) },
                modifier = Modifier.fillMaxWidth(),
                clearable = true,
                onClear = { onDateChange("") },
            )

            Spacer(Modifier.height(8.dp))

            // Filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                filterChips()
            }

            // Clear filters
            if (hasActiveFilter) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onClearFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(S.logbook.clearFilters, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    sortField: SortField,
    sortDir: SortDirection,
    onToggleSort: (SortField) -> Unit,
    enabled: Boolean,
) {
    val alpha = if (enabled) 1f else 0.35f
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .then(if (!enabled) Modifier else Modifier),
    ) {
        Text(S.common.sort + ":", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))

        // Time sort
        val timeActive = sortField == SortField.TIME
        TextButton(
            onClick = { onToggleSort(SortField.TIME) },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (timeActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(S.logbook.time, style = MaterialTheme.typography.labelSmall)
            if (timeActive) {
                Icon(
                    if (sortDir == SortDirection.DESC) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Text sort
        val textActive = sortField == SortField.TEXT
        TextButton(
            onClick = { onToggleSort(SortField.TEXT) },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (textActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(S.logbook.name, style = MaterialTheme.typography.labelSmall)
            if (textActive) {
                Icon(
                    if (sortDir == SortDirection.DESC) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── 4C stubs: list views ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryListView(
    entries: List<LogbookEntry>,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab) -> Unit,
) {
    val listState = rememberLazyListState()
    // Auto-scroll to top when a new entry is prepended
    var previousCount by remember { mutableIntStateOf(entries.size) }
    LaunchedEffect(entries.size) {
        if (entries.size > previousCount && previousCount > 0) {
            listState.animateScrollToItem(0)
        }
        previousCount = entries.size
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(entries, key = { it.id }) { entry ->
            EntrySwipeCard(
                entry = entry,
                onDelete = { onDelete(entry.id) },
                onOpenSheet = { type -> onOpenSheet(type, entry.id, ActiveTab.LOG) },
            )
        }
        item { Spacer(Modifier.height(80.dp)) } // bottom padding for nav bar
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySwipeCard(
    entry: LogbookEntry,
    onDelete: () -> Unit,
    onOpenSheet: (SheetType) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { showDeleteConfirm = true; false }
                SwipeToDismissBoxValue.StartToEnd -> { showActions = true; false }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { it * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeBackground(dismissState)
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        EntryCardContent(entry = entry)
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(S.logbook.deleteEntry) },
            text = { Text(S.logbook.deleteEntryMsg(entry.text.take(50))) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(S.logbook.delete) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(S.common.cancel) } },
        )
    }

    // Actions dropdown
    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
        DropdownMenuItem(
            text = { Text(S.logbook.setBackground) },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BG) },
            leadingIcon = { Icon(Icons.Filled.FormatPaint, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.setBorder) },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BORDER) },
            leadingIcon = { Icon(Icons.Filled.BorderColor, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.changeTime) },
            onClick = { showActions = false; onOpenSheet(SheetType.TIME_EDITOR) },
            leadingIcon = { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.changeText) },
            onClick = { showActions = false; onOpenSheet(SheetType.TEXT_EDITOR) },
            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        )
    }
}

@Composable
private fun EntryCardContent(entry: LogbookEntry) {
    val bgColor = parseHexColor(entry.bgColor)
    val borderColor = parseHexColor(entry.borderColor)
    val todayStr = remember { LocalDate.now().toString() }
    val isToday = LogbookViewModel.isoToDateStr(entry.isoDate) == todayStr

    Card(
        colors = CardDefaults.cardColors(
            containerColor = bgColor ?: MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (borderColor != null) BorderStroke(1.5.dp, borderColor) else null,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Dot indicator
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            entry.custom -> MaterialTheme.colorScheme.tertiary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Text
                if (entry.text.isNotBlank()) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bgColor != null) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                // Timestamp
                Text(
                    text = LogbookViewModel.isoToDisplayDate(entry.isoDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (bgColor != null) Color.White.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Tags
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        entry.tags.forEach { tag ->
                            val tagColor = when (tag) {
                                "custom" -> MaterialTheme.colorScheme.tertiary
                                "edited" -> Color(0xFFA78BFA) // purple for edited
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                modifier = Modifier
                                    .background(tagColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                // Metrics
                if (entry.metrics.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val metricColor = if (bgColor != null) Color.White.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        entry.metrics.forEach { (key, value) ->
                            val displayName = WidgetMetric.fromKey(key)?.displayName ?: key
                            Text(
                                text = "$displayName: $value",
                                style = MaterialTheme.typography.labelSmall,
                                color = metricColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessListView(
    processes: List<LogbookProcess>,
    vm: LogbookViewModel,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    // Auto-scroll to top when a new process is prepended
    var previousCount by remember { mutableIntStateOf(processes.size) }
    LaunchedEffect(processes.size) {
        if (processes.size > previousCount && previousCount > 0) {
            listState.animateScrollToItem(0)
        }
        previousCount = processes.size
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(processes, key = { it.id }) { proc ->
            ProcessSwipeCard(
                process = proc,
                vm = vm,
                onDelete = { onDelete(proc.id) },
                onOpenSheet = { type, cpIdx -> onOpenSheet(type, proc.id, ActiveTab.PROCESSES, cpIdx) },
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessSwipeCard(
    process: LogbookProcess,
    vm: LogbookViewModel,
    onDelete: () -> Unit,
    onOpenSheet: (SheetType, Int) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { showDeleteConfirm = true; false }
                SwipeToDismissBoxValue.StartToEnd -> { showActions = true; false }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { it * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        ProcessCardContent(
            process = process,
            vm = vm,
            onCheckpointClick = { cpIdx -> onOpenSheet(SheetType.CHECKPOINT_DETAIL, cpIdx) },
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(S.logbook.deleteProcess) },
            text = { Text(S.logbook.deleteProcessMsg(process.text.take(50))) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(S.logbook.delete) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(S.common.cancel) } },
        )
    }

    // Actions dropdown
    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
        DropdownMenuItem(
            text = { Text(S.logbook.setBackground) },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BG, -1) },
            leadingIcon = { Icon(Icons.Filled.FormatPaint, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.setBorder) },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BORDER, -1) },
            leadingIcon = { Icon(Icons.Filled.BorderColor, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.changeTime) },
            onClick = { showActions = false; onOpenSheet(SheetType.TIME_EDITOR, -1) },
            leadingIcon = { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.changeText) },
            onClick = { showActions = false; onOpenSheet(SheetType.TEXT_EDITOR, -1) },
            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text(S.logbook.addCheckpoint) },
            onClick = { showActions = false; vm.addCheckpoint(process.id) },
            leadingIcon = { Icon(Icons.Filled.AddCircleOutline, null, tint = MaterialTheme.colorScheme.secondary) },
        )
    }
}

@Composable
private fun ProcessCardContent(
    process: LogbookProcess,
    vm: LogbookViewModel,
    onCheckpointClick: (Int) -> Unit,
) {
    val bgColor = parseHexColor(process.bgColor)
    val borderColor = parseHexColor(process.borderColor)
    val overdue = process.isOverdue()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = bgColor ?: MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.5.dp,
            borderColor ?: if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else Color.Transparent,
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Loop,
                    contentDescription = null,
                    tint = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = process.text,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (bgColor != null) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (overdue) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "OVERDUE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Timestamp
            Text(
                text = LogbookViewModel.isoToDisplayDate(process.isoDate),
                style = MaterialTheme.typography.bodySmall,
                color = if (bgColor != null) Color.White.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Tags
            if (process.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    process.tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA78BFA),
                            modifier = Modifier
                                .background(Color(0xFFA78BFA).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Checkpoint track
            CheckpointTrack(
                checkpoints = process.checkpoints,
                currentCheckpoint = process.currentCheckpoint,
                overdue = overdue,
                onCheckpointClick = onCheckpointClick,
                onAddCheckpoint = { vm.addCheckpoint(process.id) },
                bgColor = bgColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Checkpoint track (horizontal dots + connector line)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CheckpointTrack(
    checkpoints: List<Checkpoint>,
    currentCheckpoint: Int,
    overdue: Boolean,
    onCheckpointClick: (Int) -> Unit,
    onAddCheckpoint: () -> Unit,
    bgColor: Color?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        checkpoints.forEachIndexed { idx, cp ->
            val isCurrent = idx == currentCheckpoint
            val isCompleted = idx < currentCheckpoint
            val isOverdueCp = isCurrent && overdue

            // Dot
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCheckpointClick(idx) }
                    .padding(horizontal = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isOverdueCp -> MaterialTheme.colorScheme.error
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                        .then(
                            if (isCurrent) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Filled.Check, null,
                            tint = Color.White,
                            modifier = Modifier.size(7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cp.name.take(8),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = when {
                        isCurrent -> if (bgColor != null) Color.White else MaterialTheme.colorScheme.primary
                        isCompleted -> if (bgColor != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary
                        else -> if (bgColor != null) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Connector line between dots
            if (idx < checkpoints.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(
                            if (idx < currentCheckpoint) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        ),
                )
            }
        }

        // Add checkpoint button
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick = onAddCheckpoint,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Filled.AddCircleOutline,
                "Add checkpoint",
                tint = if (bgColor != null) Color.White.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Swipe background (shared by entry + process cards)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        label = "swipeBg",
    )
    val icon = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
        SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.MoreHoriz
        else -> Icons.Filled.MoreHoriz
    }
    val iconTint = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val alignment = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
    }
}

// ── 4D stubs: timeline + bottom sheets ──────────────────────────────

@Composable
private fun TimelineView(entries: List<LogbookEntry>) {
    val todayStr = remember { LocalDate.now().toString() }

    // Group entries by date (chronological order within each day)
    val dayGroups = remember(entries) {
        val grouped = mutableMapOf<String, MutableList<LogbookEntry>>()
        entries.sortedBy { it.isoDate }.forEach { e ->
            val d = LogbookViewModel.isoToDateStr(e.isoDate)
            grouped.getOrPut(d) { mutableListOf() }.add(e)
        }
        // Reverse: newest day first
        grouped.entries.sortedByDescending { it.key }
    }

    if (dayGroups.isEmpty()) return

    // Legend
    val hasAuto = entries.any { !it.custom }
    val hasCustom = entries.any { it.custom }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Legend row
        if (hasAuto || hasCustom) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    if (hasAuto) {
                        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Auto-logged")
                    }
                    if (hasCustom) {
                        LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "Custom")
                    }
                }
            }
        }

        // Day sections
        dayGroups.forEach { (dateStr, dayEntries) ->
            item(key = "day_$dateStr") {
                val isToday = dateStr == todayStr
                TimelineDaySection(
                    dateStr = dateStr,
                    isToday = isToday,
                    entries = dayEntries,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineDaySection(
    dateStr: String,
    isToday: Boolean,
    entries: List<LogbookEntry>,
) {
    Column {
        // Day header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isToday) "Today" else dateStr,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${entries.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        // Horizontal scrollable timeline row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top,
        ) {
            entries.forEachIndexed { idx, entry ->
                TimelineNode(entry = entry, isToday = isToday)
                if (idx < entries.lastIndex) {
                    // Connector stem
                    Box(
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineNode(entry: LogbookEntry, isToday: Boolean) {
    val dotColor = when {
        entry.custom -> MaterialTheme.colorScheme.tertiary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val bgColor = parseHexColor(entry.bgColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp),
    ) {
        // Time label
        val timeStr = try {
            val instant = Instant.parse(entry.isoDate)
            val zdt = instant.atZone(ZoneId.systemDefault())
            "%02d:%02d".format(zdt.hour, zdt.minute)
        } catch (_: Exception) { "" }

        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(2.dp))

        // Dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )

        Spacer(Modifier.height(4.dp))

        // Stem
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(12.dp)
                .background(dotColor.copy(alpha = 0.4f)),
        )

        Spacer(Modifier.height(4.dp))

        // Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = bgColor ?: MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                if (entry.text.isNotBlank()) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = if (bgColor != null) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entry.tags.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = entry.tags.joinToString(" "),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = if (entry.custom) MaterialTheme.colorScheme.tertiary
                        else Color(0xFFA78BFA),
                    )
                }
                if (entry.metrics.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    val metricTextColor = if (bgColor != null) Color.White.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    entry.metrics.entries.take(3).forEach { (key, value) ->
                        val name = WidgetMetric.fromKey(key)?.displayName ?: key
                        Text(
                            text = "$name: $value",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = metricTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (entry.metrics.size > 3) {
                        Text(
                            text = "+${entry.metrics.size - 3} more",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = metricTextColor,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogbookBottomSheets(
    sheetType: SheetType?,
    targetId: String,
    cpIdx: Int,
    tab: ActiveTab,
    vm: LogbookViewModel,
    store: LogbookStore,
    onDismiss: () -> Unit,
) {
    if (sheetType == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        when (sheetType) {
            SheetType.CHECKPOINT_DETAIL -> CheckpointDetailSheet(
                procId = targetId, cpIdx = cpIdx, vm = vm, store = store, onDismiss = onDismiss,
            )
            SheetType.TEXT_EDITOR -> TextEditorSheet(
                targetId = targetId, tab = tab, vm = vm, store = store, onDismiss = onDismiss,
            )
            SheetType.TIME_EDITOR -> TimeEditorSheet(
                targetId = targetId, tab = tab, vm = vm, store = store, onDismiss = onDismiss,
            )
            SheetType.COLOR_PICKER_BG -> ColorPickerSheet(
                targetId = targetId, tab = tab, mode = "bg", vm = vm, store = store, onDismiss = onDismiss,
            )
            SheetType.COLOR_PICKER_BORDER -> ColorPickerSheet(
                targetId = targetId, tab = tab, mode = "border", vm = vm, store = store, onDismiss = onDismiss,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Checkpoint detail sheet
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CheckpointDetailSheet(
    procId: String,
    cpIdx: Int,
    vm: LogbookViewModel,
    store: LogbookStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val proc = store.processes.find { it.id == procId } ?: run { onDismiss(); return }
    val cp = proc.checkpoints.getOrNull(cpIdx) ?: run { onDismiss(); return }
    val curCp = proc.currentCheckpoint

    var name by remember(cp.id) { mutableStateOf(cp.name) }
    var comment by remember(cp.id) { mutableStateOf(cp.comment) }
    var dueDate by remember(cp.id) { mutableStateOf(cp.dueDate) }
    var remindAt by remember(cp.id) { mutableStateOf(cp.remindAt) }
    var notify by remember(cp.id) { mutableStateOf(cp.notify) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Capture composable strings for non-composable onClick lambdas
    val strMovedTo = S.logbook.movedTo(cp.name)
    val strSetReminderFirst = S.logbook.setReminderFirst
    val strCheckpointUpdated = S.logbook.checkpointUpdated

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Title
        Text(
            text = cp.name.ifBlank { "Checkpoint ${cpIdx + 1}" },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))

        // Status
        val statusText = when {
            cpIdx == curCp -> "\u25CF Current checkpoint"
            cpIdx < curCp -> "\u2713 Completed"
            else -> "\u25CB Upcoming"
        }
        val statusColor = when {
            cpIdx == curCp -> MaterialTheme.colorScheme.primary
            cpIdx < curCp -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)

        Spacer(Modifier.height(16.dp))

        // Name field
        Text(S.logbook.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text(S.logbook.checkpointNamePlaceholder) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(12.dp))

        // Comment field
        Text(S.logbook.commentNote, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = comment, onValueChange = { comment = it },
            placeholder = { Text(S.logbook.addNote) },
            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(12.dp))

        // Due date (native picker)
        Text(S.logbook.dueDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        DatePickerField(
            label = "Due Date",
            date = if (dueDate.isNotBlank()) try { LocalDate.parse(dueDate) } catch (_: Exception) { null } else null,
            onDateSelected = { dueDate = it.toString() },
            modifier = Modifier.fillMaxWidth(),
            clearable = true,
            onClear = { dueDate = "" },
        )

        Spacer(Modifier.height(12.dp))

        // Reminder time (native date + time pickers)
        Text(S.logbook.reminderTime, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        DateTimePickerField(
            label = "Reminder",
            dateTimeIso = remindAt,
            onDateTimeSelected = { remindAt = it },
            modifier = Modifier.fillMaxWidth(),
            clearable = true,
            onClear = { remindAt = "" },
        )

        Spacer(Modifier.height(8.dp))

        // Notify toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(S.logbook.enableReminder, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = notify,
                onCheckedChange = { notify = it },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Button row: Jump + Save
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    vm.jumpToCheckpoint(procId, cpIdx)
                    onDismiss()
                    Toast.makeText(context, strMovedTo, Toast.LENGTH_SHORT).show()
                },
                enabled = cpIdx != curCp,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(if (cpIdx == curCp) S.logbook.current else S.logbook.jumpHere)
            }

            Button(
                onClick = {
                    if (notify && remindAt.isBlank()) {
                        Toast.makeText(context, strSetReminderFirst, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    vm.updateCheckpoint(procId, cpIdx, name, comment, dueDate, remindAt, notify)
                    onDismiss()
                    Toast.makeText(context, strCheckpointUpdated, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(S.logbook.save)
            }
        }

        // Delete checkpoint (only if >1)
        if (proc.checkpoints.size > 1) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Text(S.logbook.deleteCheckpoint)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(S.logbook.deleteCheckpointConfirm) },
            text = { Text(S.logbook.deleteProcessMsg(cp.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteCheckpoint(procId, cpIdx)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(S.logbook.delete) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(S.common.cancel) } },
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Text editor sheet
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TextEditorSheet(
    targetId: String,
    tab: ActiveTab,
    vm: LogbookViewModel,
    store: LogbookStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentText = remember(targetId, tab) {
        if (tab == ActiveTab.LOG) store.entries.find { it.id == targetId }?.text ?: ""
        else store.processes.find { it.id == targetId }?.text ?: ""
    }
    var text by remember(targetId) { mutableStateOf(currentText) }
    val strTextUpdated = S.logbook.textUpdated

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(S.logbook.changeText, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text(S.logbook.enterText) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.common.cancel) }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.updateEntryText(targetId, text)
                    else vm.updateProcessText(targetId, text)
                    onDismiss()
                    Toast.makeText(context, strTextUpdated, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.logbook.save) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Time editor sheet
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TimeEditorSheet(
    targetId: String,
    tab: ActiveTab,
    vm: LogbookViewModel,
    store: LogbookStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentIso = remember(targetId, tab) {
        if (tab == ActiveTab.LOG) store.entries.find { it.id == targetId }?.isoDate ?: ""
        else store.processes.find { it.id == targetId }?.isoDate ?: ""
    }

    val initialDate = remember(currentIso) {
        try {
            Instant.parse(currentIso).atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) { LocalDate.now() }
    }
    val initialTime = remember(currentIso) {
        try {
            Instant.parse(currentIso).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)
        } catch (_: Exception) { LocalTime.now().withSecond(0).withNano(0) }
    }

    var date by remember(targetId) { mutableStateOf(initialDate) }
    var time by remember(targetId) { mutableStateOf(initialTime) }
    val strTimeUpdated = S.logbook.timeUpdated

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(S.logbook.changeTime, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        DatePickerField(
            label = "Date",
            date = date,
            onDateSelected = { date = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        TimePickerField(
            label = "Time",
            time = time,
            onTimeSelected = { time = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.common.cancel) }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.updateEntryTime(targetId, date, time)
                    else vm.updateProcessTime(targetId, date, time)
                    onDismiss()
                    Toast.makeText(context, strTimeUpdated, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.logbook.save) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Color picker sheet
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ColorPickerSheet(
    targetId: String,
    tab: ActiveTab,
    mode: String, // "bg" or "border"
    vm: LogbookViewModel,
    store: LogbookStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val palette = store.palette

    // Current color for this item
    val currentColor = remember(targetId, tab, mode) {
        val item: Any? = if (tab == ActiveTab.LOG) store.entries.find { it.id == targetId }
        else store.processes.find { it.id == targetId }
        when {
            item is LogbookEntry && mode == "bg" -> item.bgColor
            item is LogbookEntry -> item.borderColor
            item is LogbookProcess && mode == "bg" -> item.bgColor
            item is LogbookProcess -> item.borderColor
            else -> ""
        }
    }

    var selectedColor by remember(targetId) { mutableStateOf(currentColor) }
    val strColorApplied = S.logbook.colorApplied

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = if (mode == "bg") S.logbook.setBackground else S.logbook.setBorder,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))

        // Palette swatches
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            palette.forEach { hex ->
                val color = parseHexColor(hex) ?: Color.Gray
                val isSelected = selectedColor == hex
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                        )
                        .clickable { selectedColor = hex },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // "None" option
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { selectedColor = "" }
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (selectedColor.isBlank()) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(S.common.none, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(12.dp))

        // Palette editing hint
        Text(
            text = "Long-press a swatch to edit its color",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.common.cancel) }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.applyEntryColor(targetId, mode, selectedColor)
                    else vm.applyProcessColor(targetId, mode, selectedColor)
                    onDismiss()
                    Toast.makeText(context, strColorApplied, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text(S.common.apply) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Native Date / Time Picker Fields
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayText = date?.toString() ?: ""

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(S.logbook.tapToSelect, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = modifier.clickable { showDialog = true },
        textStyle = MaterialTheme.typography.bodySmall,
        leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) },
        trailingIcon = {
            if (clearable && displayText.isNotBlank() && onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        },
        shape = MaterialTheme.shapes.small,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect { interaction ->
                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                        showDialog = true
                    }
                }
            }
        },
    )

    if (showDialog) {
        val initial = date ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        onDateSelected(selected)
                    }
                    showDialog = false
                }) { Text(S.common.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(S.common.cancel) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val displayText = "%02d:%02d".format(time.hour, time.minute)

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.clickable { showDialog = true },
        textStyle = MaterialTheme.typography.bodySmall,
        leadingIcon = { Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)) },
        shape = MaterialTheme.shapes.small,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect { interaction ->
                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                        showDialog = true
                    }
                }
            }
        },
    )

    if (showDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(S.common.selectTime) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(LocalTime.of(pickerState.hour, pickerState.minute))
                    showDialog = false
                }) { Text(S.common.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(S.common.cancel) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    dateTimeIso: String,
    onDateTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    clearable: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    // Parse existing value
    val parsed = remember(dateTimeIso) {
        if (dateTimeIso.isBlank()) null
        else try {
            LocalDateTime.parse(dateTimeIso)
        } catch (_: Exception) {
            try {
                Instant.parse(dateTimeIso).atZone(ZoneId.systemDefault()).toLocalDateTime()
            } catch (_: Exception) { null }
        }
    }

    val displayText = if (parsed != null) {
        "%s %02d:%02d".format(parsed.toLocalDate(), parsed.hour, parsed.minute)
    } else ""

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text(S.logbook.tapToSelectDateTime, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = modifier.clickable { showDateDialog = true },
        textStyle = MaterialTheme.typography.bodySmall,
        leadingIcon = { Icon(Icons.Filled.Alarm, null, Modifier.size(18.dp)) },
        trailingIcon = {
            if (clearable && displayText.isNotBlank() && onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(18.dp)) {
                    Icon(Icons.Filled.Clear, "Clear")
                }
            }
        },
        shape = MaterialTheme.shapes.small,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect { interaction ->
                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                        showDateDialog = true
                    }
                }
            }
        },
    )

    // Step 1: Date picker
    if (showDateDialog) {
        val initialDate = parsed?.toLocalDate() ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        pendingDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                        showDateDialog = false
                        showTimeDialog = true
                    }
                }) { Text(S.common.next) }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) { Text(S.common.cancel) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    // Step 2: Time picker
    if (showTimeDialog) {
        val initialTime = parsed?.toLocalTime() ?: LocalTime.now().withSecond(0).withNano(0)
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text(S.common.selectTime) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = pendingDate ?: LocalDate.now()
                    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val ldt = LocalDateTime.of(selectedDate, selectedTime)
                    onDateTimeSelected(ldt.toString())
                    showTimeDialog = false
                }) { Text(S.common.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = false }) { Text(S.common.cancel) }
            },
        )
    }
}
