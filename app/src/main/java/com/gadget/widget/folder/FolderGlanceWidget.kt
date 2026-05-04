package com.gadget.widget.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gadget.apps.AppsEntryPoint
import com.gadget.data.db.apps.Folder
import com.gadget.ui.folder.FolderPopupActivity
import dagger.hilt.android.EntryPointAccessors

/**
 * Glance-based "designable" variant of the folder widget. Coexists with the
 * RemoteViews provider added in batch 8: same `apps_widget_config` schema,
 * same FolderWidgetConfigActivity, same FolderPopupActivity launch on tap.
 *
 * The visual differentiator is a folder-color-tinted Card surface (vs. the
 * dark rounded rectangle used by the RemoteViews flavor), giving users a
 * "designable" alternative when adding the widget.
 */
class FolderGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appsDao()
        val config = dao.getWidgetConfig(appWidgetId)
        val folder = config?.let { dao.getFolder(it.folderId) }

        provideContent {
            if (folder == null) {
                NeutralContent()
            } else {
                FolderContent(folder = folder)
            }
        }
    }

    companion object {
        suspend fun updateAll(context: android.content.Context) {
            val widget = FolderGlanceWidget()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(FolderGlanceWidget::class.java)
            for (id in ids) widget.update(context, id)
        }
    }
}

@Composable
private fun FolderContent(folder: Folder) {
    val context = LocalContext.current
    val accent = Color(folder.baseColorArgb)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(accent.copy(alpha = 0.22f))
            .padding(12.dp)
            .clickable(
                actionStartActivity(FolderPopupActivity.intent(context, folder.id)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name,
                style = TextStyle(
                    color = ColorProvider(accent),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))
            // Accent stripe echoes the RemoteViews variant so the two flavors
            // feel related when placed side-by-side on the home screen.
            Box(
                modifier = GlanceModifier
                    .width(28.dp)
                    .height(3.dp)
                    .cornerRadius(2.dp)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}

@Composable
private fun NeutralContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(Color(0x331A1A2E))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Folder",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
