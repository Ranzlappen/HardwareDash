package com.gadget.data.db.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Manual membership of an app inside a folder. Smart/rule-driven folders compute
 * their contents at query time and don't write rows here.
 */
@Entity(
    tableName = "apps_folder_app",
    primaryKeys = ["folder_id", "app_key"],
    indices = [Index(value = ["folder_id"])],
)
data class FolderApp(
    @ColumnInfo(name = "folder_id")
    val folderId: Long,
    @ColumnInfo(name = "app_key")
    val appKey: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
)
