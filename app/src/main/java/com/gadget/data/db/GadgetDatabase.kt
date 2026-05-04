package com.gadget.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.data.db.apps.FolderApp
import com.gadget.data.db.apps.FolderRuleEntity
import com.gadget.data.db.apps.FolderWidgetConfig
import com.gadget.data.db.apps.WebLinkApp

@Database(
    entities = [
        MetricReading::class,
        MetricSession::class,
        Folder::class,
        FolderApp::class,
        AppRecord::class,
        WebLinkApp::class,
        FolderRuleEntity::class,
        FolderWidgetConfig::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class GadgetDatabase : RoomDatabase() {
    abstract fun metricDao(): MetricDao
    abstract fun appsDao(): AppsDao
}

/**
 * v1 → v2 adds the App-Organizer module's tables. No existing column or row is
 * touched, so this is purely additive and safe to run on populated v1 databases.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_folder` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `base_color_argb` INTEGER NOT NULL,
                `cover_icon` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `locked` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_folder_app` (
                `folder_id` INTEGER NOT NULL,
                `app_key` TEXT NOT NULL,
                `sort_order` INTEGER NOT NULL,
                PRIMARY KEY(`folder_id`, `app_key`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_apps_folder_app_folder_id` " +
                "ON `apps_folder_app` (`folder_id`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_record` (
                `app_key` TEXT NOT NULL,
                `package_name` TEXT NOT NULL,
                `activity_class` TEXT,
                `label` TEXT NOT NULL,
                `user_serial` INTEGER NOT NULL,
                `is_web_apk` INTEGER NOT NULL,
                `is_web_link` INTEGER NOT NULL,
                `first_install_time` INTEGER NOT NULL,
                `last_seen` INTEGER NOT NULL,
                PRIMARY KEY(`app_key`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_apps_record_package_name` " +
                "ON `apps_record` (`package_name`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_weblink` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `url` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `favicon_path` TEXT,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent(),
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_folder_rule` (
                `folder_id` INTEGER NOT NULL,
                `rule_json` TEXT NOT NULL,
                PRIMARY KEY(`folder_id`)
            )
            """.trimIndent(),
        )
    }
}

/**
 * v2 → v3 adds the per-`appWidgetId` folder-widget config table. Purely
 * additive; populated v2 databases pick up the new table without touching
 * any existing data.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `apps_widget_config` (
                `app_widget_id` INTEGER NOT NULL,
                `folder_id` INTEGER NOT NULL,
                `size_variant` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`app_widget_id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_apps_widget_config_folder_id` " +
                "ON `apps_widget_config` (`folder_id`)",
        )
    }
}
