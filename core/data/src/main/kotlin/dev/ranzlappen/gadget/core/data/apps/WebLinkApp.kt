package dev.ranzlappen.gadget.core.data.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-added URL that behaves as a virtual "app" inside folders. Launched via
 * Intent.ACTION_VIEW. The optional `faviconPath` is an absolute path to a PNG
 * cached under `filesDir/apps_favicons/<sha1>.png`.
 */
@Entity(tableName = "apps_weblink")
data class WebLinkApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "favicon_path")
    val faviconPath: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
