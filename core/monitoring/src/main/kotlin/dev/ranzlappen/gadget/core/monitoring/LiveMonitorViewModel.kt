package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.model.MetricSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Backs [LiveMonitorContainer] — the **live**, in-memory companion to the
 * persisted [MonitorViewModel]. It reads a metric's [MetricSource] directly
 * (no Room, no foreground service) into a bounded ring buffer and exposes a
 * fast-updating [LiveTrace], so the chart reacts at the live cadence instead
 * of the history pipeline's poll → DB → downsample latency.
 *
 * One instance per metricKey (`hiltViewModel(key = "$metricKey#live")`).
 * Sampling runs only between [start] and [stop] (the container drives these
 * from a `DisposableEffect`, so it stops when the card leaves the screen),
 * and pauses while [frozen]. Live settings ([intervalMs], [windowMs]) are
 * **ephemeral** — this is a transient analysis surface, not persisted config.
 */
@HiltViewModel
class LiveMonitorViewModel @Inject constructor(
    private val metricSources: Map<String, @JvmSuppressWildcards MetricSource>,
    private val collapseRepo: CollapseStateRepository,
) : ViewModel() {

    private val boundKey = MutableStateFlow<String?>(null)

    // Visibility gate (driven by the container's DisposableEffect) AND the
    // user on/off toggle. Sampling runs only when both are true and not frozen.
    private val visible = MutableStateFlow(false)

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _frozen = MutableStateFlow(false)
    val frozen: StateFlow<Boolean> = _frozen.asStateFlow()

    private val _intervalMs = MutableStateFlow(DEFAULT_LIVE_INTERVAL_MS)
    val intervalMs: StateFlow<Long> = _intervalMs.asStateFlow()

    private val _windowMs = MutableStateFlow(DEFAULT_LIVE_WINDOW_MS)
    val windowMs: StateFlow<Long> = _windowMs.asStateFlow()

    private val buffer = ArrayDeque<TimedSample>()

    private val _trace = MutableStateFlow(LiveTrace.Empty)
    val trace: StateFlow<LiveTrace> = _trace.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sampling: Flow<Float> =
        combine(boundKey, visible, _enabled, _frozen, _intervalMs) { key, vis, on, frozen, interval ->
            Gate(key, vis && on, frozen, interval)
        }.flatMapLatest { gate ->
            val source = gate.key?.let { metricSources[it] }
            if (source == null || !gate.on || gate.frozen) {
                emptyFlow()
            } else {
                source.stream() ?: pollFlow(source, gate.interval)
            }
        }

    init {
        viewModelScope.launch {
            sampling.collect { value -> append(value) }
        }
    }

    /** The metric's unit (e.g. "%") for the stats readout; "" if unbound. */
    fun unit(metricKey: String): String =
        metricSources[metricKey]?.descriptor?.unit.orEmpty()

    /** Bind [metricKey] and mark the card visible. Idempotent. Sampling still
     *  requires the user [enabled] toggle. */
    fun start(metricKey: String) {
        boundKey.value = metricKey
        visible.value = true
    }

    /** Mark the card not visible (called when it leaves the screen). */
    fun stop() {
        visible.value = false
    }

    /** User on/off toggle for the live stream. */
    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }

    /** Freeze holds the current trace for inspection; sampling pauses until
     *  unfrozen (and resumes from live, leaving a visible time gap). */
    fun toggleFreeze() {
        _frozen.value = !_frozen.value
    }

    fun setIntervalMs(ms: Long) {
        _intervalMs.value = ms.coerceIn(MIN_LIVE_INTERVAL_MS, MAX_LIVE_INTERVAL_MS)
    }

    fun setWindowMs(ms: Long) {
        _windowMs.value = ms.coerceIn(MIN_LIVE_WINDOW_MS, MAX_LIVE_WINDOW_MS)
        // Re-trim to the new window and re-emit so the chart resizes live.
        trim(System.currentTimeMillis())
        emitTrace(System.currentTimeMillis())
    }

    fun expanded(id: String): Flow<Boolean> = collapseRepo.expanded(id)

    fun setExpanded(id: String, expanded: Boolean) {
        viewModelScope.launch { collapseRepo.setExpanded(id, expanded) }
    }

    private fun pollFlow(source: MetricSource, intervalMs: Long): Flow<Float> = flow {
        while (true) {
            val value = withTimeoutOrNull(SAMPLE_TIMEOUT_MS) { source.sample() }
            if (value != null) emit(value)
            delay(intervalMs)
        }
    }

    private fun append(value: Float) {
        val now = System.currentTimeMillis()
        buffer.addLast(TimedSample(now, value))
        trim(now)
        emitTrace(now)
    }

    private fun trim(now: Long) {
        val cutoff = now - _windowMs.value
        while (buffer.isNotEmpty() && buffer.first().t < cutoff) buffer.removeFirst()
        while (buffer.size > MAX_LIVE_SAMPLES) buffer.removeFirst()
    }

    private fun emitTrace(now: Long) {
        val samples = buffer.toList()
        val values = samples.map { it.value }
        _trace.value = LiveTrace(
            samples = samples,
            windowMs = _windowMs.value,
            nowMs = now,
            current = values.lastOrNull(),
            min = values.minOrNull(),
            max = values.maxOrNull(),
            avg = if (values.isEmpty()) null else values.average().toFloat(),
        )
    }

    private data class Gate(
        val key: String?,
        val on: Boolean,
        val frozen: Boolean,
        val interval: Long,
    )

    private companion object {
        const val SAMPLE_TIMEOUT_MS = 2_000L
    }
}

/** A single in-memory live sample: wall-clock [t] and [value]. */
@Immutable
data class TimedSample(val t: Long, val value: Float)

/**
 * A snapshot of the live buffer plus derived stats. [nowMs] is the right-edge
 * time the chart anchors to; samples older than `nowMs - windowMs` have been
 * trimmed. [current]/[min]/[max]/[avg] are null while the buffer is empty.
 */
@Immutable
data class LiveTrace(
    val samples: List<TimedSample>,
    val windowMs: Long,
    val nowMs: Long,
    val current: Float?,
    val min: Float?,
    val max: Float?,
    val avg: Float?,
) {
    companion object {
        val Empty = LiveTrace(
            samples = emptyList(),
            windowMs = DEFAULT_LIVE_WINDOW_MS,
            nowMs = 0L,
            current = null,
            min = null,
            max = null,
            avg = null,
        )
    }
}

internal const val DEFAULT_LIVE_INTERVAL_MS = 100L
internal const val MIN_LIVE_INTERVAL_MS = 20L
internal const val MAX_LIVE_INTERVAL_MS = 1_000L
internal const val DEFAULT_LIVE_WINDOW_MS = 10_000L
internal const val MIN_LIVE_WINDOW_MS = 2_000L
internal const val MAX_LIVE_WINDOW_MS = 60_000L
internal const val MAX_LIVE_SAMPLES = 4_000
