package dev.ranzlappen.gadget.feature.apps.widget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for a placed folder widget, persisted as JSON via
 * the kit's [dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore].
 * The modular replacement for the legacy Room `apps_widget_config` row — which
 * now survives only as the legacy-ingestion seam (see `:core:data`
 * `FolderWidgetConfig`).
 *
 * A content widget has no function/action, so this carries only:
 *  - [folderId] — the App-Organizer folder this instance renders + opens.
 *  - [sizePreset] — density hint until the launcher reports a real size
 *    (replaces the legacy "1x1"/"2x2" `sizeVariant`; one adaptive layout now
 *    handles both via [dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity]).
 *  - the [WidgetKitConfig] contract fields (displayName / removed /
 *    schemaVersion / appearance).
 *
 * [folderId] = [NO_FOLDER] is the self-heal default for an `appWidgetId` whose
 * config went missing — the renderer paints a neutral placeholder.
 */
@Serializable
data class FolderWidgetConfig(
    val folderId: Long = NO_FOLDER,
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig {
    companion object {
        const val NO_FOLDER: Long = -1L
    }
}
