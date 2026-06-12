# AI Prompt Library

Reusable prompts for future AI sessions. Each assumes the agent has read
[AI Collaboration](AI-Collaboration) and the wiki pages it references.
Copy, fill the `<…>` slots, and paste.

## Architecture review

```
Review the current diff for architecture compliance on HardwareDash.
Read Architecture, Module-Authoring-Contract, and Design-System first.
Check: dependency direction (app → feature → core, no feature↔feature, no
*-rooted in standard); no com.gadget.** imports in new code; design tokens
from LocalGadgetTheme.current (no raw dp, glass via GlassSurface); root
gating via :core:root not BuildConfig.IS_ROOTED; MetricSource +
ActionHandler bound @IntoMap. Report violations with file:line and a fix.
```

## Feature migration (general)

```
Migrate <feature> from legacy-main into :feature:<name> following the
Feature-Migration-Guide 8-step recipe. Do NOT check out legacy-main — use
`git show legacy-main:<path>`. First produce a survey (≤400 words: file
map, permissions, resources, services, persistence, cross-feature deps) and
a lean v1 cut (now vs deferred). Pause for my review before coding. Respect
the Module-Authoring-Contract (monitoring- and automation-ready) and the
Troubleshooting CI traps. There is no local Android SDK — CI is the compile
gate.
```

## Torch-style advanced migration

```
Migrate <feature> as an advanced (Torch-style) feature: hardware control +
home widget(s) + QS tile + monitoring + automation, and (if it has a
privileged mode) a rooted tier. Read Torch-Blueprint,
Widgets-Tiles-and-Surfaces, Monitoring-Framework, Automation-Engine, and
Flavors-and-Root-Safety. Reuse :core:widgetkit / :core:monitoring /
:core:automation / :core:root — never hand-roll the pin flow, chart,
dispatch, or root gate. Enforce the widget pin-reliability halves
(FLAG_MUTABLE callback + claimSolePending rescue) and the rooted safety
gate (RootSafetyGate + RootFeatureKey, hard ceilings, NonCancellable
restore). Plan first, batch in ≈5–8 files, pause after each.
```

## Minimal feature migration

```
Migrate <feature> as a MINIMAL feature (a read-only readout / simple
control). Do NOT copy Torch's widget/strobe/rooted seams — they'd be dead
infrastructure. Floor: Controller + Hilt @Binds; @HiltViewModel + stateless
<Feature>ScreenContent + <Feature>Screen; ModuleScreenScaffold + ModuleInfo
+ a tri-state ModuleCapabilitiesSection; GadgetDestination + GadgetApp
wiring; a MetricSource per readable signal + (if it has actions) an
ActionHandler; unit + one instrumented test + the preview matrix. Read
Feature-Migration-Guide (minimal template).
```

## Widget migration

```
Add a home-screen widget to :feature:<name> on :core:widgetkit. Read
Widgets-Tiles-and-Surfaces. Pick the archetype: function-driven
(BaseGadgetWidgetProvider + WidgetCustomizationSheet) or content/launcher
(BaseContentWidgetProvider + ContentWidgetCustomizationSheet). Contribute a
WidgetKitConfig + WidgetConfigStore; bind the renderer + feedback dispatcher
@IntoMap @StringKey(FEATURE_ID); RemoteViews-safe layout only (@RemoteView
classes; @id refs via WidgetKitR); enforce BOTH pin-reliability halves
(FLAG_MUTABLE+ComponentName callback AND reconcilePendingConfig/
claimSolePending) and the soft-delete (removed=true) pattern.
```

## Rooted feature

```
Add a rooted capability to :feature:<name>. Read Flavors-and-Root-Safety
and Torch-Blueprint (rooted tier). Declare a feature-side capability
interface in src/main (no libsu / com.gadget.root.* imports). Bind a no-op
in :feature:<name>-standard (standardImplementation) and the real impl in
:feature:<name>-rooted (rootedImplementation), each with its own @Binds
module. Route every privileged call through RootSafetyGate +
RootFeatureKey; hard-cap the hardware, bound any override with an absolute
time ceiling, restore state in a NonCancellable finally. Surface each
function as a ModuleCapability row (red on standard). Verify the standard
leak gate stays green.
```

## CI failure diagnosis

```
CI is failing on <workflow/job>. There is no local Android SDK, so reason
from the logs + the Troubleshooting page. Identify which trap it is
(internal-leak, SDK-guard, Compose-callback, Hilt collision/duplicate-bind,
flavor source-set, RemoteViews, Room-schema, R8/serialization, leak gate,
non-exhaustive when, init forward-ref). Give the file:line and the minimal
fix. If it's a new trap, propose a Troubleshooting entry.
```

## PR review

```
Review PR #<n> for HardwareDash. Use the AI-Collaboration review checklist.
Be frugal with comments — only flag genuine issues (architecture/safety/
consistency violations, missing tests, missed wiki updates, the
Module-Authoring-Contract done items). For each finding: file:line, why,
and the fix. Confirm CI (standard + rooted), the leak gate, and the
no-legacy-import assertion are green.
```

## Documentation update

```
I changed <X>. Update the wiki to match (in this PR). Touch only the
pages the change affects and refresh their "last reviewed" footer:
Roadmap-and-Status / Feature-Catalog / Module-Catalog for features;
Component-Catalog for new components; Asset-Catalog for new assets;
Troubleshooting for a new CI trap; Decision-Records for an architectural
choice. Rewrite into wiki-native prose — don't paste raw code dumps.
```

---

> _Last reviewed: 2026-06-12 · Source: derived from this wiki's pages ·
> Related: [AI Collaboration](AI-Collaboration)._
