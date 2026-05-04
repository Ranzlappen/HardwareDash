package com.gadget.data.db.apps

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppsDao {

    // ── Folders ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Query("DELETE FROM apps_folder WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query("SELECT * FROM apps_folder ORDER BY sort_order ASC, id ASC")
    fun observeFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM apps_folder WHERE id = :id")
    suspend fun getFolder(id: Long): Folder?

    // ── Manual folder ↔ app membership ──────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApp(membership: FolderApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApps(memberships: List<FolderApp>)

    @Query("DELETE FROM apps_folder_app WHERE folder_id = :folderId AND app_key = :appKey")
    suspend fun removeFolderApp(folderId: Long, appKey: String)

    @Query("DELETE FROM apps_folder_app WHERE folder_id = :folderId")
    suspend fun clearFolderMembership(folderId: Long)

    @Query("SELECT * FROM apps_folder_app WHERE folder_id = :folderId ORDER BY sort_order ASC")
    fun observeMembership(folderId: Long): Flow<List<FolderApp>>

    @Query("SELECT * FROM apps_folder_app WHERE folder_id = :folderId ORDER BY sort_order ASC")
    suspend fun getMembership(folderId: Long): List<FolderApp>

    /** Every folder ↔ app row across every folder. Used by the editor's
     *  "this app is also in folder X" hint. */
    @Query("SELECT * FROM apps_folder_app")
    fun observeAllMembership(): Flow<List<FolderApp>>

    // ── App records (materialized cache) ────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRecord(record: AppRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRecords(records: List<AppRecord>)

    @Query("DELETE FROM apps_record WHERE app_key = :appKey")
    suspend fun deleteAppRecord(appKey: String)

    @Query("DELETE FROM apps_record WHERE package_name = :packageName")
    suspend fun deleteAppRecordsForPackage(packageName: String)

    @Query("SELECT * FROM apps_record ORDER BY label COLLATE NOCASE ASC")
    fun observeAppRecords(): Flow<List<AppRecord>>

    @Query("SELECT * FROM apps_record")
    suspend fun getAppRecords(): List<AppRecord>

    @Query("SELECT * FROM apps_record WHERE app_key = :appKey")
    suspend fun getAppRecord(appKey: String): AppRecord?

    // ── Web-link apps ───────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWebLink(link: WebLinkApp): Long

    @Update
    suspend fun updateWebLink(link: WebLinkApp)

    @Query("DELETE FROM apps_weblink WHERE id = :id")
    suspend fun deleteWebLink(id: Long)

    @Query("SELECT * FROM apps_weblink ORDER BY label COLLATE NOCASE ASC")
    fun observeWebLinks(): Flow<List<WebLinkApp>>

    @Query("SELECT * FROM apps_weblink WHERE id = :id")
    suspend fun getWebLink(id: Long): WebLinkApp?

    // ── Folder rule (1:1) ───────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: FolderRuleEntity)

    @Query("DELETE FROM apps_folder_rule WHERE folder_id = :folderId")
    suspend fun deleteRule(folderId: Long)

    @Query("SELECT * FROM apps_folder_rule WHERE folder_id = :folderId")
    suspend fun getRule(folderId: Long): FolderRuleEntity?

    @Query("SELECT * FROM apps_folder_rule")
    fun observeRules(): Flow<List<FolderRuleEntity>>

    // ── Folder widget config (per-appWidgetId) ──────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetConfig(config: FolderWidgetConfig)

    @Query("DELETE FROM apps_widget_config WHERE app_widget_id = :appWidgetId")
    suspend fun deleteWidgetConfig(appWidgetId: Int)

    @Query("SELECT * FROM apps_widget_config WHERE app_widget_id = :appWidgetId")
    suspend fun getWidgetConfig(appWidgetId: Int): FolderWidgetConfig?

    @Query("SELECT * FROM apps_widget_config")
    suspend fun getAllWidgetConfigs(): List<FolderWidgetConfig>

    @Query("SELECT * FROM apps_widget_config WHERE folder_id = :folderId")
    suspend fun getWidgetConfigsForFolder(folderId: Long): List<FolderWidgetConfig>
}
