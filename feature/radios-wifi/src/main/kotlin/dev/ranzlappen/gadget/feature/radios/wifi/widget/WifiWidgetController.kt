package dev.ranzlappen.gadget.feature.radios.wifi.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.feature.radios.wifi.WifiMonitor
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
 * Repaints every placed WiFi widget whenever the enabled / connected state or
 * the signal bucket changes — the content-source → repaint side of the kit's
 * content archetype, driven through [ContentWidgetUpdater]. Distinct on
 * (enabled, connected, rssi) so unrelated state churn doesn't repaint.
 *
 * Eagerly instantiated for the process lifetime (the app's startup path injects
 * it, mirroring `BatteryWidgetController` / `StorageWidgetController`).
 */
@Singleton
class WifiWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitor: WifiMonitor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            monitor.state
                .map { Triple(it.enabled, it.connected, it.rssiDbm) }
                .distinctUntilChanged()
                // Skip the initial replay — the launcher already paints placed
                // widgets via onUpdate; only repaint on subsequent changes.
                .drop(1)
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, WifiWidgetProvider.PROVIDER_CLASS)
                }
        }
    }
}
