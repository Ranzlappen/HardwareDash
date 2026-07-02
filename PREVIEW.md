# HardwareDash — live app preview

An **approximate live preview of the entire app**, auto-generated on CI from
the Compose `@Preview` matrix via [Roborazzi](https://github.com/takahirom/roborazzi)
— every screen rendered on the JVM, no device or emulator. It refreshes on
every push to `main`, keeps a per-version archive, and diffs each version
against the previous one so UI changes are easy to eyeball across builds.

> **Why not embedded live here?** GitHub sanitises `<script>`/`<iframe>` in
> Markdown, so a genuinely interactive app can't run inside an `.md` on
> github.com. The interactive "container" therefore lives on **GitHub Pages**
> (fully browsable, filter + light/dark toggle + version compare), and the
> same shots are mirrored as an inline image gallery you can read without
> leaving GitHub.

## View it

| What | Where |
|---|---|
| 🖼️ **Interactive gallery** (latest, filter + theme + compare) | **https://ranzlappen.github.io/HardwareDash/** |
| 🗂️ **Inline image gallery + per-version archive** | [`app-previews` branch → `PREVIEW.md`](https://github.com/Ranzlappen/HardwareDash/blob/app-previews/PREVIEW.md) |
| 🔀 **Version-to-version diffs** | https://ranzlappen.github.io/HardwareDash/compare/ (linked from the archive) |
| 📦 **Per-PR previews** | Download the `roborazzi-previews-pr-<n>` artifact from the PR's **App preview gallery** check |

## How it's built

1. `:screenshots` — a test-only aggregator module — depends on `:core:ui` and
   every standard `:feature:*` module. Roborazzi's
   `generateComposePreviewRobolectricTests` scans the whole
   `dev.ranzlappen.gadget` namespace and renders each `@Preview` to a PNG under
   Robolectric.
2. [`scripts/build_preview_gallery.py`](scripts/build_preview_gallery.py) turns
   the PNGs into the per-version gallery (Markdown + interactive HTML), the
   cross-version index, and a pixel diff vs the previous version.
3. [`.github/workflows/app-preview.yml`](.github/workflows/app-preview.yml)
   publishes to GitHub Pages and commits the gallery to the `app-previews`
   orphan branch (binaries stay out of `main` history).

Full docs: **[App Preview Gallery](https://github.com/Ranzlappen/HardwareDash/wiki/App-Preview-Gallery)** in the wiki.
