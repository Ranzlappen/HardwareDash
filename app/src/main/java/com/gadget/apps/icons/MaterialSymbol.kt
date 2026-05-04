package com.gadget.apps.icons

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.gadget.R

/**
 * Curated set of Material symbols the user can pick as a folder cover icon.
 * Each entry pairs:
 *  - an [ImageVector] from `material-icons-extended` for in-app rendering, and
 *  - a vector [drawableRes] under `res/drawable/` for the widget's RemoteViews
 *    `setImageViewResource` path (vendored alongside the lib because Compose
 *    ImageVectors aren't directly addressable by RemoteViews).
 *
 * Stored in `Folder.coverIcon` as `symbol:<id>`; the prefix is parsed by
 * [parseCoverIcon] in `FolderWidgetRenderer` (Batch G).
 */
enum class MaterialSymbol(
    val id: String,
    val icon: ImageVector,
    @DrawableRes val drawableRes: Int,
) {
    Folder("folder", Icons.Filled.Folder, R.drawable.ic_symbol_folder),
    Work("work", Icons.Filled.Work, R.drawable.ic_symbol_work),
    Games("games", Icons.Filled.SportsEsports, R.drawable.ic_symbol_games),
    MusicNote("music_note", Icons.Filled.MusicNote, R.drawable.ic_symbol_music_note),
    MenuBook("menu_book", Icons.Filled.MenuBook, R.drawable.ic_symbol_menu_book),
    Star("star", Icons.Filled.Star, R.drawable.ic_symbol_star);

    companion object {
        fun fromId(id: String): MaterialSymbol? = entries.firstOrNull { it.id == id }
    }
}
