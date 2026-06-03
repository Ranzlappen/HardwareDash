package dev.ranzlappen.gadget.core.widgetkit.config

import kotlinx.serialization.Serializable

/**
 * The user's chosen **starting size** for a widget, picked in the
 * customization dialog.
 *
 * Android does not let an app pin a widget at an exact cell size, so this is a
 * **density hint**, not an enforced footprint: it seeds the initial render
 * (whether the label shows, how much breathing room the icon gets) until the
 * launcher reports an actual size, after which the widget renders adaptively
 * (see `BaseGadgetWidgetProvider.onAppWidgetOptionsChanged`). Every kit widget
 * is declared resizable, so the real on-screen footprint is whatever the user
 * drags it to.
 */
@Serializable
enum class WidgetSizePreset { Small, Medium, Large }
