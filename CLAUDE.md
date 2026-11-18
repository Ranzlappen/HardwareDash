# Claude Bootstrap

**Direction lives in [`MASTER-PLAN.md`](MASTER-PLAN.md)** — read it first.
v3 ("pragmatic") is the current direction; v1 (`legacy-main`) and v2
(`archive/v2-modular-2026`) are read-only archives.

How-to documentation lives in the
**[GitHub Wiki](https://github.com/Ranzlappen/HardwareDash/wiki)** (source in
[`wiki/`](wiki/)). Before planning or coding, read — in order:

1. [`MASTER-PLAN.md`](MASTER-PLAN.md) — principles, target shape, current batch.
2. [AI Collaboration](wiki/AI-Collaboration.md) — workflow, branches, batches.
3. [Module Authoring Contract](wiki/Module-Authoring-Contract.md) — the v3
   required core + opt-in extras.
4. [Design System](wiki/Design-System.md).
5. [Roadmap & Status](wiki/Roadmap-and-Status.md) — what actually works.

Read a subsystem page only when the task touches it:
[Flavors & Root Safety](wiki/Flavors-and-Root-Safety.md),
[Widgets, Tiles & Surfaces](wiki/Widgets-Tiles-and-Surfaces.md),
[Monitoring Framework](wiki/Monitoring-Framework.md),
[Automation Engine](wiki/Automation-Engine.md),
[Troubleshooting](wiki/Troubleshooting.md) (CI-only traps — there is no
Android SDK locally; CI is the compile gate).

## Hard local rules

- New code lives under `dev.ranzlappen.gadget.**`; never import `com.gadget.**`
  (CI-enforced).
- Standard/rooted separation is mandatory — never branch on
  `BuildConfig.IS_ROOTED`; rooted code goes through the `:core:root` Hilt seam.
- No feature-to-feature dependencies — route through `core/*`.
- Use the design system (`LocalGadgetTheme.current`, `:core:ui` components), not
  raw Material components or raw `dp`.
- A feature is done when its core job works on a real phone (a ticked line in
  [`docs/v3-smoke-test.md`](docs/v3-smoke-test.md)) — not when it has seams.
  `MetricSource` / `ActionHandler` / widgets / rooted rows are **opt-in**, added
  only when they give the user something. Never add `assert_*` actions.
- Work only on the assigned `claude/*` branch; small batches; commit → push →
  pause for review. Never push to `main`.

## Keep docs in sync

Update `MASTER-PLAN.md` checkboxes in the same batch as the work. When
behaviour changes, update the matching wiki page and smoke-test line. Prefer
shortening a stale page over adding to it.
