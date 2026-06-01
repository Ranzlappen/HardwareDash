package dev.ranzlappen.gadget.feature.vibration.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseGadgetWidgetProvider
import dev.ranzlappen.gadget.feature.vibration.monitor.VibrationBootRearmHandler
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationPlaybackService
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.launch

/**
 * Home-screen vibrate widget — 1×1 cell. Tapping fires a one-shot buzz at the
 * configured amplitude/duration via [VibrationPlaybackService] (which folds the
 * commanded amplitude into the monitored signal).
 *
 * Lifecycle skeleton inherited from [BaseGadgetWidgetProvider] (onUpdate →
 * renderAll with self-heal, onDeleted purge, the post-tap feedback/animation
 * chain). Overrides [reconcilePendingConfig] with `claimSolePending` so a
 * freshly-pinned widget reliably applies its config on the first tap even when
 * the OS pin-success callback never fires (the first-pin reliability fix).
 */
class VibrateWidgetProvider : BaseGadgetWidgetProvider<VibrationWidgetConfig>() {

    override val logTag: String = VibrationPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> = VibrateWidgetProvider::class.java

    override val featureId: String = VibrationBootRearmHandler.FEATURE_ID

    override fun configStore(context: Context): WidgetConfigStore<VibrationWidgetConfig> =
        entry(context).vibrationWidgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun defaultConfig(context: Context): VibrationWidgetConfig = VibrationWidgetConfig(
        type = WidgetType.Vibrate,
        displayName = context.getString(R.string.vibration_widget_default_name_vibrate),
    )

    // A vibrate widget has no persistent on/off state — each tap is a discrete
    // buzz — so the icon never shows an "active" variant.
    override suspend fun activeState(context: Context): Boolean = false

    override suspend fun reconcilePendingConfig(context: Context): VibrationWidgetConfig? =
        entry(context).vibrationPendingConfigs().claimSolePending { it.type == WidgetType.Vibrate }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_VIBRATE_TAP) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        // Start the playback FGS synchronously in the broadcast context to stay
        // inside the allowed FGS-start window on Android 12+.
        val startIntent = Intent(context, VibrationPlaybackService::class.java).apply {
            putExtra(VibrationPlaybackService.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        } catch (e: IllegalStateException) {
            Log.w(logTag, "Vibrate FGS start refused", e)
        }

        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                handleTapAfterAction(context, appWidgetId, newState = false)
            } catch (t: Throwable) {
                Log.e(logTag, "VibrateWidget onReceive failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: VibrationWidgetConfig,
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
