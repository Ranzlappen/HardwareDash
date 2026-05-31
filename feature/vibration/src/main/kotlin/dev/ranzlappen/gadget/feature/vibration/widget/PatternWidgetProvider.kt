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
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationPlaybackService
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.launch

/**
 * Home-screen pattern widget — tapping plays the configured saved
 * [dev.ranzlappen.gadget.feature.vibration.VibrationPattern] once via
 * [VibrationPlaybackService] (which reads the widget's `patternId` from its
 * persisted [VibrationWidgetConfig]).
 *
 * Mirror of torch's `StrobeWidgetProvider` minus the running-state toggle (a
 * pattern is a one-shot play, not a sustained on/off). Overrides
 * [reconcilePendingConfig] with `claimSolePending` so the chosen pattern
 * applies on the first tap even when the OS pin-success callback never fires.
 */
class PatternWidgetProvider : BaseGadgetWidgetProvider<VibrationWidgetConfig>() {

    override val logTag: String = VibrationPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> = PatternWidgetProvider::class.java

    override fun configStore(context: Context): WidgetConfigStore<VibrationWidgetConfig> =
        entry(context).widgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun defaultConfig(context: Context): VibrationWidgetConfig = VibrationWidgetConfig(
        type = WidgetType.Pattern,
        displayName = context.getString(R.string.vibration_widget_default_name_pattern),
    )

    override suspend fun activeState(context: Context): Boolean = false

    override suspend fun reconcilePendingConfig(context: Context): VibrationWidgetConfig? =
        entry(context).pendingConfigs().claimSolePending { it.type == WidgetType.Pattern }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_PATTERN_TAP) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
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
            Log.w(logTag, "Pattern FGS start refused", e)
        }

        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                handleTapAfterAction(context, appWidgetId, newState = false)
            } catch (t: Throwable) {
                Log.e(logTag, "PatternWidget onReceive failed", t)
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
        RemoteViews(context.packageName, R.layout.widget_pattern).apply {
            val renderer = appearanceRenderer(context)
            renderer.apply(context = context, views = this, appearance = config.appearance, active = active)
            if (config.removed) {
                setInt(WidgetKitR.id.widget_icon, "setImageAlpha", REMOVED_WIDGET_ICON_ALPHA)
                setInt(R.id.widget_pattern_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_pattern_button, null)
                return@apply
            }
            if (pressed) renderer.applyPressedFrame(context, this, config.appearance)
            val rippleOn = config.appearance.tap.enabled &&
                config.appearance.tap.animation == TapAnimation.Ripple
            setInt(
                R.id.widget_pattern_button,
                "setBackgroundResource",
                if (rippleOn) WidgetKitR.drawable.widget_tap_ripple else android.R.color.transparent,
            )
            if (config.appearance.tap.enabled) {
                setOnClickPendingIntent(R.id.widget_pattern_button, tapPendingIntent(context, appWidgetId))
            } else {
                setOnClickPendingIntent(R.id.widget_pattern_button, null)
            }
        }

    private fun entry(context: Context): PatternWidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, PatternWidgetEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PatternWidgetEntryPoint {
        fun widgetRepository(): WidgetConfigStore<VibrationWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
        fun pendingConfigs(): PendingWidgetConfigs<VibrationWidgetConfig>
    }

    companion object {
        const val ACTION_PATTERN_TAP =
            "dev.ranzlappen.gadget.feature.vibration.ACTION_PATTERN_TAP"

        fun tapPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, PatternWidgetProvider::class.java).apply {
                action = ACTION_PATTERN_TAP
                component = ComponentName(context, PatternWidgetProvider::class.java)
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
