package dev.ranzlappen.gadget.core.data.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-`appWidgetId` configuration row for a placed folder widget, preserved here
 * so a legacy `gadget_db` import (in-place app upgrade) keeps already-placed
 * folder widgets bound to their folder — the system-assigned `appWidgetId`s are
 * still valid across an in-place update (unlike a cross-device restore, where
 * the user re-pins and the OS mints fresh ids).
 *
 * The modular runtime additionally mirrors per-instance config into the
 * widgetkit `WidgetConfigStore` (DataStore) for the launcher/content widget
 * archetype; this table remains the legacy-ingestion seam.
 *
 * `sizeVariant` is the literal grid size of the placed widget ("2x2", "1x1", …)
 * so the renderer can pick a layout without re-deriving size at runtime.
 */
@Entity(
    tableName = "apps_widget_config",
    indices = [Index(value = ["folder_id"])],
)
data class FolderWidgetConfig(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo(name = "folder_id")
    val folderId: Long,
    @ColumnInfo(name = "size_variant")
    val sizeVariant: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
