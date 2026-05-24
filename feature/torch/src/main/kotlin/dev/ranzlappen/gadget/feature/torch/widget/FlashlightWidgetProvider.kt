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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.widget.customization.TapAnimation
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.feature.torch.widget.feedback.WidgetFeedbackDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Home-screen flashlight widget — 1×1 cell.
 *
 * Each pinned instance has its own [TorchWidgetConfig] entry keyed by
 * `appWidgetId`. The config carries the per-widget appearance
 * (background mode, icon style, tap animation, feedback). The widget
 * provider reads the config in `onUpdate` and `onReceive` to drive
 * both the RemoteViews paint and the optional toast / notification
 * fired on toggle.
 *
 * Click flow:
 *   1. User taps the widget; launcher fires the [PendingIntent]
 *      attached in [onUpdate] with action [ACTION_FLASHLIGHT_TOGGLE]
 *      and the per-widget `appWidgetId` as an extra.
 *   2. [onReceive] catches the action, runs `controller.toggle()`,
 *      then dispatches the widget's configured feedback variant.
 *   3. We then schedule an immediate self-update so the icon flips.
 *
 * **Self-heal:** if `onUpdate` runs for an `appWidgetId` with no
 * saved config (a race between the launcher's pin completion and
 * our pin-success receiver, or a config evicted by an over-eager
 * cleanup), the provider persists a default config keyed by that
 * ID so the in-app widget list still surfaces the widget and the
 * user can configure it without re-pinning.
 *
 * [onDeleted] purges the config so dragging the widget off the home
 * screen doesn't leak a record into the repository.
 */
class FlashlightWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val ep = entry(context)
        val controller = ep.torchController()
        val isOn = controller.state.value.isOn
        val repo = ep.widgetRepository()
        val configs = repo.all.value

        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                // Self-heal: persist a default Flashlight config so
                // the in-app list shows this widget on next refresh.
                // Fire-and-forget — the save is async; the render
                // below uses the in-memory default in the meantime.
                val default = TorchWidgetConfig(
                    type = WidgetType.Flashlight,
                    displayName = context.getString(R.string.torch_widget_default_name_flashlight),
                )
                Log.w(PendingTorchWidgetConfigs.TAG, "FlashlightWidget self-heal id=$id")
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    repo.save(id, default)
                }
                default
            }
            appWidgetManager.updateAppWidget(
                id,
                buildRemoteViews(context, id, isOn, config),
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_FLASHLIGHT_TOGGLE) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val ep = entry(context)
        val controller = ep.torchController()
        // Toggle on the singleton — the TorchCallback synchronously
        // updates the shared TorchState so other surfaces react.
        controller.toggle()
        val newState = controller.state.value.isOn

        // Dispatch the per-widget feedback (toast / notification).
        // Look up config by appWidgetId; if missing fall back silently.
        val config = ep.widgetRepository().all.value[appWidgetId]
        if (config != null) {
            ep.feedbackDispatcher().dispatch(
                displayName = config.displayName,
                newState = newState,
                feedback = config.appearance.feedback,
            )
        }

        // Refresh every flashlight widget instance so the icon flips.
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, FlashlightWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) onUpdate(context, appWidgetManager, ids)

        // Overlay the held tap-press frame on the tapped instance (the
        // refresh above already painted its resting state).
        if (config != null &&
            appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
            config.appearance.tap.enabled &&
            config.appearance.tap.animation.hasPressFrame()
        ) {
            playTapPressFrame(
                context = context,
                appWidgetId = appWidgetId,
                pressedViews = buildRemoteViews(context, appWidgetId, newState, config, pressed = true),
                restingViews = buildRemoteViews(context, appWidgetId, newState, config, pressed = false),
            )
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = entry(context).widgetRepository()
        val purgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        purgeScope.launch {
            appWidgetIds.forEach { repo.delete(it) }
        }
    }

    /**
     * Build the [RemoteViews] for one widget instance. The
     * [WidgetAppearanceRenderer] does the heavy lifting of background
     * + icon swap; the provider just attaches the click PendingIntent
     * if taps are enabled.
     */
    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        torchOn: Boolean,
        config: TorchWidgetConfig,
        pressed: Boolean = false,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_flashlight).apply {
            val renderer = entry(context).appearanceRenderer()
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = torchOn,
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
                if (rippleOn) R.drawable.widget_tap_ripple else android.R.color.transparent,
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
        fun widgetRepository(): TorchWidgetConfigRepository
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
