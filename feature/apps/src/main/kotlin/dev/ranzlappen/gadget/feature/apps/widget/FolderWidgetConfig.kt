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
    /** Whether the folder-name strip paints (when the widget is large enough);
     *  user-toggled in the customizer. */
    val showLabel: Boolean = true,
    /** Cover symbol + name tint. [FOLLOW_FOLDER_COLOR] (the default) follows the
     *  folder's own `baseColorArgb`; any other ARGB overrides it. New field, so
     *  older on-disk configs decode with the follow default. */
    val coverTintArgb: Long = FOLLOW_FOLDER_COLOR,
    /** Optional icon key from the icon catalog. `null` means use the default folder cover. */
    val iconKey: String? = null,
    override val displayName: String = "",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 2,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig {
    companion object {
        const val NO_FOLDER: Long = -1L

        /** Sentinel for [coverTintArgb]: follow the folder's `baseColorArgb`
         *  rather than a fixed override. `0L` is fully-transparent black —
         *  never a meaningful cover tint — so it's safe as the "follow" marker. */
        const val FOLLOW_FOLDER_COLOR: Long = 0L
    }
}
