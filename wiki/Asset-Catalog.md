# Asset Catalog

Inventory of visual and Android resources. Naming conventions and
replacement rules matter most here — keep this **semi-generated**: refresh
the counts from the commands at the bottom so it doesn't rot.

Per the maintenance rules, **every new asset category updates this page.**

## Launcher / app icon

| Asset | Path | Notes |
|---|---|---|
| Launcher icon | `app/src/main/res/mipmap-*/ic_launcher.webp` + `_round.webp` | 5 densities (mdpi → xxxhdpi), WEBP. |
| Adaptive layers | `app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml` | Vector adaptive-icon layers. |
| Monochrome icon | `app/src/main/res/drawable/ic_gadget_monochrome.xml` | Themed-icon (Android 13+). |
| Brand artwork | `…/drawable-nodpi/ic_gadget_artwork.png` | Per-flavor variants under `standardDebug/`, `standardRelease/`, `rootedDebug/`, `rootedRelease/` — lets each flavor/build show distinct artwork. |
| Brand background | `app/src/main/res/drawable/ic_gadget_background.xml` | Vector. |

**Flavor availability:** the launcher mipmaps are shared (`src/main`); the
`ic_gadget_artwork.png` is overridden per flavor+buildType source set.

## Vector drawables (icons)

Total ~23 vector XML drawables across the app + 4 feature/core modules:

| Module | Count | Examples |
|---|---:|---|
| `app/src/main` | 6 | launcher layers, `ic_gadget_*`. |
| `core/widgetkit` | 3 | `widget_background_glass.xml`, `widget_background_solid.xml`, `widget_tap_ripple.xml` — the shared widget background paints. |
| `feature/torch` | 4 | `ic_flashlight_on/off.xml`, `ic_strobe.xml`, `ic_strobe_on.xml`. |
| `feature/vibration` | 3 | `ic_vibration_on/off.xml`, `ic_vibration_pattern.xml`. |
| `feature/apps` | 7 | `ic_symbol_folder/games/menu_book/music_note/star/work.xml` (folder cover symbols), `widget_folder_background.xml`, `widget_refresh_icon.xml`. |

**UI icons inside Compose** use `androidx.compose.material.icons.*`
(`Icons.Outlined.*` / `Icons.Filled.*`) — not drawable resources — so most
in-app iconography needs no asset file. Drawable XMLs exist mainly for
**RemoteViews widgets** (which can't host Compose) and the launcher.

**Naming convention:** `ic_<feature>_<state>` for feature icons
(`ic_flashlight_on`); `widget_<role>` for widget chrome
(`widget_background_glass`); `ic_symbol_<name>` for folder cover symbols.

## Widget resources

Widgets are RemoteViews — see [Widgets, Tiles &
Surfaces](Widgets-Tiles-and-Surfaces) for the inflation constraints.

**Layouts (`res/layout/widget_*.xml`):**

| Feature | Layouts |
|---|---|
| torch | `widget_flashlight.xml`, `widget_strobe.xml`, `widget_monitor.xml`, `widget_monitor_chart.xml` |
| vibration | `widget_vibrate.xml`, `widget_pattern.xml`, `widget_monitor.xml`, `widget_monitor_chart.xml` |
| apps | `widget_folder.xml` |
| app (legacy) | `widget_layout.xml`, `widget_action.xml` |

**AppWidget metadata (`res/xml/widget_*_info.xml`):** one
`<appwidget-provider>` per widget type — torch (4: flashlight, strobe,
monitor, monitor-chart), vibration (4: vibrate, pattern, monitor,
monitor-chart), apps (1: folder).

> **RemoteViews-safe layouts only.** Widget layouts may use only
> `@RemoteView` classes (`FrameLayout`, `LinearLayout`, `ImageView`,
> `TextView`, `ProgressBar`, …) — never a bare `<View>`/`<Space>`. The
> shared id pool (`@id/widget_background`, `@id/widget_icon`,
> `@id/widget_label`) is declared in `core/widgetkit/.../values/ids.xml`;
> reference them from Kotlin via `WidgetKitR.id.*` (non-transitive R).

## Themes & colours

| File | Role |
|---|---|
| `app/src/main/res/values/themes.xml` | App theme (Compose `GadgetTheme` is the real source; this is the bootstrap/splash theme). |
| `app/src/main/res/values/colors.xml` | Bootstrap colours. |
| `core/widgetkit/.../values/colors.xml` | Widget chrome colours (RemoteViews can't read Compose tokens). |
| `feature/torch/.../colors.xml`, `feature/vibration/.../colors.xml` | Feature widget colours. |
| `feature/apps/.../themes.xml` | Folder popup activity theme. |

In-app colour comes from `LocalGadgetTheme.current.colors`, **not** these
XML files — see [Design System](Design-System).

## Strings (localization)

`strings.xml` files (one per module that ships user copy): `app`,
`core/monitoring`, `core/ui`, `core/widgetkit`, `feature/apps`,
`feature/automation-ui`, `feature/sensors`, `feature/torch`,
`feature/vibration`.

**Convention:** generic chrome strings live in the **core** module that
owns the component (e.g. `widget_kit_appearance_*` in `:core:widgetkit`,
the `ModuleInfo` section labels in `:core:ui`); feature-specific copy
(permission rationales, OS notes) lives in the **feature** module. Legacy
`:app` strings still use the `localization/Strings.kt`
`m(lang, en, de, es, fr)` helper; new modules use Android `strings.xml`.

## Runtime asset stores (not in `res/`)

These are user-generated files under `filesDir`, swept into the backup ZIP
(format v5 — see [Troubleshooting → Backup](Troubleshooting)):

| Dir | Contents |
|---|---|
| `folder_covers/<id>.png` | App-Organizer folder cover photos. |
| `apps_favicons/<sha1>` | Web-link favicon cache. |
| `widget_icons/custom:<uuid>.webp` | Custom torch/vibration widget icons (saved `WEBP_LOSSY` q80, API 30+). |

**Replacement rule:** when you add a new `filesDir` asset dir, append a
`(subdir, prefix)` pair to `BackupManager.filesDirAssetSweeps` and bump
`BACKUP_FORMAT_VERSION`.

## Refreshing this catalog

```bash
# Raster assets
find . -path "*/res/*" \( -name "*.png" -o -name "*.webp" \) | sort
# Vector drawables by module
find . -path "*res/drawable*" -name "*.xml" | awk -F/ '{print $2"/"$3}' | sort | uniq -c
# Widget layouts + metadata
find . -path "*res/layout*" -name "widget_*.xml" | sort
find . -path "*res/xml*" -name "widget_*_info.xml" | sort
# String / theme / colour files
find . -name "strings.xml" -o -name "themes.xml" -o -name "colors.xml" | sort
```

---

> _Last reviewed: 2026-06-12 · Source: live `find` over `*/res/*`,
> `docs/backup.md` · Related modules: `:core:widgetkit`, `:feature:torch`,
> `:feature:vibration`, `:feature:apps`, `:app`._
