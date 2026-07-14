package dev.ranzlappen.gadget.core.automation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The event/edge that wakes a [Rule] — as opposed to a [Condition], which
 * is a state gate re-checked at that moment. Evaluation is event-driven off
 * triggers; conditions never wake a rule on their own. See
 * `docs/automation-engine.md` § Trigger taxonomy.
 *
 * **Wire format is sacred.** Every subtype carries an explicit
 * package-pinned [SerialName] so the persisted JSON discriminator survives
 * any future package move (the kotlinx polymorphic-discriminator pitfall in
 * CLAUDE.md). [RuleSerializationTest] regression-pins each string — if it
 * turns red, restore the pin or ship a migrator; never edit the test.
 */
@Serializable
sealed interface Trigger {

    /**
     * Fires when a `MetricSource` value crosses [value] in the [edge]
     * direction.
     *
     * [clearValue] is the hysteresis re-arm bound: after firing, the
     * trigger re-arms only once the metric crosses back past [clearValue]
     * on the opposite side of [op]. `null` re-arms as soon as the predicate
     * goes false — fine for clean digital signals, risky for noisy analog
     * sensors (prefer a clearValue there). Example: proximity `Lt 5` with
     * `clearValue = 8` fires below 5 cm and re-arms only above 8 cm, so
     * noise dithering around 5 cm cannot machine-gun the rule.
     */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.MetricThreshold")
    data class MetricThreshold(
        val metricKey: String,
        val op: ComparisonOp,
        val value: Float,
        val edge: Edge = Edge.Rising,
        val clearValue: Float? = null,
    ) : Trigger

    /**
     * Wall-clock schedule, backed by `AlarmManager` (not WorkManager —
     * ADR-0002 Decision 4). [exact] opts into
     * `setExactAndAllowWhileIdle` behind the `SCHEDULE_EXACT_ALARM`
     * permission, degrading to a ±10 min window when denied — see the
     * degradation contract table in `docs/automation-engine.md`.
     */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.Schedule")
    data class Schedule(
        /** Minutes after local midnight, `0..1439`. */
        val timeOfDayMinutes: Int,
        val daysOfWeek: Set<DayOfWeek> = DayOfWeek.everyDay(),
        val exact: Boolean = false,
    ) : Trigger

    /** System broadcast: boot, charging state, connectivity changes. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.SystemEvent")
    data class SystemEvent(val event: SystemEventKind) : Trigger

    /** User-initiated: a widget / QS tile / in-app "run now". No scheduling. */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.Manual")
    data object Manual : Trigger

    /**
     * Fires when the device crosses a circular location fence. Backed by the
     * platform `GeofencingClient` (Play Services), which hosts the fence at the
     * OS level and delivers transitions through a `PendingIntent` — so a
     * Geofence rule is one-shot / edge-driven like [Schedule] and never needs
     * the resident automation service.
     *
     * [latitude] / [longitude] are WGS-84 degrees; [radiusMeters] is the fence
     * radius (the platform recommends ≥ ~100 m for reliable triggering).
     * [transition] selects which crossing fires.
     */
    @Serializable
    @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.Geofence")
    data class Geofence(
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
        val transition: GeofenceTransition = GeofenceTransition.Enter,
    ) : Trigger
}

/** Which crossing of a [Trigger.Geofence] fence fires: entering or leaving. */
@Serializable
enum class GeofenceTransition { Enter, Exit }

/** Broadcast kinds a [Trigger.SystemEvent] can subscribe to. */
@Serializable
enum class SystemEventKind { BootCompleted, PowerConnected, PowerDisconnected, Connectivity }

/**
 * Which crossing of a [Trigger.MetricThreshold] predicate fires: entering
 * it ([Rising]) or leaving it ([Falling]) — never every matching sample.
 */
@Serializable
enum class Edge { Rising, Falling }

/** Comparison operator for thresholds and [Condition.MetricCompare]. */
@Serializable
enum class ComparisonOp { Lt, Lte, Gt, Gte, Eq, Neq }

/**
 * Day-of-week for [Trigger.Schedule]. A project-local enum (not
 * `java.time.DayOfWeek`) so kotlinx-serialization handles it natively and
 * the wire names stay under this module's control.
 */
@Serializable
enum class DayOfWeek {
    Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday;

    companion object {
        /** All seven days — the [Trigger.Schedule.daysOfWeek] default. */
        // values() (not the 1.9.x-experimental Enum.entries) — this module
        // pins Kotlin 1.9.10 and is CI-verified only (Mode C).
        fun everyDay(): Set<DayOfWeek> = values().toSet()
    }
}
