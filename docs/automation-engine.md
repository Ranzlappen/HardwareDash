# Automation Engine

> **Status: design (June 2026).** This replaces the Batch-0 placeholder.
> It specifies the rule model, trigger/condition taxonomy, evaluator,
> runtime host, persistence, safety, and module boundaries for the
> cross-automation engine — the spine of the "versatile and powerful
> phone-controlling app" vision. The build is sequenced as a multi-batch
> effort (engine core → runtime → UI), tracked in epic
> [#145](https://github.com/Ranzlappen/HardwareDash/issues/145). The key
> decisions are recorded in [ADR-0002](adr/0002-automation-engine.md).

## Goal

Let the user wire **`when <trigger> [if <conditions>] then <actions>`**
rules without writing Kotlin. The engine reads device signals through the
existing `MetricSource` seam, evaluates rules event-driven (no polling),
and dispatches actuator commands through the existing `ActionHandler`
registry — **never importing a feature module**.

The two seams it stands on already exist:

| Seam | Module | Role in automation |
|---|---|---|
| `ActionHandler` + `ModuleActionRegistry` | `:core:automation` | **Action (write) side** — enumerate + dispatch a feature's invocable actions by `featureId`/`actionKey`. Already consumed by widgets. |
| `MetricSource` + `MetricDescriptor` | `:core:model` | **Trigger/condition (read) side** — a feature's readable signals, push or poll. Already consumed by monitoring; explicitly designed to feed automation too. |

`:core:hardware` (epic [#146](https://github.com/Ranzlappen/HardwareDash/issues/146))
is the **discovery/enumeration** layer over the read side — it lets the
rule-builder list "what signals exist on this device" without importing
any feature, the symmetric partner to `ModuleActionRegistry` on the write
side. The engine itself reads values through `MetricSource`; the registry
is what the *builder UI* enumerates.

## Modules

| Module | Contents | Android? | Compose? |
|---|---|---|---|
| `:core:automation` | The contract (`ActionHandler`, `ModuleAction`, `ModuleActionRegistry` — already shipped) **plus** the new rule model, the pure-Kotlin `RuleEvaluator`, the `RuleRepository` contract, and the runtime host (`AutomationService` + `AutomationScheduler` + receivers). | Yes (service/alarms) | **No** |
| `:core:data` | The Room `automation.db` (sibling to `apps.db` / `monitoring.db`) + `RuleRepository` impl. Features read repositories from `:core:data`, never Room directly. | Yes | No |
| `:feature:automation-ui` | The Compose rule list + rule builder. | Yes | Yes |

Invariant: the dependency edge points `:core:data` → `:core:automation`
(the `RuleRepository` **contract** lives in `:core:automation`; `:core:data`
implements it over Room and binds it via Hilt — the reverse edge would be a
cycle). `:core:automation` additionally depends on `:core:model`
(MetricSource) and `:core:root` (root gating) as the runtime lands, and on
**no `:feature:*` module**. The evaluator is pure Kotlin so it can be
JVM-unit-tested with zero Android.

## Rule model

```kotlin
@Serializable
data class Rule(
    val id: String,                       // UUID
    val name: String,
    val enabled: Boolean = true,
    val trigger: Trigger,                 // the edge that starts evaluation
    val conditions: List<Condition> = emptyList(),
    val conditionLogic: ConditionLogic = ConditionLogic.All,  // ALL (AND) | ANY (OR)
    val actions: List<RuleAction> = emptyList(),
    val cooldownSeconds: Int = 0,         // min seconds between firings; 0 = none
)

enum class ConditionLogic { All, Any }   // AND / OR over the condition list

/** A reference into the ActionHandler registry — dispatched verbatim. */
@Serializable
data class RuleAction(
    val featureId: String,                // ModuleActionRegistry key
    val actionKey: String,                // ModuleAction.key
    val params: Map<String, String> = emptyMap(),  // keyed by ActionParam.name
    val requiresRoot: Boolean = false,    // mirror of the ModuleAction flag, cached at author time
)
```

**Trigger vs. condition** is a deliberate distinction:
- A **Trigger** is the *event/edge* that wakes the rule ("battery crossed
  below 20 %", "9:00 AM", "power connected").
- A **Condition** is a *state gate* re-checked at that moment ("…and screen
  is off", "…and Wi-Fi is `home`"). Conditions never wake a rule on their
  own; they only permit/deny the actions once a trigger fires.

This keeps evaluation event-driven (cheap) instead of continuously
re-scanning every condition.

### Trigger taxonomy

Every trigger is a `@Serializable sealed` subtype with an **explicit,
package-pinned `@SerialName`** (see the serialization pitfall in
`CLAUDE.md` — moving these later without re-pinning corrupts stored rules).

```kotlin
@Serializable
sealed interface Trigger {

    /** Fires when a MetricSource value crosses [value] in [edge] direction. */
    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.MetricThreshold")
    data class MetricThreshold(
        val metricKey: String,            // MetricDescriptor.metricKey
        val op: ComparisonOp,             // LT, LTE, GT, GTE, EQ, NEQ
        val value: Float,
        val edge: Edge = Edge.Rising,     // fire on entering (Rising) / leaving (Falling) the predicate, not every sample
        val clearValue: Float? = null,    // hysteresis: after firing, re-arm only once the metric crosses back past this on the opposite side of [op]; null = re-arm when the predicate goes false
    ) : Trigger

    /** Wall-clock schedule. Backed by AlarmManager (exact-ish), not WorkManager. */
    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.Schedule")
    data class Schedule(
        val timeOfDayMinutes: Int,        // 0..1439, local time
        val daysOfWeek: Set<DayOfWeek> = DayOfWeek.everyDay(),
        val exact: Boolean = false,       // opt-in exact firing; see the degradation contract in Runtime host
    ) : Trigger

    /** System broadcast: boot, power connected/disconnected, connectivity changes. */
    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.SystemEvent")
    data class SystemEvent(val event: SystemEventKind) : Trigger

    /** User-initiated: a widget/QS tile/in-app "run now". No scheduling. */
    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Trigger.Manual")
    data object Manual : Trigger

    // Geofence is deferred (needs location permission UX + a fused-location
    // dependency); it plugs in here as a later sealed subtype.
}

enum class SystemEventKind { BootCompleted, PowerConnected, PowerDisconnected, Connectivity }
enum class Edge { Rising, Falling }
enum class ComparisonOp { Lt, Lte, Gt, Gte, Eq, Neq }
```

**Hysteresis (`clearValue`).** A `MetricThreshold` of proximity `Lt 5` with
`clearValue = 8` fires when the reading drops below 5 cm and then re-arms
**only** after the reading exceeds 8 cm — so sensor noise dithering around
the 5 cm threshold cannot machine-gun the rule. With `clearValue = null` the
trigger simply re-arms when the predicate goes false (one sample back above
5 cm), which is fine for clean digital signals but risky for noisy analog
sensors — prefer a `clearValue` for the latter.

### Condition model

Conditions are state gates over the same read seam:

```kotlin
@Serializable
sealed interface Condition {
    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Condition.MetricCompare")
    data class MetricCompare(val metricKey: String, val op: ComparisonOp, val value: Float) : Condition

    @Serializable @SerialName("dev.ranzlappen.gadget.core.automation.Condition.TimeWindow")
    data class TimeWindow(val startMinutes: Int, val endMinutes: Int) : Condition
}
```

`conditionLogic` (ALL/ANY) folds the list into one boolean. A nested
boolean tree is intentionally **not** in v1 — flat list + AND/OR covers the
overwhelming majority of real rules and keeps the builder UI legible. The
sealed shape leaves room to add a `Group` condition later if needed.

## Evaluator (pure Kotlin — the cheapest correctness)

```kotlin
/** No Android, no coroutines, no I/O — a pure function of inputs. */
class RuleEvaluator {
    /**
     * Given the [rule], the [firedTrigger] that woke it, and a [readings]
     * snapshot (metricKey → current value) + [now], return the actions to
     * dispatch — or empty if the rule is disabled, the trigger doesn't
     * match, or the conditions fail. Root-gated actions are filtered per
     * [rootAvailable].
     */
    fun evaluate(
        rule: Rule,
        firedTrigger: Trigger,
        readings: Map<String, Float>,
        now: LocalTime,
        rootAvailable: Boolean,
        sinceLastFiredMillis: Long?,      // ms since this rule last fired; null = never fired
    ): List<RuleAction>
}
```

The evaluator returns **empty** (the rule is in cooldown) when
`cooldownSeconds > 0 && sinceLastFiredMillis != null &&
sinceLastFiredMillis < cooldownSeconds * 1000L` — before any
trigger/condition work, so a rule under cooldown can't dispatch regardless
of how its trigger fired.

Because it's a pure function, batch **3.2** unit-tests it exhaustively:
threshold edges (rising vs falling, op boundaries), ALL/ANY folding,
enabled/disabled, time windows that wrap midnight, **root-gated actions
filtered out when `rootAvailable == false`**, the **cooldown boundary**
(just-under / exactly-at / just-over `cooldownSeconds * 1000`), and
**hysteresis arm/re-arm sequences** (fire at `value`, suppressed until the
reading crosses `clearValue`, then re-arm). No emulator required.

## Runtime host

**Decision (see ADR-0002): a dedicated `AutomationService` foreground
service that self-stops when zero rules are enabled** — mirroring
`MonitorService`'s self-stop pattern, not a second always-on process.

The FGS is resident **only while ≥1 enabled rule has a metric-stream
trigger** (the one trigger kind that needs a continuous subscription).
Schedule-, broadcast-, and manual-only rule sets evaluate **one-shot**
(alarm or receiver → start, evaluate, dispatch, stop) with no persistent
service or notification — so a user who only has "at 09:00…" / "on power
connected…" rules never sees an ongoing automation notification.

Rationale, weighed against Android background limits:

- **Metric-threshold triggers** subscribe to each referenced
  `MetricSource` — preferring `stream()` (push; zero idle wakeups) and
  falling back to a bounded `sample()` poll. This reuses the exact seam
  monitoring already drives, so an idle device with only metric rules
  costs nothing.
- **Schedule triggers** use `AlarmManager` — **not** `WorkManager`, whose
  15-minute floor is too coarse for "at 9:00 turn X on". An
  `AutomationScheduler` (re)arms the next alarm per enabled `Schedule`,
  degrading by the per-rule `exact` flag and the live permission state:

  | State | AlarmManager call | Builder UI |
  |---|---|---|
  | `exact = false` (default) | `setWindow` (±10 min) | "around 09:00" |
  | `exact = true` and `canScheduleExactAlarms()` | `setExactAndAllowWhileIdle` | "at 09:00" |
  | `exact = true` but permission denied | `setWindow` (±10 min) | "around 09:00" + badge "needs Alarms & reminders" linking `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` |

  The manifest declares `SCHEDULE_EXACT_ALARM` — a user-grantable special
  permission that is **denied by default on Android 14+ fresh installs**
  targeting SDK 33+, so the default path must be inexact and the `exact`
  opt-in must tolerate denial. `USE_EXACT_ALARM` is **explicitly not used**:
  Play restricts it to alarm-clock / calendar apps, which Gadget is not.
- **System-event triggers** use manifest `BroadcastReceiver`s for the
  power events (`ACTION_POWER_CONNECTED`/`_DISCONNECTED` are on the
  implicit-broadcast exemption list). `BootCompleted` rules fire from the
  boot-rearm path: `BootCompleted` re-arm reuses `:core:widgetkit`'s
  `BootRearmHandler` Hilt multibinding (one more handler keyed by an
  automation `FEATURE_ID`) — **don't** add a second boot receiver.
  **Amendment (batch 3.3):** `SystemEventKind.Connectivity` is *modeled but
  not yet armed* — connectivity broadcasts stopped being deliverable to
  manifest receivers in Android N, so arming it needs a registered
  `NetworkCallback` inside a resident component; queued behind the builder
  UI batch (which should hide it from the trigger picker until then).
- **Manual triggers** dispatch immediately via a one-shot path; they don't
  need the service running.

  **Cooldown vs. Manual (decided, batch 3.3):** cooldown bounds *automated*
  storms, so a `Trigger.Manual` "run now" tap **bypasses the cooldown
  check** — an explicit tap is consent and is already human-rate-limited.
  But the runtime **still calls `markFired`** after a manual dispatch, so a
  manual run *delays the next automatic fire* (which is the intuitive
  behaviour). The bypass lives in `RuleEvaluator` (`rule.trigger is
  Trigger.Manual`), not the runtime, so it's JVM-tested. **Runtime
  contract:** `evaluate` always receives the rule's *own* trigger instance
  as `firedTrigger` (the runtime resolves which rule a fired event belongs
  to before calling the evaluator); `firedTrigger != rule.trigger` is the
  evaluator's defensive guard, not a routing mechanism.

Why a separate FGS and not folding into `MonitorService`: monitoring's
"one shared FGS for the whole app" rule is about not spawning a service
*per feature module*. Automation is a single cross-cutting engine, so one
automation FGS is consistent with that spirit, and an independent
enable/disable lifecycle (self-stop when no rule is enabled) keeps the two
subsystems decoupled. If dual persistent notifications prove annoying in
practice, consolidating both into one "Gadget background" service is a
future refinement — recorded as a follow-up in ADR-0002, not v1.

The service flow per fired trigger: gather the `readings` snapshot (one
`sample()` per metric the matched rules reference) → `RuleEvaluator.evaluate`
→ for each returned `RuleAction`, `ModuleActionRegistry.dispatch(featureId,
actionKey, params)`. The dispatch path is already structured-concurrency
safe and feeds the same runtime/monitoring as in-app controls.

The service enforces an **`AutomationBudget`** so chaining (one rule's
action moving a metric that triggers another rule) can't spin: at most **16
action dispatches per trigger-evaluation cycle** and a global rolling cap of
**60 dispatches / 60 s**. On breach it drops the overflow, logs it, and
posts a single "Automation throttled" notification instead of silently
machine-gunning. These constants are tunable at batch 3.3.

## Persistence

A new Room database **`automation.db`** in `:core:data`, sibling to
`apps.db` and `monitoring.db`. One table:

```
rules(id TEXT PK, name TEXT, enabled INTEGER, trigger_json TEXT,
      conditions_json TEXT, condition_logic TEXT, actions_json TEXT,
      cooldown_seconds INTEGER, created_at INTEGER, updated_at INTEGER,
      last_fired_at INTEGER)
```

`last_fired_at` (nullable) is persisted so per-rule cooldowns survive
process death and reboot — a metric that is flapping across its threshold
at restart cannot re-storm because the cooldown clock is restored, not
reset.

The sealed `Trigger`/`Condition`/`RuleAction` graphs are stored as
**kotlinx-serialization JSON columns**, not normalized tables — this keeps
the relational schema flat and lets the sealed hierarchies evolve through
`@SerialName` pinning + (if ever needed) a JSON migrator, exactly as
`:core:widgetkit` configs do. `RuleRepository` exposes CRUD + an observable
`Flow<List<Rule>>`. `schemaVersion = 1`; the committed `schemas/` dir gets
the exported schema, and any later bump ships a migration test (load old →
migrate → assert) per the repo convention.

`automation.db` joins the whole-app ZIP as `databases/automation.db`,
bumping the backup format **v4 → v5** (the `BackupManager` change ships in
batch 3.2 alongside the DB). Restore stages it off the live Room path with a
WAL checkpoint, following the exact `apps.db` staging pattern hardened in
PRs #143/#144. Rules restored from a **rooted** device's backup onto a
standard install are then defanged by gating layer 2 (the evaluator's
`rootAvailable` filter, already specified in § Safety).

## Safety — root gating

A **standard build must never schedule or dispatch a root action.** Three
layers:

1. **Builder filter** — the rule-builder's action picker filters out
   `ModuleAction.requiresRoot` entries on the standard flavor (the same
   flavor filter the widget function picker already applies).
2. **Evaluator filter** — `RuleEvaluator.evaluate(..., rootAvailable)`
   drops any `RuleAction` whose `requiresRoot` is true when root isn't
   available. This protects rules that arrived via a restored backup from
   a rooted device.
3. **Dispatch gate** — root actions reach hardware only through the
   feature's existing `:core:root` `RootSafetyGate` + `RootFeatureKey`
   path (capability + opt-out + rate-limit); the engine adds no new
   privileged path. `rootAvailable` is sourced from
   `RootCapabilityRegistry`.

The engine never branches on `BuildConfig.IS_ROOTED`; it reads
availability from `:core:root`, so the Hilt seam picks the right behaviour
per flavor.

## Canonical end-to-end example

> "When proximity drops below 5 cm, turn the torch off."

- **Trigger:** `MetricThreshold(metricKey = "proximity", op = Lt, value = 5f, edge = Rising)`
  — sourced from the sensors feature's `MetricSource` (epic #146 / the
  sensors migration).
- **Action:** `RuleAction(featureId = "torch", actionKey = "off")` —
  dispatched through `TorchActionHandler`.

This is the acceptance test for the engine runtime (batch 3.3) and the
builder (batch 3.4).

## Build sequencing

1. **3.1 — this doc + ADR-0002.** (done)
2. **3.2 — engine core:** rule model + pinned `@SerialName`s + round-trip
   tests; `automation.db` + `RuleRepository` (+ backup v5 — `BackupManager`
   adds `databases/automation.db`); pure-Kotlin `RuleEvaluator` + exhaustive
   JVM tests.
3. **3.3 — runtime:** `AutomationService` + `AutomationScheduler` + system
   receivers + boot re-arm; an integration test driving a rule →
   dispatch → torch controller.
4. **3.4 — `:feature:automation-ui`:** rules list (empty-state) + rule
   builder (trigger picker → conditions → actions rendered from
   `ModuleAction.params`), wired into `GadgetDestination.Automation` +
   `GadgetApp`; instrumented stateless-screen test under
   `instrumented-tests.yml`.

`:core:hardware` (epic #146) lands alongside 3.2/3.3 as the enumeration
layer the builder uses to list available trigger/condition signals.
