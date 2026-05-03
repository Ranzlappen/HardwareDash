package com.gadget.data.db.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined folder of apps that can be exposed as a home-screen widget.
 *
 * `coverIcon` uses a small ADT-as-string convention so the column can carry any of
 * `emoji:🎮`, `image:<absolute path>`, `app:<package>`, or `auto` (derived).
 */
@Entity(tableName = "apps_folder")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "base_color_argb")
    val baseColorArgb: Int,
    @ColumnInfo(name = "cover_icon")
    val coverIcon: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "locked")
    val locked: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
