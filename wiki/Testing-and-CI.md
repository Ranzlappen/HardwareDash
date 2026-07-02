# Testing & CI

How Gadget is verified. There is **no Android SDK in the agent dev
container**, so `./gradlew compileDebugKotlin` won't run locally — **CI is
the compile gate.** Be extra careful about the [Troubleshooting](Troubleshooting)
traps that don't show up in a local syntax check.

## Test layers

| Layer | Where | Runs | What |
|---|---|---|---|
| **Unit (JVM)** | `src/test/` | CI (`ci-refactor.yml`) | Pure logic — serialization round-trips, the `RuleEvaluator`, maths, in-memory structures. `junit` + `mockk` + `turbine` + `coroutines-test`. |
| **Instrumented (Compose)** | `src/androidTest/` | CI emulator (`instrumented-tests.yml`) | Stateless `<Feature>ScreenContent` under `GadgetTestTheme`; modal/sheet hosting. |
| **Preview matrix** | `@Preview` fns | rendered by Studio / CI lint / the app-preview gallery | Visual coverage across theme / font / RTL / size class. Also rendered to PNGs by `:screenshots` (Roborazzi) → [App Preview Gallery](App-Preview-Gallery). |
| **Macrobenchmark** | `:benchmark` | Phase 4 | Recomposition counts, frame timing. |

### Unit tests — `:core:testing`

JVM tests carry pure-logic correctness. The CI list is curated (grow it as
modules gain tests): `:core:automation`, `:core:data`, `:core:hardware`,
`:core:widgetkit` run `testDebugUnitTest`. The `RuleEvaluator` is the
flagship — exhaustive JVM tests for threshold edges, ALL/ANY folding,
midnight-wrapping windows, root filtering, cooldown boundary, and
hysteresis arm/re-arm, all with **zero emulator** (it's a pure function).
Serialization is regression-tested by `RuleSerializationTest` and
`WidgetAppearanceSerializationTest` (the `@SerialName` pins).

### Instrumented tests

`instrumented-tests.yml` runs `connectedDebugAndroidTest` on a headless
**API 30 emulator** for: `:core:ui`, `:core:data`, `:feature:torch`,
`:feature:vibration`, `:feature:apps`, `:feature:sensors`,
`:feature:automation-ui`. Compose UI tests assert against a settled tree —
animations are disabled. Decompose every screen into a stateful
`<Feature>Screen` (Hilt-wrapped) + a stateless `<Feature>ScreenContent` so
the inner content is testable without Hilt or the real controller. Closes
#92; `GadgetBottomSheet` coverage is `ModalsTest` (closes #91).

### Preview matrix policy

Every component file with a public composable ships ≥1 `@Composable`
preview:

- **Always:** `@GadgetPreviewLightDark` + `@GadgetPreviewLargeFont` +
  `@GadgetPreviewRtl`.
- **Layout-driven components** (cards, list rows, empty states, shimmers):
  add `@GadgetPreviewSizeClasses`. Skip for width-invariant components.

Defined in `core/ui/preview/GadgetPreviewMatrix.kt`. See [Design
System](Design-System).

## CI workflows

| Workflow | Trigger | Does |
|---|---|---|
| `build-apk.yml` | push to `main` | Builds `standard` + `rooted` debug **and** R8-minified release; runs the quality gates (leak gate, libsu assertion) + `lint` job (no-legacy-import assertion, `detekt`, `ktlintCheck`); creates a GitHub Release with the release builds. |
| `ci-refactor.yml` | PR iteration | JVM unit tests (curated module list) + assembles standard+rooted debug APKs + posts an APK-size delta vs main. |
| `instrumented-tests.yml` | PR | `connectedDebugAndroidTest` on an API 30 emulator for the migrated modules. |
| `build-release.yml` | manual | Signed release APK + AAB (build-only; gates ran upstream). |
| `app-preview.yml` | push to `main` / PR / manual | Renders every `@Preview` to PNG via Roborazzi (`:screenshots`, JVM/Robolectric — no emulator), builds a per-version gallery + version diff, publishes to GitHub Pages + the `app-previews` branch. See [App Preview Gallery](App-Preview-Gallery). |
| `cleanup-ci-storage.yml` | manual | Prunes workflow artifacts + un-flagged releases. |

## Flavor builds & the leak gate

CI builds both flavors on every push. The **standard-APK leak gate** in
`build-apk.yml` runs on `assembleStandardDebug` **and**
`assembleStandardRelease` and hard-fails if the APK contains su strings,
rooted assets, or root-tier permissions. The rooted leg conversely asserts
the rooted APK **does** contain the expected libsu signature. Full pattern:
[Flavors & Root Safety](Flavors-and-Root-Safety).

The `lint` job also asserts **no legacy `com.gadget.*` imports** in modular
code — the clean-cut policy, enforced in CI.

## detekt / ktlint

`./gradlew detekt ktlintCheck` runs both flavors. Config in `config/`.
Detekt findings were driven to zero in PR #164 (closes #72 / #68); the
`continue-on-error: true` guard on the `lint` CI job remains until the
new-module code (IR, camera, battery, GPS, storage, torch overlay) is
verified clean on CI.

## Manual smoke checklist

Before declaring a migration done, on a device:

- [ ] The v1 vertical slice works end-to-end.
- [ ] Every entry point (screen + QS tile + widget) converges on the same
      `@Singleton` controller state.
- [ ] Widgets pin reliably (test on an OEM launcher that doesn't fire the
      pin callback — the `claimSolePending` rescue path).
- [ ] Backup export → re-import on a fresh install reproduces folders /
      widget configs / custom icons / monitor history.
- [ ] Rooted functions show red `ModuleCapability` rows on the standard
      flavor.

## Common CI failures

The failures that recur are catalogued with fixes in
**[Troubleshooting](Troubleshooting)** — Kotlin `internal`-leak, SDK-guard,
Compose-callback, flavor source-set, RemoteViews, Hilt, Room-schema, and
serialization traps. Because there's no local SDK, **assume CI is the first
real compile** and pre-check against that list.

---

> _Last reviewed: 2026-07-02 · Source: `.github/workflows/*`, `CLAUDE.md`
> (preview matrix + pitfalls), `docs/migration-guide.md`,
> `screenshots/build.gradle.kts` · Related modules: `:core:testing`,
> `:core:ui`, `:screenshots`, all tested features._
