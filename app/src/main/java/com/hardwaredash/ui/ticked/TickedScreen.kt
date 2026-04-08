package com.hardwaredash.ui.ticked

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// ═══════════════════════════════════════════════════════════════════════════════
//  Main entry point
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickedScreen(vm: TickedViewModel = viewModel()) {
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

    // ── Import / Export launchers ────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = vm.buildExportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
            val (newE, newP) = vm.importJson(json)
            Toast.makeText(context, "Imported $newE entries, $newP processes", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Main layout ─────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        // Header
        TickedHeader(
            entryCount = store.entries.size,
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onExport = {
                val fileName = "ticked-export-${LocalDate.now()}.json"
                exportLauncher.launch(fileName)
            },
        )

        Spacer(Modifier.height(8.dp))

        // Tab row
        TickedTabRow(
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
    TickedBottomSheets(
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
private fun TickedHeader(
    entryCount: Int,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
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
            text = "Ticked",
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
private fun TickedTabRow(
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
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[idx]),
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
                    Text("Log")
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
                    Text("Processes")
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
    vm: TickedViewModel,
    entries: List<TickedEntry>,
    store: TickedStore,
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
            TickedEmptyState(
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
    vm: TickedViewModel,
    processes: List<TickedProcess>,
    store: TickedStore,
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
            TickedEmptyState(
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
private fun TickedEmptyState(
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
private fun LogInputCard(vm: TickedViewModel) {
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
                    placeholder = { Text("Optional note\u2026", style = MaterialTheme.typography.bodyMedium) },
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
                    Text("Log", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Custom timestamp panel
            AnimatedVisibility(visible = showCustom) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Date field
                        OutlinedTextField(
                            value = customDate.toString(),
                            onValueChange = {
                                try { customDate = LocalDate.parse(it) } catch (_: Exception) {}
                            },
                            label = { Text("Date") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            shape = MaterialTheme.shapes.small,
                        )
                        Spacer(Modifier.width(8.dp))
                        // Time field
                        OutlinedTextField(
                            value = "%02d:%02d".format(customTime.hour, customTime.minute),
                            onValueChange = { v ->
                                try {
                                    val parts = v.split(":")
                                    if (parts.size == 2) {
                                        customTime = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                                    }
                                } catch (_: Exception) {}
                            },
                            label = { Text("Time") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            shape = MaterialTheme.shapes.small,
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
                            Text("\u2726 Log", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogToolbar(
    vm: TickedViewModel,
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
                    if (viewMode == ViewMode.LIST) Icons.Filled.ViewList else Icons.Filled.ViewTimeline,
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
            title = { Text("Clear all entries?") },
            text = { Text("This will delete all $totalCount entries. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onClearAll(); showClearConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProcessInputCard(vm: TickedViewModel) {
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
                    placeholder = { Text("Process name\u2026", style = MaterialTheme.typography.bodyMedium) },
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
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProcessToolbar(
    vm: TickedViewModel,
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
            title = { Text("Clear all processes?") },
            text = { Text("This will delete all $totalCount processes and their reminders. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { onClearAll(); showClearConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
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
                placeholder = { Text("Search\u2026", style = MaterialTheme.typography.bodySmall) },
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

            // Date filter
            OutlinedTextField(
                value = dateValue,
                onValueChange = onDateChange,
                placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (dateValue.isNotBlank()) {
                        IconButton(onClick = { onDateChange("") }, modifier = Modifier.size(18.dp)) {
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
                    Text("Clear filters", style = MaterialTheme.typography.labelSmall)
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
        Text("Sort:", style = MaterialTheme.typography.labelSmall,
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
            Text("Time", style = MaterialTheme.typography.labelSmall)
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
            Text("Name", style = MaterialTheme.typography.labelSmall)
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
    entries: List<TickedEntry>,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab) -> Unit,
) {
    val listState = rememberLazyListState()
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
    entry: TickedEntry,
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
            title = { Text("Delete entry?") },
            text = { Text("\"${entry.text.take(50)}\" will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    // Actions dropdown
    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
        DropdownMenuItem(
            text = { Text("Set Background") },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BG) },
            leadingIcon = { Icon(Icons.Filled.FormatPaint, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Set Border") },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BORDER) },
            leadingIcon = { Icon(Icons.Filled.BorderColor, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Change Time") },
            onClick = { showActions = false; onOpenSheet(SheetType.TIME_EDITOR) },
            leadingIcon = { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Change Text") },
            onClick = { showActions = false; onOpenSheet(SheetType.TEXT_EDITOR) },
            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        )
    }
}

@Composable
private fun EntryCardContent(entry: TickedEntry) {
    val bgColor = parseHexColor(entry.bgColor)
    val borderColor = parseHexColor(entry.borderColor)
    val todayStr = remember { LocalDate.now().toString() }
    val isToday = TickedViewModel.isoToDateStr(entry.isoDate) == todayStr

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
                    text = TickedViewModel.isoToDisplayDate(entry.isoDate),
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessListView(
    processes: List<TickedProcess>,
    vm: TickedViewModel,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab, Int) -> Unit,
) {
    val listState = rememberLazyListState()
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
    process: TickedProcess,
    vm: TickedViewModel,
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
            title = { Text("Delete process?") },
            text = { Text("\"${process.text.take(50)}\" and all its checkpoints will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    // Actions dropdown
    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
        DropdownMenuItem(
            text = { Text("Set Background") },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BG, -1) },
            leadingIcon = { Icon(Icons.Filled.FormatPaint, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Set Border") },
            onClick = { showActions = false; onOpenSheet(SheetType.COLOR_PICKER_BORDER, -1) },
            leadingIcon = { Icon(Icons.Filled.BorderColor, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Change Time") },
            onClick = { showActions = false; onOpenSheet(SheetType.TIME_EDITOR, -1) },
            leadingIcon = { Icon(Icons.Filled.Schedule, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Change Text") },
            onClick = { showActions = false; onOpenSheet(SheetType.TEXT_EDITOR, -1) },
            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        )
        DropdownMenuItem(
            text = { Text("Add Checkpoint") },
            onClick = { showActions = false; vm.addCheckpoint(process.id) },
            leadingIcon = { Icon(Icons.Filled.AddCircleOutline, null, tint = MaterialTheme.colorScheme.secondary) },
        )
    }
}

@Composable
private fun ProcessCardContent(
    process: TickedProcess,
    vm: TickedViewModel,
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
                text = TickedViewModel.isoToDisplayDate(process.isoDate),
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
private fun TimelineView(entries: List<TickedEntry>) {
    val todayStr = remember { LocalDate.now().toString() }

    // Group entries by date (chronological order within each day)
    val dayGroups = remember(entries) {
        val grouped = mutableMapOf<String, MutableList<TickedEntry>>()
        entries.sortedBy { it.isoDate }.forEach { e ->
            val d = TickedViewModel.isoToDateStr(e.isoDate)
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
    entries: List<TickedEntry>,
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
private fun TimelineNode(entry: TickedEntry, isToday: Boolean) {
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TickedBottomSheets(
    sheetType: SheetType?,
    targetId: String,
    cpIdx: Int,
    tab: ActiveTab,
    vm: TickedViewModel,
    store: TickedStore,
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
    vm: TickedViewModel,
    store: TickedStore,
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
        Text("Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            placeholder = { Text("Checkpoint name\u2026") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(12.dp))

        // Comment field
        Text("Comment / Note", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = comment, onValueChange = { comment = it },
            placeholder = { Text("Add a note\u2026") },
            minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(12.dp))

        // Due date
        Text("Due Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = dueDate, onValueChange = { dueDate = it },
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (dueDate.isNotBlank()) {
                    IconButton(onClick = { dueDate = "" }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(12.dp))

        // Reminder time
        Text("Reminder Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = remindAt, onValueChange = { remindAt = it },
            placeholder = { Text("YYYY-MM-DDThh:mm") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Alarm, null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (remindAt.isNotBlank()) {
                    IconButton(onClick = { remindAt = "" }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(8.dp))

        // Notify toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable reminder", style = MaterialTheme.typography.bodyMedium)
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
                    Toast.makeText(context, "Moved to \"${cp.name}\"", Toast.LENGTH_SHORT).show()
                },
                enabled = cpIdx != curCp,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(if (cpIdx == curCp) "Current" else "Jump here")
            }

            Button(
                onClick = {
                    if (notify && remindAt.isBlank()) {
                        Toast.makeText(context, "Set a reminder time first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    vm.updateCheckpoint(procId, cpIdx, name, comment, dueDate, remindAt, notify)
                    onDismiss()
                    Toast.makeText(context, "Checkpoint updated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("Save")
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
                Text("Delete Checkpoint")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete checkpoint?") },
            text = { Text("\"${cp.name}\" will be removed from this process.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.deleteCheckpoint(procId, cpIdx)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
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
    vm: TickedViewModel,
    store: TickedStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentText = remember(targetId, tab) {
        if (tab == ActiveTab.LOG) store.entries.find { it.id == targetId }?.text ?: ""
        else store.processes.find { it.id == targetId }?.text ?: ""
    }
    var text by remember(targetId) { mutableStateOf(currentText) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("Change Text", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("Enter text\u2026") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text("Cancel") }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.updateEntryText(targetId, text)
                    else vm.updateProcessText(targetId, text)
                    onDismiss()
                    Toast.makeText(context, "Text updated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text("Save") }
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
    vm: TickedViewModel,
    store: TickedStore,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text("Change Time", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = date.toString(), onValueChange = { try { date = LocalDate.parse(it) } catch (_: Exception) {} },
            label = { Text("Date") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)) },
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = "%02d:%02d".format(time.hour, time.minute),
            onValueChange = { v ->
                try {
                    val parts = v.split(":")
                    if (parts.size == 2) time = LocalTime.of(parts[0].toInt(), parts[1].toInt())
                } catch (_: Exception) {}
            },
            label = { Text("Time") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)) },
            shape = MaterialTheme.shapes.small,
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text("Cancel") }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.updateEntryTime(targetId, date, time)
                    else vm.updateProcessTime(targetId, date, time)
                    onDismiss()
                    Toast.makeText(context, "Time updated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text("Save") }
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
    vm: TickedViewModel,
    store: TickedStore,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val palette = store.palette

    // Current color for this item
    val currentColor = remember(targetId, tab, mode) {
        val item = if (tab == ActiveTab.LOG) store.entries.find { it.id == targetId }
        else store.processes.find { it.id == targetId }
        when {
            item is TickedEntry && mode == "bg" -> item.bgColor
            item is TickedEntry -> item.borderColor
            item is TickedProcess && mode == "bg" -> item.bgColor
            item is TickedProcess -> item.borderColor
            else -> ""
        }
    }

    var selectedColor by remember(targetId) { mutableStateOf(currentColor) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = if (mode == "bg") "Set Background" else "Set Border",
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
            Text("None", style = MaterialTheme.typography.bodyMedium)
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
            ) { Text("Cancel") }

            Button(
                onClick = {
                    if (tab == ActiveTab.LOG) vm.applyEntryColor(targetId, mode, selectedColor)
                    else vm.applyProcessColor(targetId, mode, selectedColor)
                    onDismiss()
                    Toast.makeText(context, "Color applied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small,
            ) { Text("Apply") }
        }
    }
}
