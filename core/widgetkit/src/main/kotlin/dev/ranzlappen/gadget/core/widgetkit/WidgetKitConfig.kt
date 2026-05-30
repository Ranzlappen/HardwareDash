package dev.ranzlappen.gadget.core.widgetkit

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance

/**
 * Contract every feature's per-instance home-screen widget config
 * implements so `:core:widgetkit` can drive the generic
 * pin → persist → render → delete flow without knowing the feature's
 * specifics.
 *
 * A feature config (e.g. torch's `TorchWidgetConfig`) keeps its own
 * feature fields (widget kind, rate, …) and additionally satisfies this
 * contract:
 * - [displayName] — user-facing label shown in the in-app widget list and
 *   used by the toggle-feedback templates.
 * - [removed] — soft-delete flag. A non-host app can't pull a placed widget
 *   off a third-party launcher, so an in-app delete sets this `true` (the
 *   provider then renders the instance inert) rather than deleting the
 *   stored config, which the provider's self-heal would otherwise recreate.
 *   `onDeleted` purges the config for real when the user drags it off.
 * - [schemaVersion] — monotonic version of the serialized shape so a future
 *   migrator can upgrade older on-disk configs. Additive: a field rename or
 *   semantic shift bumps this; additive field changes don't need to.
 * - [appearance] — generic per-instance presentation (background mode + icon
 *   style + tap behaviour + feedback). The kit's
 *   [dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer]
 *   reads it generically so every kit-built widget gets the same chrome.
 *
 * Intentionally tiny and Compose-free — `:core:widgetkit` is foundation
 * infrastructure, not a UI module.
 */
interface WidgetKitConfig {
    val displayName: String
    val removed: Boolean
    val schemaVersion: Int
    val appearance: WidgetAppearance
}
