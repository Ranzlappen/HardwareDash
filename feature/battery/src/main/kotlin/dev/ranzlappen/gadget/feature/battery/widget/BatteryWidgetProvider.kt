package dev.ranzlappen.gadget.feature.battery.widget

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
import dev.ranzlappen.gadget.feature.battery.R
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

private const val MAX_PERCENT = 100

/**
 * The battery status home-screen widget — a read-only consumer of the kit's
 * content/display archetype ([BaseContentWidgetProvider]). Paints the live
 * level %, a progress bar, and a charging line, and opens the app on tap.
 *
 * Display-only: no per-instance binding, configure activity, or pin flow. It is
 * added straight from the launcher's widget picker and self-heals to
 * [defaultConfig] via the base's `saveIfAbsent`. Repaints reactively through
 * [BatteryWidgetController] → `ContentWidgetUpdater` as the battery changes.
 */
class BatteryWidgetProvider : BaseContentWidgetProvider<BatteryWidgetConfig>() {

    override val logTag: String = "BatteryWidget"

    override fun configStore(context: Context): WidgetConfigStore<BatteryWidgetConfig> =
        entryPoint(context).batteryWidgetConfigStore()

    override fun sizePresetOf(config: BatteryWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): BatteryWidgetConfig = BatteryWidgetConfig()

    /** Tap opens the app (its launcher entry); null on the rare device without one. */
    override fun launchIntent(context: Context, appWidgetId: Int, config: BatteryWidgetConfig): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)

    override suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: BatteryWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews {
        val ep = entryPoint(context)
        val battery = ep.batteryMonitor().state.value
        val views = RemoteViews(context.packageName, R.layout.widget_battery)

        views.setTextViewText(
            R.id.battery_widget_percent,
            if (battery.level >= 0) {
                context.getString(R.string.battery_widget_percent_format, battery.level)
            } else {
                context.getString(R.string.battery_widget_unknown)
            },
        )
        views.setProgressBar(
            R.id.battery_widget_progress,
            MAX_PERCENT,
            battery.level.coerceIn(0, MAX_PERCENT),
            false,
        )

        // Charging line hidden at the most compact density to keep the % legible.
        views.setTextViewText(
            R.id.battery_widget_status,
            context.getString(
                if (battery.isCharging) {
                    R.string.battery_widget_charging
                } else {
                    R.string.battery_widget_discharging
                },
            ),
        )
        views.setViewVisibility(
            R.id.battery_widget_status,
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
            R.id.widget_battery_root,
            tapPendingIntent(context, appWidgetId, config),
        )
        return views
    }

    private fun entryPoint(context: Context): BatteryWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BatteryWidgetEntryPoint::class.java,
        )

    companion object {
        val PROVIDER_CLASS: Class<out AppWidgetProvider> = BatteryWidgetProvider::class.java
    }
}
