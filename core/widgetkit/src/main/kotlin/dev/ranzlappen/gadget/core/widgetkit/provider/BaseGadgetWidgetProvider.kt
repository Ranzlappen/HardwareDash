package dev.ranzlappen.gadget.core.widgetkit.provider

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.render.hasPressFrame
import dev.ranzlappen.gadget.core.widgetkit.render.playTapPressFrame
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable [AppWidgetProvider] base for the kit-built per-instance
 * widget pattern. Lifts the four lifecycle skeletons that
 * `FlashlightWidgetProvider` and `StrobeWidgetProvider` had ~80%
 * identical:
 *  - **[onUpdate]** — `goAsync` + receiver scope + [renderAll].
 *  - **[onDeleted]** — `goAsync` + per-id `configStore.delete`.
 *  - **[renderAll]** — load configs, self-heal via [defaultConfig] +
 *    `saveIfAbsent`, paint each instance with [buildRemoteViews].
 *  - **[handleTapAfterAction]** — the common post-action chain a
 *    feature's `onReceive` runs **after** its feature-specific
 *    synchronous action: read the tapped widget's config, dispatch the
 *    configured [WidgetFeedbackDispatcher] feedback, repaint every
 *    instance (resting state), and play the held tap-press frame on
 *    the tapped instance.
 *
 * The feature subclass only owns:
 *  - the Hilt EntryPoint shape (each feature returns differently-typed
 *    stores / catalogues, so the entry-point interface itself is per-
 *    feature; the base reaches the kit instances via the abstract
 *    accessors below),
 *  - the feature-specific synchronous part of [onReceive] (e.g. tap →
 *    `torchController.toggle()`),
 *  - [buildRemoteViews] for its own layout file,
 *  - [activeState] (e.g. `torchController.state.value.isOn`),
 *  - [defaultConfig] (the value `renderAll` falls back to on self-heal).
 *
 * **Monitor / chart providers don't fit this pattern.** They read a
 * shared metric config, not a per-`appWidgetId` [WidgetKitConfig]; they
 * stay as standalone `AppWidgetProvider`s.
 */
abstract class BaseGadgetWidgetProvider<T : WidgetKitConfig> : AppWidgetProvider() {

    /** Logcat tag for this provider's lifecycle traces. Threaded through
     *  [PendingWidgetConfigs] and [BaseWidgetPinSuccessReceiver] as the
     *  same per-feature tag so the whole flow is filterable by one
     *  string (`adb logcat -s <tag>:D`). */
    protected abstract val logTag: String

    /** The concrete provider's class — used by [handleTapAfterAction]
     *  to enumerate currently-placed instances via
     *  [AppWidgetManager.getAppWidgetIds]. */
    protected abstract val providerClass: Class<out AppWidgetProvider>

    /** Hilt-resolved per-feature config store. Implementations typically
     *  delegate to `EntryPointAccessors.fromApplication(...)`. */
    protected abstract fun configStore(context: Context): WidgetConfigStore<T>

    /** Hilt-resolved kit appearance renderer. */
    protected abstract fun appearanceRenderer(context: Context): WidgetAppearanceRenderer

    /** Hilt-resolved kit feedback dispatcher (carries the feature's
     *  channel + small-icon config via its constructor). */
    protected abstract fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher

    /** The default config used for self-healing missing entries — a
     *  race-safe fallback the user can tweak in the in-app list once
     *  the real config catches up. */
    protected abstract fun defaultConfig(context: Context): T

    /** Current active state used to drive the icon-style active/inactive
     *  swap. Examples: torchController.state.value.isOn (flashlight),
     *  strobeRuntime.running.value (strobe). */
    protected abstract suspend fun activeState(context: Context): Boolean

    /** Build the feature-specific [RemoteViews] for one widget instance.
     *  Should call [WidgetAppearanceRenderer.apply] for the background /
     *  icon paint, attach the tap PendingIntent when
     *  `config.appearance.tap.enabled`, and optionally call
     *  [WidgetAppearanceRenderer.applyPressedFrame] when `pressed`. */
    protected abstract fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: T,
        pressed: Boolean,
    ): RemoteViews

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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = configStore(context)
        WidgetReceiverScope.scope.launch {
            appWidgetIds.forEach { id ->
                store.delete(id)
                Log.d(logTag, "onDeleted purged id=$id")
            }
        }
    }

    /**
     * Render every [appWidgetIds] instance from its persisted config.
     * Reads via [WidgetConfigStore.getAll] (not the hot `all.value`
     * cache) so a cold process — empty cache — still paints the saved
     * appearance instead of self-healing a default over it. Self-heal
     * only fires when the config is genuinely absent.
     */
    protected suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val store = configStore(context)
        val configs = store.getAll()
        val active = activeState(context)
        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                val default = defaultConfig(context)
                Log.w(logTag, "self-heal id=$id")
                // saveIfAbsent (not save) so a concurrent pin-success
                // write of the real config is never clobbered by this
                // default. See WidgetConfigStore.saveIfAbsent.
                store.saveIfAbsent(id, default)
                default
            }
            appWidgetManager.updateAppWidget(
                id,
                buildRemoteViews(context, id, active, config, pressed = false),
            )
        }
    }

    /**
     * Common post-action chain a feature's `onReceive` runs **after**
     * its synchronous feature action (`controller.toggle()`,
     * `service.start()`, etc.) completes:
     *
     *  1. Load the tapped widget's config (DataStore-backed read, not
     *     the hot cache — so a cold process still sees per-widget
     *     feedback + animation config).
     *  2. Dispatch the configured [WidgetFeedbackDispatcher] feedback on
     *     the main thread (Toast needs a Looper).
     *  3. Repaint every instance (resting state) so the icon swap
     *     reflects the new global state.
     *  4. Overlay the held tap-press frame on the tapped instance for
     *     ~280 ms via [playTapPressFrame] (no-op for None / Ripple
     *     animations).
     *
     * Call from inside the feature's own `goAsync` coroutine — the base
     * does not open its own; the caller already owns the
     * `pendingResult.finish()` lifecycle.
     *
     * @param appWidgetId the id of the tapped instance (may be
     *                    [AppWidgetManager.INVALID_APPWIDGET_ID] if the
     *                    extra was missing — feedback / press-frame
     *                    silently no-op in that case).
     * @param newState the post-action active state used by the icon
     *                 paint + the `{state}` template substitution.
     */
    protected suspend fun handleTapAfterAction(
        context: Context,
        appWidgetId: Int,
        newState: Boolean,
    ) {
        val store = configStore(context)
        val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            store.get(appWidgetId)
        } else {
            null
        }
        Log.d(
            logTag,
            "tap id=$appWidgetId state=$newState config=${config != null} " +
                "fb=${config?.appearance?.feedback?.let { it::class.simpleName }} " +
                "anim=${config?.appearance?.tap?.animation}",
        )

        if (config != null) {
            // Toast needs a Looper — dispatch on the main thread.
            withContext(Dispatchers.Main) {
                feedbackDispatcher(context).dispatch(
                    displayName = config.displayName,
                    newState = newState,
                    feedback = config.appearance.feedback,
                )
            }
        }

        // Repaint every instance (resting state).
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, providerClass)
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
    }

    /** Build the broadcast [Intent] used to repaint a single placed
     *  instance via [AppWidgetManager.ACTION_APPWIDGET_UPDATE]. Features
     *  can use this directly or wrap it in a helper. */
    protected fun appWidgetUpdateIntent(context: Context, appWidgetId: Int): Intent =
        Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            context,
            providerClass,
        ).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            component = ComponentName(context, providerClass)
        }
}
