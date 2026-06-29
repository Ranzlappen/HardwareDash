package dev.ranzlappen.gadget.feature.battery.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.feature.battery.BatteryMonitor
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
 * Repaints every placed battery widget whenever the level or charging state
 * changes — the content-source → repaint side of the kit's content archetype,
 * driven through [ContentWidgetUpdater]. Distinct-until-changed on just
 * (level, charging) so an unchanged temperature/voltage tick doesn't repaint.
 *
 * Eagerly instantiated for the process lifetime (the app's startup path injects
 * it, mirroring `FolderWidgetController`).
 */
@Singleton
class BatteryWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitor: BatteryMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            monitor.state
                .map { it.level to it.isCharging }
                .distinctUntilChanged()
                // Skip the initial replay — the launcher already paints placed
                // widgets via onUpdate; only repaint on subsequent changes.
                .drop(1)
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, BatteryWidgetProvider.PROVIDER_CLASS)
                }
        }
    }
}
