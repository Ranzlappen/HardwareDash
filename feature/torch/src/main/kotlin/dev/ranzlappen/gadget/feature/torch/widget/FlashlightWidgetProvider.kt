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
 * The designated generic torch home-screen widget provider — every **new**
 * torch widget pins here. One generic provider serves every function the user
 * can pick (flashlight / strobe / morse); a tap resolves the config's bound
 * [WidgetFunction] from [TorchWidgetFunctionCatalog] and dispatches it through
 * the kit's [WidgetFunctionDispatcher], so routing a widget tap flows through
 * the same `:core:automation` actions (and therefore the same strobe runtime /
 * monitoring) as the in-app controls.
 *
 * Each pinned instance has its own [TorchWidgetConfig] entry keyed by
 * `appWidgetId`, carrying the bound function (`actionKey`), its params, the
 * starting-size preset, and the per-widget appearance.
 *
 * **Tap flow** (owned by [BaseGadgetWidgetProvider]):
 *   1. User taps; the launcher fires the [ACTION_FLASHLIGHT_TOGGLE]
 *      [PendingIntent] attached in [buildRemoteViews] with the per-widget
 *      `appWidgetId`.
 *   2. The base's `onReceive` (final) runs [onBeforeReceive] (arms the
 *      external-state observer), then on the [tapAction] match calls
 *      `dispatchTap` → resolve function → dispatch → feedback → repaint.
 *
 * **Lifecycle skeleton inherited** from [BaseGadgetWidgetProvider]:
 *  - `onUpdate` → `renderAll` with reconcile/self-heal fallback.
 *  - `onAppWidgetOptionsChanged` → adaptive re-render on resize.
 *  - `onDeleted` → per-id `WidgetConfigStore.delete`.
 */
class FlashlightWidgetProvider : BaseGadgetWidgetProvider<TorchWidgetConfig>() {

    override val logTag: String = TorchPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> =
        FlashlightWidgetProvider::class.java

    override val featureId: String = TorchBootRearmHandler.FEATURE_ID

    override val tapAction: String = ACTION_FLASHLIGHT_TOGGLE

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
        displayName = context.getString(R.string.torch_widget_default_name_flashlight),
        actionKey = TorchWidgetConfig.FUNCTION_FLASHLIGHT,
    )

    // Start watching for OS-level torch changes (QS tile / other apps) so a
    // placed widget repaints on external toggles. Idempotent + lazy, and
    // re-arms here after a process restart since this runs on every widget
    // broadcast.
    override fun onBeforeReceive(context: Context, intent: Intent) {
        entry(context).torchWidgetStateObserver().ensureStarted()
    }

    // Rescue a freshly-pinned widget whose OS success callback never landed:
    // pull the sole unclaimed pending config. All new torch pins land on this
    // provider, so the predicate is unconditional — sole-match still guards
    // against two simultaneous pins having their configs swapped. Keeps a
    // first-pin appearance (icon / background / tap feedback) and the picked
    // function reliable on the first render rather than self-healing a blank
    // flashlight default.
    override suspend fun reconcilePendingConfig(context: Context): TorchWidgetConfig? =
        entry(context).pendingConfigs().claimSolePending { true }

    /**
     * Build the [RemoteViews] for one widget instance. The
     * [WidgetAppearanceRenderer] does the heavy lifting of background + icon
     * swap; the provider attaches the click PendingIntent, paints the inert
     * removed-state, and shows the name label at [WidgetRenderDensity.Expanded].
     */
    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: TorchWidgetConfig,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
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
                setInt(R.id.widget_flashlight_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_flashlight_button, null)
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
                R.id.widget_flashlight_button,
                "setBackgroundResource",
                if (rippleOn) WidgetKitR.drawable.widget_tap_ripple else android.R.color.transparent,
            )
            if (config.appearance.tap.enabled) {
                setOnClickPendingIntent(
                    R.id.widget_flashlight_button,
                    togglePendingIntent(context, appWidgetId),
                )
            } else {
                // Tap disabled — explicitly clear any previously attached
                // PendingIntent so the widget renders display-only.
                setOnClickPendingIntent(R.id.widget_flashlight_button, null)
            }
        }

    private fun entry(context: Context): FlashlightWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FlashlightWidgetEntryPoint::class.java,
        )

    /**
     * Hilt entry point — gives a system-instantiated BroadcastReceiver
     * access to the singletons without `@AndroidEntryPoint`.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FlashlightWidgetEntryPoint {
        fun torchWidgetStateObserver(): TorchWidgetStateObserver
        fun widgetRepository(): WidgetConfigStore<TorchWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
        fun widgetFunctionDispatcher(): WidgetFunctionDispatcher
        fun widgetFunctionCatalog(): TorchWidgetFunctionCatalog
        fun pendingConfigs(): PendingWidgetConfigs<TorchWidgetConfig>
    }

    companion object {
        /**
         * Custom broadcast action this receiver listens for. Declared
         * in the AndroidManifest's intent-filter alongside
         * `APPWIDGET_UPDATE` so the OS routes user taps here.
         */
        const val ACTION_FLASHLIGHT_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_FLASHLIGHT_TOGGLE"

        /**
         * Build the toggle [PendingIntent]. Each `appWidgetId` gets
         * a distinct PendingIntent (`requestCode = appWidgetId`) so
         * per-instance feedback dispatch sees the correct ID.
         */
        fun togglePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, FlashlightWidgetProvider::class.java).apply {
                action = ACTION_FLASHLIGHT_TOGGLE
                component = ComponentName(context, FlashlightWidgetProvider::class.java)
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
