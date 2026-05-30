package dev.ranzlappen.gadget.core.widgetkit.config

import androidx.annotation.DrawableRes

/**
 * A single icon choice surfaced by the kit's appearance picker. Features
 * map their per-feature icon catalog entries (e.g. torch's
 * `WidgetIconCatalog.Entry`) into a `List<WidgetIconChoice>` and pass it
 * to `WidgetAppearanceSection`.
 *
 * Generic shape so the kit's picker UI doesn't need to know about any
 * feature-specific catalog type.
 *
 * @property key stable identifier persisted in `IconStyle.activeKey`
 *               / `inactiveKey`.
 * @property drawable bundled drawable resource — feature's own R.
 * @property displayName user-facing label shown as the swatch's
 *                       content description.
 */
data class WidgetIconChoice(
    val key: String,
    @DrawableRes val drawable: Int,
    val displayName: String,
)
