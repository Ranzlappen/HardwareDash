package dev.ranzlappen.gadget.feature.vibration.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionDispatcher
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseGadgetWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.provider.WidgetRenderDensity
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.monitor.VibrationBootRearmHandler
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

/**
 * Home-screen vibrate widget — the **designated new-pin** generic provider.
 * Tapping resolves the config's bound [WidgetFunction] (a momentary buzz or,
 * if the user picked it, a saved-pattern play) and dispatches it through the
 * kit's [WidgetFunctionDispatcher] → [dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler],
 * which folds the commanded amplitude into the monitored signal. The provider
 * never starts a hardware service itself — the base owns the
 * tap → dispatch → feedback → repaint chain.
 *
 * Inherits the [BaseGadgetWidgetProvider] lifecycle (onUpdate → renderAll
 * self-heal, onDeleted purge, adaptive density on resize). Overrides
 * [reconcilePendingConfig] with `claimSolePending` so a freshly-pinned widget
 * reliably applies its config on the first tap even when the OS pin-success
 * callback never fires (the first-pin reliability fix). Since this is the only
 * provider used for new pins, the predicate matches any pending entry.
 */
class VibrateWidgetProvider : BaseGadgetWidgetProvider<VibrationWidgetConfig>() {

    override val logTag: String = VibrationPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> = VibrateWidgetProvider::class.java

    override val featureId: String = VibrationBootRearmHandler.FEATURE_ID

    override val tapAction: String = ACTION_VIBRATE_TAP

    override fun configStore(context: Context): WidgetConfigStore<VibrationWidgetConfig> =
        entry(context).vibrationWidgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun functionDispatcher(context: Context): WidgetFunctionDispatcher =
        entry(context).widgetFunctionDispatcher()

    override fun resolveFunction(context: Context, config: VibrationWidgetConfig): WidgetFunction? =
        entry(context).vibrationWidgetFunctionCatalog().functionFor(config.actionKey)

    override fun paramsOf(config: VibrationWidgetConfig): Map<String, String> = config.params

    override fun sizePresetOf(config: VibrationWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): VibrationWidgetConfig = VibrationWidgetConfig(
        displayName = context.getString(R.string.vibration_widget_default_name_vibrate),
        actionKey = VibrationWidgetConfig.FUNCTION_ONESHOT,
    )

    override suspend fun reconcilePendingConfig(context: Context): VibrationWidgetConfig? =
        entry(context).vibrationPendingConfigs().claimSolePending { true }

    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: VibrationWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_vibrate).apply {
            val renderer = appearanceRenderer(context)
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = active,
                featureId = featureId,
            )
            setTextViewText(WidgetKitR.id.widget_label, config.displayName)
            setViewVisibility(
                WidgetKitR.id.widget_label,
                if (density.showLabel && !config.removed) View.VISIBLE else View.GONE,
            )
            if (config.removed) {
                setInt(WidgetKitR.id.widget_icon, "setImageAlpha", REMOVED_WIDGET_ICON_ALPHA)
                setInt(R.id.widget_vibrate_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_vibrate_button, null)
                return@apply
            }
            if (pressed) renderer.applyPressedFrame(context, this, config.appearance)
            val rippleOn = config.appearance.tap.enabled &&
                config.appearance.tap.animation == TapAnimation.Ripple
            setInt(
                R.id.widget_vibrate_button,
                "setBackgroundResource",
                if (rippleOn) WidgetKitR.drawable.widget_tap_ripple else android.R.color.transparent,
            )
            if (config.appearance.tap.enabled) {
                setOnClickPendingIntent(R.id.widget_vibrate_button, tapPendingIntent(context, appWidgetId))
            } else {
                setOnClickPendingIntent(R.id.widget_vibrate_button, null)
            }
        }

    private fun entry(context: Context): VibrateWidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, VibrateWidgetEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface VibrateWidgetEntryPoint {
        fun vibrationWidgetRepository(): WidgetConfigStore<VibrationWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
        fun widgetFunctionDispatcher(): WidgetFunctionDispatcher
        fun vibrationWidgetFunctionCatalog(): VibrationWidgetFunctionCatalog
        fun vibrationPendingConfigs(): PendingWidgetConfigs<VibrationWidgetConfig>
    }

    companion object {
        const val ACTION_VIBRATE_TAP =
            "dev.ranzlappen.gadget.feature.vibration.ACTION_VIBRATE_TAP"

        fun tapPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, VibrateWidgetProvider::class.java).apply {
                action = ACTION_VIBRATE_TAP
                component = ComponentName(context, VibrateWidgetProvider::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
