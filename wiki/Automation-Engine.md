# Automation Engine

The cross-automation engine is shipped end-to-end (epics #145/#146).
Absorbs `docs/automation-engine.md`; the load-bearing decisions are in
[Decision Records → ADR-0002](Decision-Records).

## Goal

Let the user wire **`when <trigger> [if <conditions>] then <actions>`**
rules without writing Kotlin. The engine reads device signals through the
`MetricSource` seam, evaluates rules event-driven (no polling), and
dispatches commands through the `ActionHandler` registry — **never
importing a feature module**.

The two seams it stands on already exist (see [Module Authoring
Contract](Module-Authoring-Contract)):

| Seam | Module | Role |
|---|---|---|
| `ActionHandler` + `ModuleActionRegistry` | `:core:automation` | Action (write) side — dispatch a feature's actions by `featureId`/`actionKey`. |
| `MetricSource` + `MetricDescriptor` | `:core:model` | Trigger/condition (read) side — a feature's readable signals. |

`:core:hardware`'s `HardwareRegistry` is the discovery layer over the read
side: it lets the builder list "what signals exist on this device" without
importing any feature — the symmetric partner to `ModuleActionRegistry`.

## Modules

| Module | Contents | Compose? |
|---|---|:--:|
| `:core:automation` | The contract + the rule model + pure `RuleEvaluator` + `RuleRepository` contract + runtime (`AutomationService` + `AutomationScheduler` + receivers). | No |
| `:core:data` | Room `automation.db` + `RoomRuleRepository`. | No |
| `:feature:automation-ui` | The Compose rules list + `RuleEditorSheet` builder. | Yes |
| `:feature:automation` (+ `-rooted`) | Screenless by design — 3 generic rooted power-tools (privileged intent fire, allow-listed settings override, dumpsys snapshot) exposed only via its `automation_extras` `ActionHandler`, so they surface as rule-builder actions inside `automation-ui`'s existing action picker rather than a competing screen. | No |

Dependency edge: `:core:data → :core:automation` (the `RuleRepository`
**contract** lives in `:core:automation`; `:core:data` implements it over
Room — the reverse would be a cycle). `:core:automation` also depends on
`:core:model` + `:core:root`, and on **no `:feature:*`**. The evaluator is
pure Kotlin → JVM-testable with zero Android.

## Rule model

```kotlin
@Serializable data class Rule(
    val id: String, val name: String, val enabled: Boolean = true,
    val trigger: Trigger,                          // the edge that starts evaluation
    val conditions: List<Condition> = emptyList(),
    val conditionLogic: ConditionLogic = All,      // ALL (AND) | ANY (OR)
    val actions: List<RuleAction> = emptyList(),
    val cooldownSeconds: Int = 0,                  // min seconds between firings
)
@Serializable data class RuleAction(
    val featureId: String, val actionKey: String,
    val params: Map<String, String> = emptyMap(), val requiresRoot: Boolean = false)
```

**Trigger ≠ Condition** (a deliberate distinction): a **trigger** is the
event/edge that wakes the rule ("battery crossed below 20 %", "9:00 AM",
"power connected"); a **condition** is a state gate re-checked at that
moment ("…and screen is off"). Conditions never wake a rule — this keeps
evaluation event-driven (cheap) instead of continuously re-scanning.

### Triggers

Every trigger is a `@Serializable sealed` subtype with an **explicit,
package-pinned `@SerialName`** (moving these later without re-pinning
corrupts stored rules — see [Troubleshooting →
serialization](Troubleshooting)):

- **`MetricThreshold`** — fires when a `MetricSource` value crosses
  `value` in an `edge` direction (`Rising`/`Falling`), with **hysteresis**
  (`clearValue`): a proximity `Lt 5` with `clearValue = 8` fires below 5 cm
  and re-arms only after exceeding 8 cm, so noise can't machine-gun it.
- **`Schedule`** — wall-clock via **AlarmManager** (not WorkManager, whose
  15-min floor is too coarse). Degrades by the per-rule `exact` flag + live
  permission state (table below).
- **`SystemEvent`** — `BootCompleted` / `PowerConnected` /
  `PowerDisconnected` / `Connectivity`.
- **`Geofence`** — device crosses a circular fence (`lat`/`lon`/`radiusMeters`
  + `GeofenceTransition` Enter/Exit). OS-hosted via `GeofencingClient`
  (`GeofenceRegistrar` + `GeofenceReceiver`), so it's one-shot/edge-driven
  like a Schedule alarm — **not** service-resident — and re-armed on boot and
  rule change. Needs `ACCESS_BACKGROUND_LOCATION` ("Allow all the time").
- **`ExternalBroadcast`** — an external automation app (Tasker/MacroDroid/…)
  fires the rule by broadcasting the single fixed app-namespaced action
  `dev.ranzlappen.gadget.feature.automation.EXTERNAL_TRIGGER` with a `tag`
  string extra; `AutomationExternalBroadcastReceiver` matches the tag against
  enabled rules. Exported so external apps can reach it, but there's **no
  dynamic action string** — a stray broadcast can only fire a rule the user
  already authored (whose actions are gated), so exposure is bounded.
- **`Manual`** — a widget/QS-tile/in-app "run now".

`enum ComparisonOp { Lt, Lte, Gt, Gte, Eq, Neq }`, `enum Edge { Rising,
Falling }`.

### Conditions

`MetricCompare(metricKey, op, value)`, `TimeWindow(startMinutes,
endMinutes)`, and **`Group(logic, children)`** — the nested boolean node
(W7). A top-level `conditions` list folds by `conditionLogic` (ALL/ANY);
each `Group` folds its own `children` by its own `logic`, recursing to any
depth (so `A AND (B OR C)` is expressible). `Group` carries the pinned
`@SerialName("…Condition.Group")`; the evaluator's `Condition.holds`
recurses and `referencedMetricKeys()` gathers grouped metric keys so
`RuleFireExecutor` samples them. An empty group is vacuously true.

## Evaluator (pure Kotlin)

```kotlin
class RuleEvaluator {
    fun evaluate(rule: Rule, firedTrigger: Trigger, readings: Map<String, Float>,
        now: LocalTime, rootAvailable: Boolean, sinceLastFiredMillis: Long?): List<RuleAction>
}
```

Returns the actions to dispatch — or empty if disabled, trigger mismatch,
conditions fail, in cooldown, or root-gated when root is unavailable.
Cooldown is checked **first** (`sinceLastFiredMillis < cooldownSeconds *
1000`). A `Trigger.Manual` "run now" **bypasses cooldown** (an explicit tap
is consent) but the runtime **still `markFired`s**, so a manual run delays
the next automatic fire. Being pure, it's JVM-tested exhaustively:
threshold edges, ALL/ANY, midnight-wrapping windows, root filtering,
cooldown boundary, hysteresis arm/re-arm.

## Runtime host

A dedicated **`AutomationService`** (`specialUse` FGS) resident **only
while ≥1 enabled rule needs a live subscription** (a `MetricThreshold`
stream **or** a `Connectivity` rule). Schedule / power / boot / manual rule
sets evaluate **one-shot** (alarm or receiver → start, evaluate, dispatch,
stop) with no persistent notification.

- **Metric triggers** subscribe to each `MetricSource` — preferring
  `stream()` (push; zero idle wakeups), falling back to a bounded
  `sample()` poll. An idle device with only metric rules costs nothing.
- **Schedule triggers** use AlarmManager via `AutomationScheduler`:

  | State | AlarmManager call | Builder UI |
  |---|---|---|
  | `exact = false` (default) | `setWindow` (±10 min) | "around 09:00" |
  | `exact = true` + `canScheduleExactAlarms()` | `setExactAndAllowWhileIdle` | "at 09:00" |
  | `exact = true` but denied | `setWindow` (±10 min) | "around 09:00" + badge → `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` |

  The manifest declares `SCHEDULE_EXACT_ALARM` (denied by default on
  Android 14+ fresh installs targeting SDK 33+), so the default path is
  inexact and the `exact` opt-in tolerates denial. `USE_EXACT_ALARM` is
  **not** used (Play restricts it to alarm-clock/calendar apps).
- **System events** — power events use manifest `BroadcastReceiver`s.
  `BootCompleted` re-arm reuses `:core:widgetkit`'s `BootRearmHandler`
  multibinding (**no second boot receiver**). **`Connectivity` can't use a
  manifest receiver** (broadcasts stopped being deliverable in Android N) —
  it's armed by the resident service via one default-network
  `ConnectivityManager.NetworkCallback`, with the immediate `onAvailable`
  replay swallowed as baseline (no fire-on-subscribe). A Connectivity rule
  therefore keeps the FGS resident, like a metric rule.
- **Manual** dispatches immediately via a one-shot path.

Every path funnels through **`RuleFireExecutor`** — one budget-confined
fire→evaluate→dispatch pipeline (returns the dispatched count; the manual
"run now" surface uses it for honest feedback). The flow per fired
trigger: gather a `readings` snapshot → `RuleEvaluator.evaluate` → for each
`RuleAction`, `ModuleActionRegistry.dispatch(...)`.

**Storms are bounded, not detected.** Chaining is a feature; runaway is
prevented by per-rule `cooldownSeconds` (persisted), `clearValue`
hysteresis, and an **`AutomationBudget`** (≤16 dispatches per cycle, ≤60 /
60 s globally) that drops overflow and posts a single "Automation
throttled" notification.

## Persistence

`automation.db` in `:core:data` (**v2** as of W7), holding a `rules` table
and a `rule_fire_history` audit table. The sealed `Trigger` /
`Condition[]` / `RuleAction[]` graphs are stored as
**kotlinx-serialization JSON columns** (flat schema; sealed hierarchies
evolve via `@SerialName` pinning + a `Migrator`). `last_fired_at` is
persisted so per-rule cooldowns survive process death and reboot.
`automation.db` rides the backup ZIP (format v5) and
`DatabaseCheckpointer.checkpointAll()`. The v1→v2 migration
(`AutomationDatabase.MIGRATION_1_2`) is additive — it creates
`rule_fire_history` and leaves `rules` untouched.

## High-end features (W7)

- **Nested condition groups** — `Condition.Group` (see above).
- **Rule templates + transfer** — `RuleTemplates` is a built-in recipe
  catalog (`create(id)` mints a concrete `Rule`); `AutomationTransfer`
  export/imports a versioned `RuleBundle` JSON document reusing the shared
  `AutomationJson`, surfaced in the builder as a template picker, clipboard
  export, and paste-to-import. Import saves each rule with a fresh id
  through `RuleRepository.save` (which normalizes).
- **Firing history** — `RuleFireExecutor` writes a `RuleFireRecord`
  (`Fired` / `Skipped` / `Throttled`, dispatched count, dry-run flag) on
  every evaluation to `RuleFireHistoryRepository` (bounded to 100 rows);
  the builder shows the trail with a clear action.
- **Dry-run / test-fire** — `RuleFireExecutor.dryRun` evaluates a rule
  without dispatching or touching the cooldown clock (models a Manual
  evaluation to report the true action set), surfaced as a per-rule
  "Test fire" button; it records a dry-run history entry.

## Safety — three-layer root gating

A standard build must **never** schedule or dispatch a root action:

1. **Builder filter** — the action picker drops `requiresRoot` entries on
   standard (when `RootCapabilityRegistry.hasRootAccess()` is false).
2. **Evaluator filter** — `RuleEvaluator` drops root actions when
   `rootAvailable` is false (protects rules restored from a rooted backup).
3. **Dispatch gate** — root actions reach hardware only through the
   feature's `:core:root` `RootSafetyGate` + `RootFeatureKey`.

The engine never branches on `BuildConfig.IS_ROOTED`. See [Flavors & Root
Safety](Flavors-and-Root-Safety).

## UI (`:feature:automation-ui`)

Rules list (DashCard rows: trigger/action summary, enable switch, run-now,
delete) + the `RuleEditorSheet` builder (trigger kind chips → per-kind
params, flat ALL/ANY conditions, actions with param editors auto-generated
from `ActionParam` schemas). The builder enumerates signals via
`HardwareRegistry` and actions via `ModuleActionRegistry`. Registered at
`GadgetDestination.Automation`. Saves flow `RuleRepository.save` →
`AutomationScheduler.scheduleNext` / `AutomationController.ensureStarted`;
deletes also `scheduler.cancel(id)`. The exact-alarm degradation badge
shows in the editor and as a tri-state `ModuleCapability` row (refreshed on
`ON_RESUME`). `AutomationScreenContentTest` runs in the instrumented matrix.

## Canonical end-to-end example

> "When proximity drops below 5 cm, turn the torch off."

- **Trigger:** `MetricThreshold(metricKey = "proximity", op = Lt, value =
  5f, edge = Rising)` — from the sensors feature's `MetricSource`.
- **Action:** `RuleAction(featureId = "torch", actionKey = "off")` —
  dispatched through `TorchActionHandler`.

See also: [Monitoring Framework](Monitoring-Framework) (the same
`MetricSource` seam), [Torch Blueprint](Torch-Blueprint).

---

> _Last reviewed: 2026-07-14 · Source: `docs/automation-engine.md`,
> `docs/adr/0002-automation-engine.md`, `CLAUDE.md` (automation) · Related
> modules: `:core:automation`, `:core:hardware`, `:core:data`,
> `:feature:automation-ui`, `:feature:automation`._
