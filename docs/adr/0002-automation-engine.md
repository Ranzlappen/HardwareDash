# ADR-0002: Cross-automation engine — model, runtime, and persistence

- **Status:** Accepted (design); implementation tracked in epic [#145](https://github.com/Ranzlappen/HardwareDash/issues/145)
- **Date:** 2026-06-10
- **Deciders:** project owner, Claude (implementer)
- **Supersedes:** the placeholder design in `docs/automation-engine.md`

## Context

The stated product vision — "a versatile and powerful phone-controlling
app" — *is* the cross-automation engine: `when <trigger> [if <conditions>]
then <actions>`. Today `:core:automation` ships only the **action-side
contract** (`ActionHandler`, `ModuleAction`, `ModuleActionRegistry`),
already consumed by widgets and implemented by `TorchActionHandler` /
`VibrationActionHandler`. There is **no engine that consumes it**: no rule
model, trigger taxonomy, evaluator, scheduler, runtime, or UI. The
read-side seam (`MetricSource` in `:core:model`) exists and was explicitly
designed to feed automation, but nothing drives it for automation yet.

The full design is in `docs/automation-engine.md`. This ADR records the
load-bearing decisions and the alternatives rejected.

## Decisions

### 1. Trigger ≠ Condition

A **trigger** is the event/edge that wakes a rule; **conditions** are state
gates re-checked at that moment. Evaluation is event-driven off triggers,
not a continuous re-scan of conditions. *Rejected:* a single flat "list of
conditions, poll them all" model — it forces polling and conflates "what
woke the rule" with "is the rule allowed to act".

### 2. Reuse both existing registries verbatim

Actions dispatch through `ModuleActionRegistry.dispatch(featureId,
actionKey, params)`; trigger/condition values read through the
`MetricSource` Hilt map. The engine imports **no feature module**.
*Rejected:* a parallel automation-specific action/sensor abstraction — it
would duplicate the two seams the codebase already standardised on and
re-introduce the central hardcoding the `Link` module was faulted for.

### 3. Flat conditions + ALL/ANY, not a nested boolean tree (v1)

A flat `List<Condition>` folded by a `ConditionLogic.{All,Any}` enum covers
the overwhelming majority of real rules and keeps the builder legible. The
sealed `Condition` shape leaves room for a `Group` subtype later.
*Rejected for v1:* a full nested AND/OR tree — more model + UI complexity
than early rules justify.

### 4. Dedicated `AutomationService` FGS, self-stopping when no rule is enabled

The engine runs in its own foreground service that self-stops when zero
rules are enabled, mirroring `MonitorService`'s self-stop pattern.

- Metric triggers subscribe to `MetricSource.stream()` (push; zero idle
  wakeups) with a bounded `sample()` poll fallback.
- Schedule triggers use **`AlarmManager`** (windowed-inexact by default;
  per-rule `exact` opt-in behind `SCHEDULE_EXACT_ALARM` with a documented
  denied-fallback — see the degradation contract in the design doc),
  **not** `WorkManager` — WorkManager's 15-minute floor is too coarse for
  "at 09:00 do X".
- System-event triggers use registered `BroadcastReceiver`s; `BootCompleted`
  re-arm reuses `:core:widgetkit`'s `BootRearmHandler` multibinding rather
  than adding a second boot receiver.

*Rejected:* (a) folding automation into `MonitorService` — the "one shared
FGS" rule targets *per-module* services; automation is a single
cross-cutting engine, and an independent enable/disable lifecycle keeps the
subsystems decoupled. (b) `WorkManager` as the primary scheduler — wrong
granularity and battery model for fine-grained rules. A future
consolidation of the monitoring + automation FGS into one "Gadget
background" service (to avoid two persistent notifications) is left as a
follow-up, not v1.

### 5. Persist rules in `automation.db` with JSON columns for the sealed graphs

A new Room DB in `:core:data` (sibling to `apps.db` / `monitoring.db`),
one `rules` table, with `Trigger` / `Condition[]` / `RuleAction[]` stored
as kotlinx-serialization **JSON columns**. Every persisted sealed subtype
carries an explicit package-pinned `@SerialName`; round-trip tests guard
the wire format. *Rejected:* normalising triggers/conditions/actions into
relational tables — far more schema + migration churn for sealed
hierarchies that evolve, with no query benefit (rules are read whole).

### 6. Three-layer root gating

(1) the builder filters `requiresRoot` actions out of the picker on
standard; (2) `RuleEvaluator` drops root actions when `rootAvailable` is
false (protects restored-from-rooted-backup rules); (3) dispatch reaches
hardware only through the feature's existing `:core:root` `RootSafetyGate`
+ `RootFeatureKey` path. The engine never branches on
`BuildConfig.IS_ROOTED`. *Rejected:* relying on a single layer — defence in
depth is cheap and the standard APK must be *structurally* unable to fire a
root action.

### 7. Pure-Kotlin evaluator

`RuleEvaluator.evaluate(rule, firedTrigger, readings, now, rootAvailable):
List<RuleAction>` is a pure function — no Android, no I/O, no coroutines —
so correctness is locked down with exhaustive JVM unit tests (threshold
edges, ALL/ANY, time-window midnight wrap, root filtering) with no
emulator. *Rejected:* an evaluator coupled to the service/coroutine
runtime — it would push the cheapest-to-test logic behind the
hardest-to-test boundary.

### 8. Storms are bounded, not detected

Chaining (an action moves a metric that is another rule's trigger) is a
feature. Runaway firing is prevented structurally: (a) per-rule
`cooldownSeconds`, persisted via `last_fired_at` so it survives restarts;
(b) `MetricThreshold.clearValue` hysteresis so noise around a threshold
can't re-fire on every sample; (c) a runtime `AutomationBudget`
(per-cycle + rolling dispatch caps) with a user-visible throttle
notification. *Rejected:* static cross-rule cycle detection — undecidable
against real-world signals, and unnecessary once firing is budgeted.

## Consequences

### Positive

- The engine drives every module through seams that already exist and are
  already tested by the widget + monitoring subsystems.
- Event-driven + push-preferring + self-stopping ⇒ an idle device with
  rules costs ~zero wakeups.
- The hardest logic (evaluation) is the cheapest to test (pure Kotlin).
- Root safety is structural, not conventional.

### Negative

- A second foreground-service notification when both monitoring and
  automation are active. Mitigation: self-stop when idle; possible future
  service consolidation (noted above).
- `:core:automation` gains Android dependencies (service, AlarmManager,
  Room-backed repository) beyond the pure-contract module it is today. The
  evaluator stays pure-Kotlin to preserve testability.
- `AlarmManager` exactness depends on Doze/standby allowances and the
  `SCHEDULE_EXACT_ALARM` permission. The degradation contract (inexact
  default, `exact` opt-in, denied-fallback to windowed) is now specified at
  design time; the only deferred piece is the settings-redirect UX polish in
  3.3/3.4.

## Alternatives considered (summary)

See the per-decision *Rejected* notes above. The throughline: do **not**
invent new abstractions where `ActionHandler` (write) and `MetricSource`
(read) already provide them, and do **not** poll where push + alarms +
broadcasts give an event-driven model within Android's background limits.

## Implementation plan

Sequenced in `docs/automation-engine.md` (§ Build sequencing) and epic
#145: engine core (rule model + evaluator + persistence) → runtime
(service + scheduler + boot re-arm) → `:feature:automation-ui`. `:core:hardware`
(epic #146) lands alongside as the builder's signal-enumeration layer.
