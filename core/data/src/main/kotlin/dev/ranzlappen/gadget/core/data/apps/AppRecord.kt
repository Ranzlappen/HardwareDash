package dev.ranzlappen.gadget.core.data.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Materialized cache of every launchable "app" the system exposes plus user-added
 * web-link apps, addressed by a stable `appKey`:
 *  - installed apps: `installed:<userSerial>:<package>/<activityClass>`
 *  - web-link apps : `weblink:<id>`
 *
 * The cache is rebuilt by `AppScanner` on launch and on PACKAGE_ADDED/REMOVED so
 * widget receivers can render without re-querying `LauncherApps` from a cold path.
 */
@Entity(
    tableName = "apps_record",
    indices = [Index(value = ["package_name"])],
)
data class AppRecord(
    @PrimaryKey
    @ColumnInfo(name = "app_key")
    val appKey: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "activity_class")
    val activityClass: String?,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "user_serial")
    val userSerial: Long,
    @ColumnInfo(name = "is_web_apk")
    val isWebApk: Boolean = false,
    @ColumnInfo(name = "is_web_link")
    val isWebLink: Boolean = false,
    @ColumnInfo(name = "first_install_time")
    val firstInstallTime: Long = 0L,
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,
    /** Snapshot of `ApplicationInfo.flags & FLAG_EXTERNAL_STORAGE` at scan time. */
    @ColumnInfo(name = "is_on_external_storage", defaultValue = "0")
    val isOnExternalStorage: Boolean = false,
    /** Pre-installed / system app — true if the package has FLAG_SYSTEM or
     *  FLAG_UPDATED_SYSTEM_APP, mirroring how Settings → Apps surfaces them. */
    @ColumnInfo(name = "is_system_app", defaultValue = "0")
    val isSystemApp: Boolean = false,
)
