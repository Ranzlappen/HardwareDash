package dev.ranzlappen.gadget.core.model

import kotlinx.coroutines.flow.Flow

/**
 * A module's readable signal — the single seam for **both** monitoring
 * (chart/history) and, later, automation triggers/conditions.
 *
 * A feature contributes one [MetricSource] per readable signal into a Hilt
 * `Map<String, MetricSource>` (`@IntoMap @StringKey(metricKey)`). The
 * `:core:monitoring` sampler reads it on the metric's cadence and persists
 * the value; the future automation engine will read the same source to
 * evaluate trigger conditions. The contract stays free of Android and of
 * any feature module so it can live in `:core:model`; the only foundation
 * dependency is pure-Kotlin `kotlinx.coroutines` for the optional [stream].
 *
 * This is the deliberate fix for the legacy `Link` module, which hardcoded
 * a 70-entry metric registry inside `LinkService` instead of letting each
 * feature own its signals.
 *
 * **Push vs. poll.** A source is sampled in one of two ways, chosen per
 * signal to keep the shared [dev.ranzlappen.gadget.core.monitoring] service
 * efficient as modules multiply:
 *  - **Poll** (default): the sampler calls [sample] every `pollIntervalMs`.
 *    Use for genuinely sampled signals (battery %, temperature, RSSI).
 *  - **Push**: override [stream] to emit only when the value *changes*. Use
 *    for event-driven signals (an actuator's on/off/brightness). The
 *    sampler collects the flow instead of polling, so an idle actuator
 *    causes **zero** wakeups. Prefer push wherever the underlying state is
 *    already observable.
 */
interface MetricSource {

    /** Static metadata: key, label, unit, bounds, category. */
    val descriptor: MetricDescriptor

    /**
     * Read the current value of this metric. Called on a polling cadence;
     * implementations should return quickly (a cached/last-known reading)
     * rather than blocking on a long hardware acquisition. Always required —
     * it backs the poll path and serves as a one-shot read for callers (e.g.
     * the automation engine) even when [stream] is also provided.
     */
    suspend fun sample(): Float

    /**
     * Optional **push** source. Return a hot/cold [Flow] that emits the
     * current value and then a fresh value on every change (de-duplicated by
     * the implementation). When non-null the monitoring sampler collects this
     * instead of polling [sample], recording each emission as a step so an
     * idle, event-driven signal incurs no periodic wakeups. Return `null`
     * (the default) to use the poll path.
     */
    fun stream(): Flow<Float>? = null
}

/**
 * Describes a [MetricSource] for the monitoring UI and (later) the
 * automation rule builder — so neither has to hardcode per-metric copy.
 */
data class MetricDescriptor(
    val metricKey: String,
    val displayName: String,
    val unit: String = "",
    val min: Float = 0f,
    val max: Float = 100f,
    val category: MetricCategory = MetricCategory.Other,
)

enum class MetricCategory {
    Battery,
    Network,
    Sensor,
    Actuator,
    Location,
    Device,
    Other,
}
