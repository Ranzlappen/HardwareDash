package dev.ranzlappen.gadget.feature.radios.wifi.widget

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.android.EntryPointAccessors
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseContentWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.radios.wifi.R
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

private const val MAX_PERCENT = 100

/**
 * The WiFi-signal home-screen widget — a read-only consumer of the kit's
 * content/display archetype ([BaseContentWidgetProvider]). Paints the signal %
 * (mapped from RSSI), a progress bar, and the SSID / state line, and opens the
 * app on tap.
 *
 * Display-only: no per-instance binding, configure activity, or pin flow. Added
 * straight from the launcher picker and self-healed to [defaultConfig] via the
 * base's `saveIfAbsent`. Repaints reactively through [WifiWidgetController] →
 * `ContentWidgetUpdater` as the connection / signal changes.
 */
class WifiWidgetProvider : BaseContentWidgetProvider<WifiWidgetConfig>() {

    override val logTag: String = "WifiWidget"

    override fun configStore(context: Context): WidgetConfigStore<WifiWidgetConfig> =
        entryPoint(context).wifiWidgetConfigStore()

    override fun sizePresetOf(config: WifiWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): WifiWidgetConfig = WifiWidgetConfig()

    /** Tap opens the app (its launcher entry); null on the rare device without one. */
    override fun launchIntent(context: Context, appWidgetId: Int, config: WifiWidgetConfig): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: WifiWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val state = ep.wifiMonitor().state.value
        val views = RemoteViews(context.packageName, R.layout.widget_wifi)

        val rssi = state.rssiDbm
        val connected = state.enabled && state.connected && rssi != null
        val percent = if (connected) WifiSignal.signalPercent(rssi) else 0

        views.setTextViewText(
            R.id.wifi_widget_percent,
            if (connected) {
                context.getString(R.string.wifi_widget_percent_format, percent)
            } else {
                context.getString(R.string.wifi_widget_unknown)
            },
        )
        views.setProgressBar(R.id.wifi_widget_progress, MAX_PERCENT, percent, false)

        // SSID / state line, hidden at the most compact density.
        views.setTextViewText(
            R.id.wifi_widget_status,
            when {
                !state.enabled -> context.getString(R.string.wifi_widget_off)
                !state.connected -> context.getString(R.string.wifi_widget_disconnected)
                else -> state.ssid ?: context.getString(R.string.wifi_widget_unknown)
            },
        )
        views.setViewVisibility(
            R.id.wifi_widget_status,
            if (density == WidgetRenderDensity.Compact) View.GONE else View.VISIBLE,
        )

        // Name label paints only at the Expanded density (kit convention).
        views.setTextViewText(WidgetKitR.id.widget_label, config.displayName)
        views.setViewVisibility(
            WidgetKitR.id.widget_label,
            if (density.showLabel) View.VISIBLE else View.GONE,
        )

        val renderer = ep.widgetAppearanceRenderer()
        renderer.applyBackground(views, config.appearance)
        if (pressed) {
            renderer.applyContentPressedFrame(context, views, config.appearance)
        }

        views.setOnClickPendingIntent(
            R.id.widget_wifi_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    private fun entryPoint(context: Context): WifiWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WifiWidgetEntryPoint::class.java,
        )

    companion object {
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = WifiWidgetProvider::class.java
    }
}
