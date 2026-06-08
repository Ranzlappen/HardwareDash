package dev.ranzlappen.gadget.core.widgetkit.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.render.hasPressFrame
import dev.ranzlappen.gadget.core.widgetkit.render.playTapPressFrame
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import kotlinx.coroutines.launch

/**
 * Reusable [AppWidgetProvider] base for the kit's **content / launcher**
 * widget archetype — the second archetype alongside the function-driven
 * [BaseGadgetWidgetProvider].
 *
 * A content widget **renders dynamic content** (a live preview painted from
 * the feature's own data) and, on tap, **launches an Activity** rather than
 * dispatching a [dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction].
 * The App-Organizer folder widget is the reference consumer: it paints a
 * folder cover / app-preview grid and opens the floating folder popup on tap.
 *
 * This base deliberately omits the function / feedback / active-state /
 * toggle machinery of [BaseGadgetWidgetProvider] — a content widget has no
 * on/off state to reflect and no action to confirm.
 *
 * The base owns:
 *  - **[onUpdate]** / **[onDeleted]** / **[onAppWidgetOptionsChanged]** — the
 *    lifecycle skeletons (render-all, purge, adaptive re-render on resize),
 *    all on the shared [WidgetReceiverScope] under `goAsync`.
 *  - **[renderAll]** — read each instance's persisted [WidgetKitConfig] from
 *    the [WidgetConfigStore], self-heal / reconcile a missing one, resolve the
 *    render density, and hand off to [buildRemoteViews].
 *  - **[launchPendingIntent]** — the tap → Activity `PendingIntent` helper the
 *    feature attaches to its root view.
 *
 * The feature subclass owns:
 *  - the Hilt EntryPoint shape + the [configStore] accessor,
 *  - [buildRemoteViews] for its own layout (painting content **and** wiring the
 *    tap via [launchPendingIntent]),
 *  - [launchIntent] (the Activity a tap opens; `null` = non-interactive),
 *  - [sizePresetOf] + [defaultConfig], and optionally [reconcilePendingConfig].
 *
 * Content-source → repaint is driven externally: a feature `@Singleton`
 * observer collects its data flows and calls
 * [ContentWidgetUpdater.requestUpdate] to re-fire [onUpdate] for every placed
 * instance (mirrors the monitoring notifier seam).
 */
abstract class BaseContentWidgetProvider<T : WidgetKitConfig> : AppWidgetProvider() {

    /** Logcat tag for this provider's lifecycle traces. */
    protected abstract val logTag: String

    /** Hilt-resolved per-feature config store. */
    protected abstract fun configStore(context: Context): WidgetConfigStore<T>

    /** Build the feature-specific [RemoteViews] for one widget instance at the
     *  resolved [density]. Paint the content and attach the tap target via
     *  [tapPendingIntent] (typically on the layout's root view). When [pressed]
     *  is true, overlay the held tap-press frame via
     *  [dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer.applyContentPressedFrame]
     *  (only ever true for an animation with [hasPressFrame]). `suspend` because
     *  content widgets load their preview from the feature's data layer (DB +
     *  icon decode) — it runs inside [renderAll]'s receiver coroutine. */
    protected abstract suspend fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        config: T,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews

    /** The Activity intent a tap on this [config]'s widget should open, or
     *  `null` for a non-interactive widget. */
    protected abstract fun launchIntent(context: Context, appWidgetId: Int, config: T): Intent?

    /** The starting-size hint persisted on this [config], used as the fallback
     *  density before the launcher reports an actual size. */
    protected abstract fun sizePresetOf(config: T): WidgetSizePreset

    /** The default config used for self-healing missing entries. */
    protected abstract fun defaultConfig(context: Context): T

    /**
     * Rescue a brand-new `appWidgetId` that has no persisted config yet by
     * consuming the sole unclaimed pending config (see
     * [dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs.claimSolePending]).
     * Default returns `null`; the designated new-pin provider overrides it.
     */
    protected open suspend fun reconcilePendingConfig(context: Context): T? = null

    /**
     * The custom broadcast action a tap fires when a held tap-press frame
     * ([TapAnimation.Flash] / [TapAnimation.Pulse] / [TapAnimation.Scale]) is
     * configured, so this provider can paint the frame. `null` (the default)
     * keeps taps as a direct `getActivity` launch with no broadcast hop — the
     * right choice for a widget that wants no held frame. A feature that opts in
     * overrides this with a unique action string; the tap intent is **explicit**
     * (component-targeted), so no manifest `<intent-filter>` is needed (and none
     * is added, to avoid widening the receiver's exported surface).
     */
    protected open val tapAction: String? = null

    /** Feature pre-hook run on every broadcast before the default handling —
     *  e.g. arm a content observer. Default no-op. */
    protected open fun onBeforeReceive(context: Context, intent: Intent) = Unit

    override fun onReceive(context: Context, intent: Intent) {
        onBeforeReceive(context, intent)
        val action = tapAction
        if (action != null && intent.action == action) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                handleContentTap(context, appWidgetId)
            }
            return
        }
        super.onReceive(context, intent)
    }

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                renderAll(context, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Render every [appWidgetIds] instance from its persisted config. Reads via
     * [WidgetConfigStore.getAll] (not the hot cache) so a cold process paints
     * the saved content. A genuinely-absent config is rescued by
     * [reconcilePendingConfig], else self-healed to [defaultConfig] — both via
     * `saveIfAbsent` so a concurrent authoritative pin-success `save` wins.
     */
    protected suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val store = configStore(context)
        val configs = store.getAll()
        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                val rescued = reconcilePendingConfig(context)
                val fallback = rescued ?: defaultConfig(context)
                if (rescued != null) {
                    Log.d(logTag, "reconcile rescued id=$id")
                } else {
                    Log.w(logTag, "self-heal id=$id")
                }
                store.saveIfAbsent(id, fallback)
                store.getFresh(id) ?: fallback
            }
            val density = densityFor(appWidgetManager, id, config)
            appWidgetManager.updateAppWidget(
                id,
                buildRemoteViews(context, id, config, density, pressed = false),
            )
        }
    }

    /**
     * The tap target for [config]'s widget. When a held press frame is
     * configured **and** the feature set a [tapAction], the tap routes through
     * this provider (a [PendingIntent.getBroadcast] to [handleContentTap]) so
     * the frame can paint; otherwise it's a direct Activity launch
     * ([launchPendingIntent]) with no broadcast hop. Returns `null` when there's
     * nothing to launch ([launchIntent] is `null`), so an unbound placeholder
     * widget stays inert rather than playing a frame that opens nothing.
     */
    protected fun tapPendingIntent(context: Context, appWidgetId: Int, config: T): PendingIntent? {
        val action = tapAction
        if (action == null || !config.appearance.tap.animation.hasPressFrame()) {
            return launchPendingIntent(context, appWidgetId, config)
        }
        if (launchIntent(context, appWidgetId, config) == null) return null
        val intent = Intent(action)
            .setClass(context, javaClass)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Handle a tap routed through [tapAction]: **launch the target Activity
     * first** so the tap feels instant, then play the held press frame
     * concurrently (visible behind a floating launch target). Runs on the
     * shared [WidgetReceiverScope] under `goAsync` — the activity start happens
     * within the widget-click broadcast's privilege window (the same pattern the
     * function base relies on for FGS starts).
     */
    private fun handleContentTap(context: Context, appWidgetId: Int) {
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                val config = configStore(context).get(appWidgetId) ?: return@launch
                launchIntent(context, appWidgetId, config)?.let { launch ->
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(launch) }
                        .onFailure { Log.w(logTag, "content tap launch failed id=$appWidgetId", it) }
                }
                if (config.appearance.tap.animation.hasPressFrame()) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val density = densityFor(appWidgetManager, appWidgetId, config)
                    playTapPressFrame(
                        manager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        pressedViews = buildRemoteViews(context, appWidgetId, config, density, pressed = true),
                        restingViews = buildRemoteViews(context, appWidgetId, config, density, pressed = false),
                    )
                }
            } catch (t: Throwable) {
                Log.e(logTag, "content tap failed id=$appWidgetId", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Build the tap → Activity `PendingIntent` for [config]'s widget, or
     *  `null` when [launchIntent] returns `null`. Keyed by `appWidgetId` so
     *  each instance gets a distinct `PendingIntent`. */
    protected fun launchPendingIntent(context: Context, appWidgetId: Int, config: T): PendingIntent? {
        val intent = launchIntent(context, appWidgetId, config) ?: return null
        return PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Resolve the render density from the launcher-reported size, falling
     *  back to the config's starting-size preset before any size is reported. */
    private fun densityFor(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        config: T,
    ): WidgetRenderDensity {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        return when {
            minHeight <= 0 && minWidth <= 0 ->
                WidgetRenderDensity.fromPreset(sizePresetOf(config))
            minHeight >= LABEL_MIN_HEIGHT_DP -> WidgetRenderDensity.Expanded
            minHeight < COMPACT_MAX_DP && minWidth < COMPACT_MAX_DP -> WidgetRenderDensity.Compact
            else -> WidgetRenderDensity.Regular
        }
    }

    companion object {
        /** Min reported height (dp ≈ 2 cells) at which the name label paints. */
        private const val LABEL_MIN_HEIGHT_DP = 110

        /** Below this reported size (dp ≈ 1 cell) the paint is the compact frame. */
        private const val COMPACT_MAX_DP = 72
    }
}
