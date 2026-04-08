package com.hardwaredash.ui.ticked

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ---- Schema version (matches Ticked web app v6) ----
const val TICKED_SCHEMA_VERSION = 6

// ---- Default colour palette (matches web app) ----
val DEFAULT_PALETTE = listOf("#05004d", "#002e0d", "#2b0026", "#363506", "#3b0000")

// ---- Core data classes ----

@Serializable
data class Checkpoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val timestamp: String = "",   // ISO 8601 or empty
    val comment: String = "",
    val dueDate: String = "",     // YYYY-MM-DD or empty
    val remindAt: String = "",    // ISO datetime-local or empty
    val notify: Boolean = false,
)

@Serializable
data class TickedEntry(
    val id: String = UUID.randomUUID().toString(),
    val isoDate: String = "",
    val text: String = "",
    val custom: Boolean = false,
    val bgColor: String = "",
    val borderColor: String = "",
    val tags: List<String> = emptyList(),
)

@Serializable
data class TickedProcess(
    val id: String = UUID.randomUUID().toString(),
    val isoDate: String = "",
    val text: String = "",
    val custom: Boolean = false,
    val bgColor: String = "",
    val borderColor: String = "",
    val tags: List<String> = emptyList(),
    val currentCheckpoint: Int = 0,
    val checkpoints: List<Checkpoint> = emptyList(),
)

/** Top-level persistence envelope (mirrors web app's localStorage shape). */
@Serializable
data class TickedStore(
    val version: Int = TICKED_SCHEMA_VERSION,
    val savedAt: String = "",
    val palette: List<String> = DEFAULT_PALETTE,
    val entries: List<TickedEntry> = emptyList(),
    val processes: List<TickedProcess> = emptyList(),
)

/** JSON export envelope (cross-compatible with web app). */
@Serializable
data class TickedExport(
    val app: String = "Ticked",
    val version: Int = TICKED_SCHEMA_VERSION,
    val exportedAt: String = "",
    val palette: List<String> = DEFAULT_PALETTE,
    val entries: List<TickedEntry> = emptyList(),
    val processes: List<TickedProcess> = emptyList(),
)

// ---- Process templates (matches web app's 3 built-in templates) ----

enum class ProcessTemplate(
    val baseName: String,
    val checkpointNames: List<String>,
) {
    DAILY_ROUTINE("Daily Routine", listOf("Plan day", "Focus block", "Admin tasks", "Wrap-up")),
    CONTENT_CREATION("Content Creation", listOf("Research", "Outline", "Draft", "Edit", "Publish")),
    BUG_FIX("Bug Fix", listOf("Reproduce", "Root cause", "Implement fix", "Test", "Deploy")),
}

// ---- Filter / sort / view enums ----

enum class EntryTypeFilter(val label: String) {
    ALL("All"),
    AUTO("Auto-logged"),
    CUSTOM("\u2726 Custom"),   // ✦
    EDITED("\u270E Edited"),   // ✎
}

enum class ProcessTypeFilter(val label: String) {
    ALL("All"),
    EDITED("\u270E Edited"),
    OVERDUE("\u26A0 Overdue"),
}

enum class SortField { TIME, TEXT }
enum class SortDirection { DESC, ASC }
enum class ViewMode { LIST, TIMELINE }
enum class ActiveTab { LOG, PROCESSES }

// ---- Helpers ----

fun TickedProcess.isOverdue(): Boolean {
    val cp = checkpoints.getOrNull(currentCheckpoint) ?: return false
    if (cp.dueDate.isBlank()) return false
    return try {
        val dueEnd = java.time.LocalDate.parse(cp.dueDate).atTime(23, 59, 59)
        dueEnd.isBefore(java.time.LocalDateTime.now())
    } catch (_: Exception) {
        false
    }
}

fun countOverdue(processes: List<TickedProcess>): Int =
    processes.count { it.isOverdue() }
