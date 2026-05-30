package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseGadgetWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.launch

/**
 * Home-screen flashlight widget — 1×1 cell.
 *
 * Each pinned instance has its own [TorchWidgetConfig] entry keyed by
 * `appWidgetId`. The config carries the per-widget appearance
 * (background mode, icon style, tap animation, feedback). The widget
 * provider reads the config in `onUpdate` / `onReceive` to drive
 * both the RemoteViews paint and the optional toast / notification
 * fired on toggle.
 *
 * Click flow:
 *   1. User taps the widget; launcher fires the [PendingIntent]
 *      attached in [buildRemoteViews] with action [ACTION_FLASHLIGHT_TOGGLE]
 *      and the per-widget `appWidgetId` as an extra.
 *   2. [onReceive] catches the action, runs `controller.toggle()`
 *      synchronously on the receiver thread, then delegates to
 *      [BaseGadgetWidgetProvider.handleTapAfterAction] for feedback +
 *      repaint + press-frame.
 *
 * **Lifecycle skeleton inherited** from [BaseGadgetWidgetProvider]:
 *  - `onUpdate` → `renderAll` with the self-heal fallback to
 *    [defaultConfig].
 *  - `onDeleted` → per-id `WidgetConfigStore.delete`.
 */
class FlashlightWidgetProvider : BaseGadgetWidgetProvider<TorchWidgetConfig>() {

    override val logTag: String = TorchPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> =
        FlashlightWidgetProvider::class.java

    override fun configStore(context: Context): WidgetConfigStore<TorchWidgetConfig> =
        entry(context).widgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun defaultConfig(context: Context): TorchWidgetConfig = TorchWidgetConfig(
        type = WidgetType.Flashlight,
        displayName = context.getString(R.string.torch_widget_default_name_flashlight),
    )

    override suspend fun activeState(context: Context): Boolean =
        entry(context).torchController().state.value.isOn

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_FLASHLIGHT_TOGGLE) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Toggle immediately on the receiver thread — the TorchCallback
        // synchronously updates the shared TorchState so the torch reacts
        // without waiting on the config read in handleTapAfterAction.
        val controller = entry(context).torchController()
        controller.toggle()
        val newState = controller.state.value.isOn

        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                handleTapAfterAction(context, appWidgetId, newState)
            } catch (t: Throwable) {
                Log.e(logTag, "FlashlightWidget onReceive failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Build the [RemoteViews] for one widget instance. The
     * [WidgetAppearanceRenderer] does the heavy lifting of background
     * + icon swap; the provider just attaches the click PendingIntent
     * if taps are enabled.
     */
    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: TorchWidgetConfig,
        pressed: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
            val renderer = appearanceRenderer(context)
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = active,
            )
            if (config.removed) {
                // Deleted in-app but still hosted by the launcher: dim it
                // and drop the tap target so it reads as defunct until
                // the user drags it off the home screen.
                setInt(WidgetKitR.id.widget_icon, "setImageAlpha", REMOVED_WIDGET_ICON_ALPHA)
                setInt(R.id.widget_flashlight_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_flashlight_button, null)
                return@apply
            }
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
                // Tap disabled — explicitly clear any previously
                // attached PendingIntent so the widget renders
                // display-only.
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
        fun torchController(): TorchController
        fun widgetRepository(): WidgetConfigStore<TorchWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
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
