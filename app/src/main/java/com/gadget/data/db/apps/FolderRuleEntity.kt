package com.gadget.data.db.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1:1 with `Folder`. Stores the folder's smart-rule configuration as a JSON blob
 * that the rule-engine layer (added in a later batch) parses into a sealed
 * `FolderRule` ADT. Folders without a row here are purely manual.
 */
@Entity(tableName = "apps_folder_rule")
data class FolderRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "folder_id")
    val folderId: Long,
    @ColumnInfo(name = "rule_json")
    val ruleJson: String,
)
