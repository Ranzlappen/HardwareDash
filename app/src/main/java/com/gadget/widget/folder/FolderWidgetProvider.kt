package com.gadget.widget.folder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.gadget.apps.AppsEntryPoint
import com.gadget.apps.pin.PinFolderHelper
import com.gadget.data.db.apps.FolderWidgetConfig
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Classic-RemoteViews `AppWidgetProvider` for the App-Organizer folder widget.
 * Mirrors the shape of the existing 11 providers in this app. Hilt-aware via
 * `EntryPointAccessors` since `AppWidgetProvider` instances aren't injectable.
 *
 * The Glance-flavored variant (added in batch 9) is registered as a separate
 * receiver and reads the same `apps_widget_config` rows, so both variants can
 * coexist without sharing layout code.
 */
class FolderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appsDao()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        for (id in appWidgetIds) {
            scope.launch {
                FolderWidgetRenderer.update(context, appWidgetManager, id, dao)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != PinFolderHelper.ACTION_PIN_CALLBACK) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val folderId = intent.getLongExtra(PinFolderHelper.EXTRA_FOLDER_ID, -1L)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || folderId < 0L) return

        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appsDao()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            dao.upsertWidgetConfig(
                FolderWidgetConfig(
                    appWidgetId = appWidgetId,
                    folderId = folderId,
                    sizeVariant = "2x2",
                    createdAt = System.currentTimeMillis(),
                ),
            )
            FolderWidgetRenderer.update(
                context,
                AppWidgetManager.getInstance(context),
                appWidgetId,
                dao,
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Drop the per-appWidgetId config row so we don't accumulate orphans.
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appsDao()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        for (id in appWidgetIds) {
            scope.launch { dao.deleteWidgetConfig(id) }
        }
    }

    companion object {
        /** Refresh every placed folder widget. Called by [FolderWidgetController]. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, FolderWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val dao = EntryPointAccessors
                .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
                .appsDao()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            for (id in ids) {
                scope.launch {
                    FolderWidgetRenderer.update(context, manager, id, dao)
                }
            }
        }
    }
}
