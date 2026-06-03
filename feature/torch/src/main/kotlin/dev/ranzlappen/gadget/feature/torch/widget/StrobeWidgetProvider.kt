package dev.ranzlappen.gadget.feature.torch.widget

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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.monitor.TorchBootRearmHandler
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR

/**
 * **Legacy-only** torch widget provider — kept registered solely so
 * already-placed strobe widgets from before the function-driven migration stay
 * alive and tappable. **No new widget pins here**; the designated new-pin
 * provider is [FlashlightWidgetProvider] (one generic provider now serves every
 * function), so [reconcilePendingConfig] returns `null`.
 *
 * It is the same generic [BaseGadgetWidgetProvider] subclass as
 * [FlashlightWidgetProvider] — a tap resolves the placed config's bound
 * [WidgetFunction] (strobe / morse, for these legacy instances) from
 * [TorchWidgetFunctionCatalog] and dispatches it through the kit's
 * [WidgetFunctionDispatcher]. The dispatch's strobe-start runs inside the
 * broadcast's FGS-allowlist window (the base launches it from `goAsync`), so
 * no bespoke synchronous service-start path is needed any more.
 *
 * **Lifecycle skeleton inherited** from [BaseGadgetWidgetProvider] — see
 * [FlashlightWidgetProvider] for the canonical write-up.
 */
class StrobeWidgetProvider : BaseGadgetWidgetProvider<TorchWidgetConfig>() {

    override val logTag: String = TorchPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> =
        StrobeWidgetProvider::class.java

    override val featureId: String = TorchBootRearmHandler.FEATURE_ID

    override val tapAction: String = ACTION_STROBE_TOGGLE

    override fun configStore(context: Context): WidgetConfigStore<TorchWidgetConfig> =
        entry(context).widgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun functionDispatcher(context: Context): WidgetFunctionDispatcher =
        entry(context).widgetFunctionDispatcher()

    override fun resolveFunction(context: Context, config: TorchWidgetConfig): WidgetFunction? =
        entry(context).widgetFunctionCatalog().functionFor(config.actionKey)

    override fun paramsOf(config: TorchWidgetConfig): Map<String, String> = config.params

    override fun sizePresetOf(config: TorchWidgetConfig): WidgetSizePreset = config.sizePreset

    override fun defaultConfig(context: Context): TorchWidgetConfig = TorchWidgetConfig(
        displayName = context.getString(R.string.torch_widget_default_name_strobe),
        actionKey = TorchWidgetConfig.FUNCTION_STROBE,
    )

    // No new strobe pins land here (FlashlightWidgetProvider is the designated
    // new-pin provider), so there is nothing to reconcile.
    override suspend fun reconcilePendingConfig(context: Context): TorchWidgetConfig? = null

    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: TorchWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_strobe).apply {
            val renderer = appearanceRenderer(context)
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = active,
                featureId = featureId,
            )
            if (config.removed) {
                // Deleted in-app but still hosted by the launcher: dim it
                // and drop the tap target so it reads as defunct until
                // the user drags it off the home screen.
                setInt(WidgetKitR.id.widget_icon, "setImageAlpha", REMOVED_WIDGET_ICON_ALPHA)
                setInt(R.id.widget_strobe_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_strobe_button, null)
                setViewVisibility(WidgetKitR.id.widget_label, View.GONE)
                return@apply
            }
            // Adaptive name label — painted only at the expanded density.
            setTextViewText(WidgetKitR.id.widget_label, config.displayName)
            setViewVisibility(
                WidgetKitR.id.widget_label,
                if (density.showLabel) View.VISIBLE else View.GONE,
            )
            if (pressed) renderer.applyPressedFrame(context, this, config.appearance)
            // Ripple is the launcher's stock press effect — set it as the
            // click target's background only when selected, transparent
            // otherwise (set every render so a recycled view reverts).
            val rippleOn = config.appearance.tap.enabled &&
                config.appearance.tap.animation == TapAnimation.Ripple
            setInt(
                R.id.widget_strobe_button,
                "setBackgroundResource",
                if (rippleOn) WidgetKitR.drawable.widget_tap_ripple else android.R.color.transparent,
            )
            if (config.appearance.tap.enabled) {
                setOnClickPendingIntent(
                    R.id.widget_strobe_button,
                    togglePendingIntent(context, appWidgetId),
                )
            } else {
                setOnClickPendingIntent(R.id.widget_strobe_button, null)
            }
        }

    private fun entry(context: Context): StrobeWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            StrobeWidgetEntryPoint::class.java,
        )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StrobeWidgetEntryPoint {
        fun widgetRepository(): WidgetConfigStore<TorchWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
        fun widgetFunctionDispatcher(): WidgetFunctionDispatcher
        fun widgetFunctionCatalog(): TorchWidgetFunctionCatalog
        fun pendingConfigs(): PendingWidgetConfigs<TorchWidgetConfig>
    }

    companion object {
        const val ACTION_STROBE_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_STROBE_TOGGLE"

        /**
         * Build the per-widget toggle PendingIntent. Each
         * `appWidgetId` gets a distinct PendingIntent (the
         * `requestCode` is the ID itself) so the OS doesn't
         * coalesce taps from multiple instances into a single
         * intent.
         */
        fun togglePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, StrobeWidgetProvider::class.java).apply {
                action = ACTION_STROBE_TOGGLE
                component = ComponentName(context, StrobeWidgetProvider::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                /* requestCode = */ appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
