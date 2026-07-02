# App Preview Gallery

An **approximate live preview of the entire app**, generated on CI and
published so anyone can see every screen — and how it changed between
versions — without building or installing an APK.

## What you get

- **Interactive HTML container** on GitHub Pages:
  <https://ranzlappen.github.io/HardwareDash/> — a filterable, light/dark
  gallery of every screen, plus a cross-version compare view.
- **Inline Markdown gallery** on the `app-previews` branch
  ([`PREVIEW.md`](https://github.com/Ranzlappen/HardwareDash/blob/app-previews/PREVIEW.md))
  — the same shots as image tables that render on github.com.
- **Per-version archive + diffs** — one gallery per build, with a
  before│after│pixel-diff view against the previous version.
- **Per-PR previews** — the workflow renders on pull requests and uploads the
  PNGs as an artifact (no publish).

The repo root [`PREVIEW.md`](https://github.com/Ranzlappen/HardwareDash/blob/main/PREVIEW.md)
is a static pointer to all of the above.

## Why not a truly live app embedded in the `.md`?

GitHub renders Markdown through a sanitizer that strips `<script>` and
`<iframe>`, so an interactive app cannot run inside an `.md` viewed on
github.com. The pragmatic "live approximate" preview is an auto-refreshed
**screenshot gallery**: the interactive controls live on GitHub Pages, and the
images are mirrored inline in Markdown. See
[ADR-0004](Decision-Records#adr-0004--app-preview-gallery-via-roborazzi-preview-scanner).

## How it works

```
@Preview matrix  ──▶  :screenshots (Roborazzi + Robolectric, JVM)  ──▶  PNGs
      │                                                                  │
      │                    scripts/build_preview_gallery.py  ◀───────────┘
      ▼                                   │
 no per-screen               ┌────────────┼───────────────┐
 test code needed            ▼            ▼               ▼
                         PREVIEW.md   HTML container   version diff
                             └──────────── published ───────────┘
                          GitHub Pages  +  app-previews orphan branch
```

### 1. Render — `:screenshots` module

`:screenshots` is a **test-only aggregator** (ships nothing to users). It is the
single sanctioned place that depends broadly on `:core:ui` and every standard
`:feature:*` module, because Roborazzi's preview scanner can only render a
screen whose class is on its classpath.

```kotlin
// screenshots/build.gradle.kts
roborazzi {
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("dev.ranzlappen.gadget")
    }
}
```

`generateComposePreviewRobolectricTests` discovers **every** `@Preview` in the
namespace and synthesises one Robolectric test per preview, honouring each
preview's `uiMode` (light/dark), `fontScale`, and `device`/`widthDp`. So the
existing `@GadgetPreviewLightDark` / `@GadgetPreviewLargeFont` /
`@GadgetPreviewRtl` / `@GadgetPreviewSizeClasses` matrix (see
[Design System](Design-System)) renders as separate PNGs with **no extra
code**. Everything runs on the JVM under Robolectric — no emulator.

Rooted-only feature modules are pulled in **only** with
`-PenableRootedPreviews=true` (mirroring the `-PenableLsposedModule` gate), so
the default render never compiles against root code — the
[flavor-separation invariant](Flavors-and-Root-Safety) holds.

Render locally-equivalent command (CI has no local SDK — this is the compile gate):

```
./gradlew :screenshots:recordRoborazziDebug
# PNGs → screenshots/build/outputs/roborazzi/*.png
```

### 2. Assemble — `scripts/build_preview_gallery.py`

Stdlib-only. Groups PNGs by feature, writes the per-version gallery
(`gallery.md` + `index.html` + `manifest.json`), maintains the cross-version
index, and — using ImageMagick `compare` — builds the pixel diff against the
previous version.

### 3. Publish — `.github/workflows/app-preview.yml`

On push to `main` (and `workflow_dispatch`): renders, builds the gallery,
deploys to GitHub Pages, and commits the gallery to the `app-previews` orphan
branch (keeping binaries out of `main` history). On pull requests it renders
and uploads an artifact only.

## One-time setup

Enable Pages once: **Settings → Pages → Build and deployment → Source:
GitHub Actions**. Until then the deploy step fails but the `app-previews`
branch + PR artifacts still work.

## Adding a screen to the gallery

Nothing to do beyond the normal [Module Authoring Contract](Module-Authoring-Contract)
rule that every public composable file ships a `@Preview`. If a feature module
is new, add it to `screenshots/build.gradle.kts`'s dependency list. Ensure the
preview wraps its content in `GadgetTheme` / `GadgetThemedPreview` — an
unwrapped preview throws (`LocalGadgetTheme` has no default) and fails the
render.

## Related

- [Testing & CI](Testing-and-CI) · [Design System](Design-System) ·
  [Flavors & Root Safety](Flavors-and-Root-Safety) ·
  [Decision Records](Decision-Records) · [Troubleshooting](Troubleshooting)

---

> _Last reviewed: 2026-07-02 · Source: `screenshots/build.gradle.kts`,
> `scripts/build_preview_gallery.py`, `.github/workflows/app-preview.yml`,
> `gradle/libs.versions.toml`, `PREVIEW.md` · Related modules: `:screenshots`,
> `:core:ui`, all `:feature:*`._
