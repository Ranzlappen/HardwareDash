# ADR-0001: Monorepo refactor onto module-per-feature layout

- **Status:** Accepted (branch `claude/refactor-2026`)
- **Date:** 2026-05-12
- **Deciders:** project owner, Claude (implementer), Grok (reviewer)

## Context

The legacy layout puts every screen, every Hilt module, every Compose
helper, and every hardware integration under
`app/src/main/java/com/gadget/` in one Gradle module. As of May 2026:

- The module is ~80 kLOC and growing.
- Cold-cache builds on a 16-core dev box exceed 2 minutes.
- Most PRs touch unrelated files because there is no architectural seam
  between features — the IR screen, the bug-report screen, and the
  Flipper Zero integration all share an enclosing module.
- Adding a rooted-only feature means dropping code under
  `app/src/rooted/` and trusting AGP source-set scoping + the CI leak
  gate to prevent bleed-through. That works, but the seam is invisible
  in the IDE.

## Decision

Adopt the modular monorepo layout standardised by Now-In-Android and
many large Compose apps:

```
app/                     single application module
core/<name>/             infrastructure (data, designsystem, hardware, …)
feature/<name>/          one user-facing capability per module
feature/<name>-rooted/   parallel root-only capability surface
benchmark/               macrobenchmark host
build-logic/             convention plugins (DRY for module build files)
lsposed-module/          bundled Xposed asset (rooted flavor only)
```

Invariants:

1. `core/*` modules never depend on `feature/*` modules.
2. A `feature:<name>` module never depends on another `feature:<name>`
   directly. Cross-feature contracts go through `core:domain`,
   `core:navigation`, or `core:hardware`.
3. The standard application APK never compiles against a `*-rooted`
   module. The rooted flavor adds them via `rootedImplementation`
   only.
4. Each module declares its own `namespace` under
   `dev.ranzlappen.gadget.<core|feature>.<name>`. Legacy
   `com.gadget.*` packages remain in place during migration.

## Consequences

### Positive

- **Build parallelism.** Independent feature modules build in
  parallel, improving cold-cache build time. Single-feature edits only
  recompile that feature's module.
- **Compile-time root isolation.** The leak gate in CI continues to
  assert no rooted code lands in the standard APK; the module graph
  reinforces that at compile time.
- **Per-module BuildConfig.** Each module gets its own `BuildConfig`.
  Cross-module flags move to `core/common` configuration objects.
- **Reviewability.** Single-feature PRs touch a single module. Cross-
  module change requires an explicit `core/` API surface change,
  surfaced in the diff.

### Negative

- **Onboarding cost.** Contributors must learn which module to drop a
  new file into. Mitigation: `scripts/new-feature.sh` (Batch 1)
  becomes the supported entry point; `README.md` and
  `docs/architecture/overview.md` document the taxonomy.
- **Per-batch CI cost during migration.** Every batch runs the full CI
  matrix. Mitigation: batches are small (5–8 files) and bounded.
- **Hilt complexity at module boundaries.** Each feature module
  contributes Hilt entry points to `:app`. Mitigation: convention
  plugin `gadget.android.feature.hilt` (Batch 1+) standardises the
  wiring.

## Alternatives considered

- **Stay monolithic.** Rejected: build time and merge contention
  dominate. The repo is past the inflection point where modularisation
  pays back its overhead.
- **Multi-repo split.** Rejected: rooted/standard parity is enforced
  by a shared CI matrix; splitting repos would lose that and require
  re-implementing the leak gate twice.
- **Dynamic feature modules** (Play install-on-demand). Rejected: the
  rooted flavor is sideloaded; Play install-on-demand is not used.
  Static modularisation gives the build-time wins without the install
  complexity.
- **Multi-module by layer** (`ui/`, `data/`, `domain/` instead of
  per-feature). Rejected: layer-only modularisation re-creates the
  monolith's "every PR touches every module" problem on a smaller
  scale. Per-feature is the modularisation seam that maps to how the
  user thinks about the app.

## Migration plan

See `MASTER-PLAN.md`. Batches are atomic and reviewable:

- **Batch 0** (this batch): empty module skeletons + settings wiring.
- **Batch 1**: convention plugins (`build-logic/`).
- **Batch 2**: applicationId change + `app/build.gradle` → Kotlin DSL.
- **Batch 3+**: incremental migration of screens from
  `app/src/main/` into `feature/<name>/`, one feature per batch.

Each batch is reviewed and approved before the next is started — see
`AI-COLLABORATION.md`.
