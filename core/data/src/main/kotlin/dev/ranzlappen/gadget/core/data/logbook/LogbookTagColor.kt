package dev.ranzlappen.gadget.core.data.logbook

/**
 * The Logbook module's simplified color-tag system.
 *
 * The legacy `com.gadget.ui.logbook` "Ticked" tool let users paint an
 * arbitrary `#AARRGGBB` background/border pair per entry via a full ARGB
 * picker plus an editable 5-swatch palette. This module deliberately drops
 * that customization surface (out of scope per the Logbook module's
 * "session notes + reminders" brief, not a palette editor) in favour of a
 * small fixed enum. Storing a stable [name] string in Room (rather than a
 * raw ARGB [Long]) also means the design system stays the single source of
 * truth for the actual paint color — [LogbookTagColor] carries no color
 * value itself; call sites resolve one via a `@Composable` mapping to
 * `MaterialTheme.colorScheme` tones, matching the repo's "no raw color
 * literals" design-system rule.
 */
enum class LogbookTagColor {
    /** No tag — the default for a quick note. */
    None,
    Teal,
    Amber,
    Rose,
    Violet,
    Slate,
}
