package com.gadget.widget.folder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import com.gadget.apps.AppsEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 1x1 cell variant of the folder widget. Shares the storage schema and the
 * `FolderWidgetRenderer` with [FolderWidgetProvider]; the renderer picks the
 * 1x1 layout (no name strip, no tile grid — just a tinted folder symbol or
 * the cover icon if set) when the per-`appWidgetId` config row's `sizeVariant`
 * is "1x1".
 *
 * Distinct receiver class so the launcher's widget tray exposes both 1x1 and
 * 2x2 entries and `getAppWidgetIds(componentName)` cleanly enumerates each.
 */
class FolderWidget1x1Provider : AppWidgetProvider() {

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
            scope.launch { FolderWidgetRenderer.update(context, appWidgetManager, id, dao) }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appsDao()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        for (id in appWidgetIds) {
            scope.launch { dao.deleteWidgetConfig(id) }
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, FolderWidget1x1Provider::class.java),
            )
            if (ids.isEmpty()) return
            val dao = EntryPointAccessors
                .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
                .appsDao()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            for (id in ids) {
                scope.launch { FolderWidgetRenderer.update(context, manager, id, dao) }
            }
        }
    }
}
