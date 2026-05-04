package com.gadget.data.db.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-`appWidgetId` configuration row for a placed folder widget. Stored in
 * Room (rather than SharedPreferences) so it benefits from the same backup
 * sweep as the rest of the App-Organizer module and so widget receivers can
 * use the same DAO surface as the in-app screens.
 *
 * `sizeVariant` is the literal grid size of the widget the user placed
 * ("2x2", "4x2", …) so the renderer can pick a layout without re-deriving
 * size from runtime min-width/height.
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
