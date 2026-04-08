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
    // STUB — replaced in 4B
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
    // STUB — replaced in 4B
}

@Composable
private fun ProcessInputCard(vm: TickedViewModel) {
    // STUB — replaced in 4B
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
    // STUB — replaced in 4B
}

// ── 4C stubs: list views ────────────────────────────────────────────

@Composable
private fun EntryListView(
    entries: List<TickedEntry>,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab) -> Unit,
) {
    // STUB — replaced in 4C
}

@Composable
private fun ProcessListView(
    processes: List<TickedProcess>,
    vm: TickedViewModel,
    onDelete: (String) -> Unit,
    onOpenSheet: (SheetType, String, ActiveTab, Int) -> Unit,
) {
    // STUB — replaced in 4C
}

// ── 4D stubs: timeline + bottom sheets ──────────────────────────────

@Composable
private fun TimelineView(entries: List<TickedEntry>) {
    // STUB — replaced in 4D
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
    // STUB — replaced in 4D
}
