# Content-widget customization — design + contract

> Status: in progress. Slice 1 (this PR) ships the reusable sheet + the
> folder widget consuming it for **background**, **accent/cover tint**, and
> **name label + size**. Slice 2 adds **tap-animation** rendering for content
> widgets (needs a content press-frame). Tracked here so the next content
> widget follows the same pattern instead of re-hand-rolling a config screen.

## Why

`:core:widgetkit` has two provider archetypes:

- **Function-driven** (`BaseGadgetWidgetProvider`) — torch / vibration. Already
  has the comprehensive `WidgetCustomizationSheet` (name → function → params →
  size → `WidgetAppearanceSection` → preview).
- **Content/launcher** (`BaseContentWidgetProvider`) — the App-Organizer folder
  widget. Renders dynamic content and launches an Activity on tap.

The content archetype shipped with **no** customization surface — its configure
screen was a bare folder picker, and `buildRemoteViews` ignored the
`WidgetAppearance` the config already carried. So content widgets were
inconsistent with the function-driven ones and not customizable. This closes
that gap and makes the customization **reusable** so future content widgets
(not just folders) stay consistent.

## The reusable seam — `ContentWidgetCustomizationSheet`

`core/widgetkit/ui/ContentWidgetCustomizationSheet.kt` — the content-archetype
analogue of `WidgetCustomizationSheet`. Fully state-hoisted (the feature's
configure activity owns the values). Sections, top to bottom:

1. **Name** — `displayName`.
2. **Content** — a feature `@Composable` slot (the folder picker for folders;
   whatever "what does this widget show" picker a future content widget needs).
3. **Background** — `WidgetAppearance.background` (Glass / Solid / Transparent)
   + solid color picker. Reuses the shared `WidgetAppearance` field and the
   `WidgetAppearanceRenderer.applyBackground` paint path — identical chrome to
   function widgets.
4. **Accent / content tint** — a generic "follow the content's natural color"
   vs "custom color" control. Exposed as `tintArgb: Long?` (`null` = follow).
   The feature decides what "natural" means (folder → `Folder.baseColorArgb`).
5. **Label + size** — a name-label visibility toggle and the
   `WidgetSizePreset` starting-size hint (density adapts on resize as usual).
6. **Preview** — a feature `@Composable` slot (content widgets paint their own
   preview from their data, unlike the function widget's generic icon preview).
7. **Tap animation** *(slice 2)* — gated behind `showTapAnimation` (default
   `false`) until the content press-frame lands.

### Why not reuse `WidgetAppearanceSection` directly

That section is built around the **icon** model (icon picker + custom-import +
active/inactive keys + toggle feedback) — none of which applies to a content
widget that paints its own preview. The content sheet reuses the *data* model
(`WidgetAppearance.background` / `solidColor` / `tap`) and the *renderer*
(`applyBackground`), but not the icon-centric UI block.

## No shared-type changes

`WidgetAppearance` is reused as-is — only `background` / `solidColor` (and, in
slice 2, `tap.animation`) are read for content widgets; `iconStyle` / `feedback`
stay at their defaults and are ignored. So there's **no** change to the shared
serialized shape, no `WidgetAppearanceSerializationTest` impact, and no risk to
torch/vibration configs.

Content-specific bits live on the **feature's own** config (safe — it's the
feature's `@Serializable`, not the shared type):

- `FolderWidgetConfig.showLabel: Boolean = true`
- `FolderWidgetConfig.coverTintArgb: Long = FOLLOW_FOLDER_COLOR` (sentinel `0L`
  = follow `Folder.baseColorArgb`; any other value overrides).

## Rendering contract for content widgets

A content widget's layout must include an `@id/widget_background` ImageView as
the backmost child (the kit declares the id in `values/ids.xml`); the provider
calls `WidgetAppearanceRenderer.applyBackground(views, appearance)` in
`buildRemoteViews`. The folder layout adds that ImageView and drops its
hardcoded root `android:background`.

## Slice 2 — tap-animation for content widgets (follow-up)

The kit's `applyPressedFrame` mutates `@id/widget_icon`; content widgets have no
icon view. Slice 2 adds a **content press-frame** seam to
`BaseContentWidgetProvider` (e.g. a root-level scale/flash frame held ~150 ms,
mirroring the function-widget press chain) and flips `showTapAnimation = true`
in the folder sheet. Until then the folder tap is the launcher's default
highlight.

## Future content widgets

Build the configure screen on `ContentWidgetCustomizationSheet`, supply the
content + preview slots, store content-specific fields on the feature config,
and apply background via `WidgetAppearanceRenderer.applyBackground`. This is the
content-archetype parallel to the function-driven `WidgetCustomizationSheet`
contract; folder is the reference consumer.
