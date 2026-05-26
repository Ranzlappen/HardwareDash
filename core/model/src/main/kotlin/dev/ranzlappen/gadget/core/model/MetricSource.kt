package dev.ranzlappen.gadget.core.model

/**
 * A module's readable signal — the single seam for **both** monitoring
 * (chart/history) and, later, automation triggers/conditions.
 *
 * A feature contributes one [MetricSource] per readable signal into a Hilt
 * `Map<String, MetricSource>` (`@IntoMap @StringKey(metricKey)`). The
 * `:core:monitoring` sampler polls [sample] on an interval and persists the
 * value; the future automation engine will poll the same source to evaluate
 * trigger conditions. Keeping it dependency-free (pure Kotlin, a `suspend`
 * read rather than a `Flow`) lets the contract live in `:core:model` so
 * neither monitoring nor automation pulls in a feature module.
 *
 * This is the deliberate fix for the legacy `Link` module, which hardcoded
 * a 70-entry metric registry inside `LinkService` instead of letting each
 * feature own its signals.
 */
interface MetricSource {

    /** Static metadata: key, label, unit, bounds, category. */
    val descriptor: MetricDescriptor

    /**
     * Read the current value of this metric. Called on a polling cadence;
     * implementations should return quickly (a cached/last-known reading)
     * rather than blocking on a long hardware acquisition.
     */
    suspend fun sample(): Float
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
