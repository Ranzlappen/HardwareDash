# Content-widget customization — design + contract

> Status: shipped. Slice 1 delivered the reusable sheet + the folder widget
> consuming it for **background**, **accent/cover tint**, and **name label +
> size**. Slice 2 added **tap-animation** for content widgets (the content
> press-frame). Kept here so the next content widget follows the same pattern
> instead of re-hand-rolling a config screen.

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
7. **Tap animation** — `WidgetAppearance.tap.animation`. Gated behind
   `showTapAnimation` (default `false`; folder passes `true`). See the
   press-frame section below.

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

## Tap-animation for content widgets (the content press-frame)

The kit's `applyPressedFrame` mutates `@id/widget_icon`; a content widget has no
icon view, and its tap **launches an Activity** rather than dispatching an
action — so the provider never sees a direct `getActivity` tap. Two problems, one
design:

- **Launch must stay instant.** Holding a press frame *before* launching would
  add ~280 ms of lag. So when a held animation (Flash/Pulse/Scale) is picked,
  the tap routes through a broadcast (`BaseContentWidgetProvider.tapAction` →
  `getBroadcast`); `handleContentTap` **starts the Activity first**, then plays
  the frame **concurrently**. Because the folder's launch target
  (`FolderPopupActivity`) is a *floating* popup, the widget stays visible behind
  it and the frame reads cleanly. None/Ripple skip the broadcast entirely and
  launch directly (launcher's native highlight).
- **No icon to frame.** `WidgetAppearanceRenderer.applyContentPressedFrame`
  paints on the always-present `@id/widget_background`: Flash = white recolour,
  Pulse = alpha drop, Scale = inset. `applyBackground`'s resting render resets
  every mutated prop so a recycled view reverts cleanly.

`buildRemoteViews` gains a `pressed` flag; the provider builds a pressed +
resting pair and hands them to the shared `playTapPressFrame`. BAL note: the
Activity start happens in the widget-click broadcast's privilege window under
`goAsync` (the same pattern the function base relies on for FGS starts).

A content widget that wants this overrides `tapAction`, wires its root via
`tapPendingIntent` (not `launchPendingIntent`), and applies
`applyContentPressedFrame` when `pressed`. Folder is the reference.

## Future content widgets

Build the configure screen on `ContentWidgetCustomizationSheet`, supply the
content + preview slots, store content-specific fields on the feature config,
and apply background via `WidgetAppearanceRenderer.applyBackground`. This is the
content-archetype parallel to the function-driven `WidgetCustomizationSheet`
contract; folder is the reference consumer.
