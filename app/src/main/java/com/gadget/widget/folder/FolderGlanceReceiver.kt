package com.gadget.widget.folder

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * AppWidget receiver for the Glance flavor of the folder widget. Registered
 * separately in the manifest so the launcher exposes both flavors in its
 * widget tray; both share `apps_widget_config` rows + `FolderWidgetConfigActivity`.
 */
class FolderGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FolderGlanceWidget()
}
