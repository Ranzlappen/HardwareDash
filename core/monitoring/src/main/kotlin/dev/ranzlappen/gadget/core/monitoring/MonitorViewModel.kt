package dev.ranzlappen.gadget.core.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.data.MonitorSample
import dev.ranzlappen.gadget.core.data.MonitorSampleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [MonitorContainer]. One instance per metricKey (the container
 * scopes it via `hiltViewModel(key = metricKey)`), but every method is
 * metricKey-parameterised so the same VM type serves any module.
 */
@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val configRepo: MonitorConfigRepository,
    private val sampleRepo: MonitorSampleRepository,
    private val controller: MonitorController,
) : ViewModel() {

    fun config(metricKey: String): Flow<MonitorConfig> = configRepo.config(metricKey)

    /**
     * Windowed sample history. The window lower-bound slides on a ticker
     * (re-querying every poll interval) so the chart scrolls even when no
     * new sample has landed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun history(metricKey: String): Flow<List<MonitorSample>> =
        configRepo.config(metricKey).flatMapLatest { cfg ->
            flow {
                while (true) {
                    emit(System.currentTimeMillis() - cfg.windowSeconds * 1_000L)
                    delay(cfg.pollIntervalMs.coerceAtLeast(MIN_TICK_MS))
                }
            }.flatMapLatest { sinceMs -> sampleRepo.observeSince(metricKey, sinceMs) }
        }

    fun update(metricKey: String, config: MonitorConfig) {
        viewModelScope.launch {
            configRepo.save(metricKey, config)
            if (config.enabled) controller.ensureStarted()
        }
    }

    private companion object {
        const val MIN_TICK_MS = 250L
    }
}
