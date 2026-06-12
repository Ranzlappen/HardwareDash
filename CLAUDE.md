# Claude Bootstrap

The full HardwareDash AI collaboration guide and all project documentation
live in the **[GitHub Wiki](https://github.com/Ranzlappen/HardwareDash/wiki)**
(source mirrored in the [`wiki/`](wiki/) directory of this repo).

Before planning or coding, read — in order:

1. [AI Collaboration](https://github.com/Ranzlappen/HardwareDash/wiki/AI-Collaboration)
2. [Module Authoring Contract](https://github.com/Ranzlappen/HardwareDash/wiki/Module-Authoring-Contract)
3. [Design System](https://github.com/Ranzlappen/HardwareDash/wiki/Design-System)
4. [Feature Migration Guide](https://github.com/Ranzlappen/HardwareDash/wiki/Feature-Migration-Guide)
5. [Roadmap & Status](https://github.com/Ranzlappen/HardwareDash/wiki/Roadmap-and-Status)

For deeper work also read the relevant subsystem page:
[Torch Blueprint](https://github.com/Ranzlappen/HardwareDash/wiki/Torch-Blueprint),
[Widgets, Tiles & Surfaces](https://github.com/Ranzlappen/HardwareDash/wiki/Widgets-Tiles-and-Surfaces),
[Monitoring Framework](https://github.com/Ranzlappen/HardwareDash/wiki/Monitoring-Framework),
[Automation Engine](https://github.com/Ranzlappen/HardwareDash/wiki/Automation-Engine),
[Flavors & Root Safety](https://github.com/Ranzlappen/HardwareDash/wiki/Flavors-and-Root-Safety),
and [Troubleshooting](https://github.com/Ranzlappen/HardwareDash/wiki/Troubleshooting)
(the CI-only traps — there is no Android SDK locally; CI is the compile gate).

## Hard local rules

- New code lives under `dev.ranzlappen.gadget.**`.
- Do **not** import from `com.gadget.**` in new code (CI-enforced).
- Standard/rooted flavor separation is mandatory — never branch on
  `BuildConfig.IS_ROOTED`; go through the `:core:root` Hilt seam.
- Do **not** add feature-to-feature dependencies — route through `core/*`.
- Use the design system (`LocalGadgetTheme.current`, `:core:ui`
  components), not raw Material components or raw `dp`.
- A migration isn't done until it is both monitoring- **and**
  automation-ready (`MetricSource` + `ActionHandler` bound `@IntoMap`).

## Keep the wiki in sync

When you change code, update the matching wiki page in the same PR
(features → Roadmap/Feature/Module catalogs; components →
Component-Catalog; assets → Asset-Catalog; a new CI trap → Troubleshooting;
an architectural choice → Decision-Records). Every wiki page carries a
"last reviewed / source paths / related modules" footer — refresh it.
