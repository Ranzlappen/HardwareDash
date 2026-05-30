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
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.render.hasPressFrame
import dev.ranzlappen.gadget.core.widgetkit.render.playTapPressFrame
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                renderAll(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Render every [appWidgetIds] instance from its persisted config.
     * Reads via the DataStore-backed [TorchWidgetConfigRepository.getAll]
     * (not the hot `all.value` cache) so a cold process — empty cache —
     * still paints the saved appearance instead of self-healing a default
     * over it. Self-heal only fires when the config is genuinely absent.
     */
    private suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val ep = entry(context)
        val isOn = ep.torchController().state.value.isOn
        val repo = ep.widgetRepository()
        val configs = repo.getAll()
        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                val default = TorchWidgetConfig(
                    type = WidgetType.Flashlight,
                    displayName = context.getString(R.string.torch_widget_default_name_flashlight),
                )
                Log.w(PendingTorchWidgetConfigs.TAG, "FlashlightWidget self-heal id=$id")
                // saveIfAbsent (not save) so a concurrent pin-success
                // write of the real config is never clobbered by this
                // default. See TorchWidgetConfigRepository.saveIfAbsent.
                repo.saveIfAbsent(id, default)
                default
            }
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id, isOn, config))
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
        // Toggle immediately on the receiver thread — the TorchCallback
        // synchronously updates the shared TorchState so the torch reacts
        // without waiting on the config read below.
        val controller = ep.torchController()
        controller.toggle()
        val newState = controller.state.value.isOn

        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                // DataStore-backed read so a cold process still sees the
                // per-widget feedback + animation config (the hot cache is
                // empty until its first async emission).
                val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ep.widgetRepository().get(appWidgetId)
                } else {
                    null
                }
                Log.d(
                    PendingTorchWidgetConfigs.TAG,
                    "FlashlightWidget tap id=$appWidgetId on=$newState config=${config != null} " +
                        "fb=${config?.appearance?.feedback?.let { it::class.simpleName }} " +
                        "anim=${config?.appearance?.tap?.animation}",
                )
                if (config != null) {
                    // Toast needs a Looper — dispatch on the main thread.
                    withContext(Dispatchers.Main) {
                        ep.feedbackDispatcher().dispatch(
                            displayName = config.displayName,
                            newState = newState,
                            feedback = config.appearance.feedback,
                        )
                    }
                }

                // Repaint every instance (resting state).
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, FlashlightWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids.isNotEmpty()) renderAll(context, appWidgetManager, ids)

                // Overlay the held tap-press frame on the tapped instance.
                if (config != null &&
                    appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                    config.appearance.tap.enabled &&
                    config.appearance.tap.animation.hasPressFrame()
                ) {
                    playTapPressFrame(
                        manager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        pressedViews = buildRemoteViews(context, appWidgetId, newState, config, pressed = true),
                        restingViews = buildRemoteViews(context, appWidgetId, newState, config, pressed = false),
                    )
                }
            } catch (t: Throwable) {
                Log.e(PendingTorchWidgetConfigs.TAG, "FlashlightWidget onReceive failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = entry(context).widgetRepository()
        WidgetReceiverScope.scope.launch {
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
