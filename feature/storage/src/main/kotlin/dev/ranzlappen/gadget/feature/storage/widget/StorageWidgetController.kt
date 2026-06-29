package dev.ranzlappen.gadget.feature.storage.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.feature.storage.StorageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repaints every placed storage widget whenever the internal used-% changes —
 * the content-source → repaint side of the kit's content archetype, driven
 * through [ContentWidgetUpdater]. Rides the monitor's volume poll as a ticker
 * and distinct-until-changes on the integer percent so an unchanged poll
 * doesn't repaint.
 *
 * Eagerly instantiated for the process lifetime (the app's startup path injects
 * it, mirroring `FolderWidgetController` / `BatteryWidgetController`).
 */
@Singleton
class StorageWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitor: StorageMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            monitor.volumes
                .map { monitor.internalUsedPercent().toInt() }
                .distinctUntilChanged()
                // Skip the initial replay — the launcher already paints placed
                // widgets via onUpdate; only repaint on subsequent changes.
                .drop(1)
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, StorageWidgetProvider.PROVIDER_CLASS)
                }
        }
    }
}
