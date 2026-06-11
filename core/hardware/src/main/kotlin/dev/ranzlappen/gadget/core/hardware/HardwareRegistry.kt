package dev.ranzlappen.gadget.core.hardware

import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The read-side enumeration registry (epic #146;
 * `docs/automation-engine.md` § Goal) — the symmetric partner to
 * `ModuleActionRegistry`: that one enumerates + dispatches a feature's
 * **actions**, this one enumerates + reads a feature's **signals**.
 *
 * Aggregates the Hilt `Map<String, MetricSource>` multibinding — the *same*
 * map `:core:monitoring`'s sampler and the automation engine's
 * `AutomationService` / `RuleFireExecutor` consume, so there is exactly
 * **one** signal definition per readable value (the deliberate fix for the
 * legacy `Link` module's hardcoded 70-entry metric registry). A feature
 * contributes a signal once (`@Binds @IntoMap @StringKey("<metricKey>")`)
 * and it is simultaneously chartable, automatable, and enumerable here.
 *
 * Consumers: the automation rule-builder's trigger/condition pickers
 * (`:feature:automation-ui`) and any future hardware browser. Neither
 * imports a feature module — this registry is how they see the device.
 */
@Singleton
class HardwareRegistry @Inject constructor(
    private val sources: Map<String, @JvmSuppressWildcards MetricSource>,
) {
    /**
     * Every registered signal's descriptor, sorted by display name for
     * stable picker ordering. Registration implies availability on this
     * build; per-device hardware presence (a phone without a light sensor)
     * is reflected by the source itself — see [read]'s null contract.
     */
    fun signals(): List<MetricDescriptor> =
        sources.values.map { it.descriptor }.sortedBy { it.displayName }

    /** The descriptor for [metricKey], or null if no feature registered it. */
    fun descriptor(metricKey: String): MetricDescriptor? =
        sources[metricKey]?.descriptor

    /** True iff some feature registered a source under [metricKey]. */
    fun isRegistered(metricKey: String): Boolean = metricKey in sources

    /**
     * One-shot read of [metricKey]'s current value, or null when the key is
     * unregistered. (A registered-but-absent hardware signal is the
     * source's concern — sensor-backed sources return their documented
     * absent-value; the evaluator's missing-reading-fails-safe rule covers
     * the rest.)
     */
    suspend fun read(metricKey: String): Float? = sources[metricKey]?.sample()
}
