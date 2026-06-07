# Folder Widgets + App-Organizer migration

> Migrating the legacy App-Organizer (folder widgets + the apps domain they
> depend on) out of `:app` (`com.gadget.*`) into the modular architecture, and
> adding a **launcher/content** widget archetype to `:core:widgetkit`. Legacy
> backups (and in-place upgrades) must restore into HardwareDash v2.

## Why this is a large migration

The folder widget is a **different archetype** from torch/vibration: it renders
dynamic content (a live app-preview grid) and launches an `Activity` on tap —
neither fits widgetkit's Toggle/Momentary `WidgetFunction` model. It also
depends on the entire apps domain (`AppsDao`, `GadgetDatabase`, `Folder`/
`FolderApp`/`AppRecord` entities, `AppRepository`, `AppIconLoader`,
`FolderLockManager`, `FolderPopupActivity`, the folder-editor screens) — all of
which still live in `:app`. A feature module can't depend on `:app`, so the
domain needs a modular home first.

Decisions taken:
- **Scope:** full apps-domain migration, including import/export so legacy
  backups stay restorable on v2.
- **Archetype:** add a first-class launcher/content archetype to
  `:core:widgetkit` (folder is the reference consumer).
- **Widget config persistence:** DataStore `WidgetConfigStore` at runtime
  (consistent with torch/vibration); the legacy `apps_widget_config` Room table
  is preserved as the legacy-ingestion seam.
- **Backup home:** fold the migrated `BackupManager` into `:core:data` for now.

## Legacy facts that shape the work

- `GadgetDatabase` (v4) is one DB mixing legacy monitoring tables
  (`MetricReading`/`MetricSession`) **and** the apps tables. The modular
  `:core:data` already has a separate `MonitoringDatabase`. Only the apps
  tables need a modular home.
- `BackupManager` zips the raw `gadget_db` file + `shared_prefs/*` +
  `datastore/*` + `folder_covers/*` + `apps_favicons/*`. Restoring a **legacy**
  backup on v2 requires reading apps rows out of the old combined `gadget_db`
  and importing them into the modular store (an importer, not a file drop-in).
- Legacy folder widgets never declared their `<receiver>`s or
  `appwidget-provider` XML in the manifest — that's broken and must be added.

## Phases

- **A — Apps data layer → `:core:data`. ✅** `AppsDatabase` (Room) + entities +
  `AppsDao` + DI. Identical table/column names to legacy for row-for-row
  import. Separate from `MonitoringDatabase`.
- **B — Apps domain logic → `:feature:apps`. ✅** Repositories, scanner,
  launchers, favicon fetcher, lock manager, rules, icon loader + the `AppIcon`
  composable, ported onto the `:core:data` `AppsDao`. New feature `AppsEntryPoint`
  (for non-injectable consumers). `RuleEngineTest` ported. `PinFolderHelper`
  deferred to Phase E (it references the not-yet-moved widget provider).
  **`:app` does not depend on `:feature:apps` yet**, so the feature builds in
  isolation and the legacy domain keeps working — this avoids the Hilt
  entry-point getter collision (legacy + modular `AppsEntryPoint` share method
  names with different return types) until Phase G removes legacy. The 6
  `ic_symbol_*` drawables are duplicated into the feature (legacy still
  references its own copies).
- **C — In-app screens → `:feature:apps`.** `AppsViewModel`, folder editor,
  folder popup. Re-skin onto `ModuleScreenScaffold` + design-system tokens +
  a11y contract. Register route in `GadgetApp`/`GadgetDestination`.
  - **Discovered during B:** the legacy in-app screens (`AppsScreen` grid +
    `FolderEditorScreen`) are **orphaned** — referenced nowhere outside their
    own package and **not wired into navigation** (no `GadgetDestination.Apps`,
    not in the rail `modules` list). The earlier modularization left only the
    folder **widget**, the **popup**, and the `AppRepository` scan live (via
    `MainActivity`). So Phase C is partly *rebuild-and-rewire*: add
    `GadgetDestination.Apps` + an `appsScreen()` route + a rail entry, not a
    1:1 port.
  - **Localization migration:** the legacy screens use the Kotlin `S.apps.*` /
    `S.common.*` system (`Strings.kt`, 4 languages via `m()`, ~40 apps
    strings). These move to resource strings
    (`res/values{,-de,-es,-fr}/strings.xml`) + `stringResource(...)`.
  - **Dependency order:** `FolderEditorViewModel.pinToHome()` → `PinFolderHelper`
    → the folder widget provider (Phase E). So build the **popup path first**
    (`AppsViewModel` + `FolderPopup{ViewModel,Content,Activity}` — the popup is
    also what the widget launches), then the editor + `PinFolderHelper` after
    the provider exists in Phase E.
  - Theme: legacy `com.gadget.ui.theme.GadgetTheme` + `Theme.Gadget.Translucent`
    → modular `:core:designsystem` `GadgetTheme` + a feature translucent style.
- **D — Launcher/content archetype in `:core:widgetkit`. ✅** Added
  `BaseContentWidgetProvider<T : WidgetKitConfig>` — the second archetype
  alongside the function-driven `BaseGadgetWidgetProvider`: renders dynamic
  content + launches an Activity on tap (no function/feedback/toggle
  machinery). Owns the `onUpdate`/`onDeleted`/`onAppWidgetOptionsChanged`
  lifecycle + `renderAll` (config read + self-heal/reconcile + density) +
  the `launchPendingIntent` tap helper. Added `ContentWidgetUpdater` as the
  content-source → repaint seam (explicit `ACTION_APPWIDGET_UPDATE`
  self-broadcast), the analogue of the monitoring widget-notifier. *(Decision:
  the user chose the generic kit archetype over a feature-owned standalone
  provider; folder is the first consumer, the future web-link widget the
  likely second.)*
- **E — Folder widget onto the archetype.** Folder config becomes a
  `@Serializable WidgetKitConfig` (folderId + `WidgetSizePreset`) persisted via
  `WidgetConfigStore`; **one** `BaseContentWidgetProvider` subclass with a
  single adaptive layout (density decides name strip + tile count, replacing
  the legacy two 1x1/2x2 providers); port `PinFolderHelper` + the configure
  activity; add the missing manifest receivers + `widget_folder_*_info.xml`.
  The Room `apps_widget_config` table stays only as the legacy-ingestion seam.
- **F — Backup/restore v2 compatibility.** Migrated `BackupManager` sweeps the
  new modular DataStores + split DBs + asset dirs; a legacy-import path opens
  the old `gadget_db` read-only and imports `apps_*` rows (covers both ZIP
  restore and in-place app upgrade). Round-trip + legacy-fixture tests.
- **G — Cleanup + tests + CI traps.** Delete legacy `com.gadget.{apps,ui.apps,
  ui.folder,widget.folder}.*` + the `FolderWidgetConfig` Room entity from
  `:app`; resolve any Hilt entry-point name collisions; unit/instrumented
  tests; `@Preview` matrix; verify the standard-APK leak gate + the
  `import com.gadget.*` CI guard.

## CI-only traps to watch (no local Android SDK)

- Room schema export: don't commit a 0-byte placeholder in `schemas/` (the
  documented `Empty schema file` trap). CI generates the JSON.
- `nonTransitiveRClass`: qualify cross-module `R` refs (`WidgetKitR.id.…`).
- kotlinx-serialization: pin polymorphic discriminators / DataStore filenames
  so existing on-disk configs decode unchanged.
- Hilt entry-point getter name collisions across legacy/modular graphs.
- Sealed/`when` exhaustiveness after adding a `GadgetDestination`.
- RemoteViews-safe layouts only (no bare `<View>`).
