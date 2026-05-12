# Automation Engine

> **Status: placeholder.** Detailed design lands in Phase 2. This file
> exists in Batch 0 so the `core/automation/` and
> `feature/automation-ui/` module skeletons have a real target to point
> at.

## Goal

Let the user wire `if <sensor condition> then <actuator command>` rules
without writing Kotlin. The engine subscribes to the hardware registry
(see [`sensor-actuator-api.md`](sensor-actuator-api.md)) and dispatches
actuator commands when conditions evaluate true.

## Modules

| Module                   | Purpose                                          |
|--------------------------|--------------------------------------------------|
| `core:automation`        | Engine internals: rule graph, evaluator, scheduler. Pure Kotlin where possible. |
| `feature:automation-ui`  | Compose UI for authoring + viewing rules. Depends on `core:automation`, `core:designsystem`, `core:hardware`. |

## Why a dedicated module pair?

Rules can chain across feature boundaries — e.g. "if proximity sensor
< 5 cm then turn flashlight off". Putting the engine in `core/` and
the UI in `feature/automation-ui/` lets the engine be consumed by:

- The bug-report dashboard ("show the last 10 fired rules").
- Future surfaces — widget, Quick Settings Tile, Wear-OS
  complication — that want to display rule state without dragging in
  the authoring UI.
- CI tests that drive rules from a fake hardware registry.

If the engine lived in `feature/automation-ui/`, every consumer would
have to depend on Compose to get at the rule state. Splitting at the
core/feature seam keeps Compose where it belongs.

## Rule shape (sketch — final form TBD)

```
data class Rule(
    val id: RuleId,
    val name: String,
    val conditions: List<Condition>,        // ALL must hold
    val actions: List<ActuatorCommand>,
    val enabled: Boolean,
)

sealed interface Condition {
    data class SensorThreshold(
        val sensor: SensorId,
        val op: ComparisonOp,
        val value: Double,
    ) : Condition

    data class TimeWindow(val start: LocalTime, val end: LocalTime) : Condition

    // Open for extension — rooted-only conditions plug in here under
    // RootCapabilityRegistry gating.
}
```

## Persistence

Rules go in Room (`core:data`). Migration policy mirrors the rest of
the app — schema version bumps require a migration test that loads the
old schema, applies the migration, and asserts on the new shape.

## When this expands

Phase 2 (planned ordering — subject to revision).
