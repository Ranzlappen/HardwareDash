package dev.ranzlappen.gadget.feature.ambient.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.feature.ambient.AmbientSensor
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
 * Repaints every placed ambient widget whenever the brightness gauge or level
 * bucket changes — the content-source → repaint side of the kit's content
 * archetype, driven through [ContentWidgetUpdater]. Distinct on the gauge
 * percent so a sub-bucket lux jitter doesn't repaint on every sensor sample.
 *
 * Eagerly instantiated for the process lifetime (the app's startup path injects
 * it, mirroring the other widget controllers).
 */
@Singleton
class AmbientWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensor: AmbientSensor,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            sensor.state
                .map { state ->
                    val lux = state.luxLevel
                    if (state.sensorAvailable && lux != null) {
                        AmbientBrightness.brightnessPercent(lux)
                    } else {
                        -1
                    }
                }
                .distinctUntilChanged()
                // Skip the initial replay — the launcher already paints placed
                // widgets via onUpdate; only repaint on subsequent changes.
                .drop(1)
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, AmbientWidgetProvider.PROVIDER_CLASS)
                }
        }
    }
}
