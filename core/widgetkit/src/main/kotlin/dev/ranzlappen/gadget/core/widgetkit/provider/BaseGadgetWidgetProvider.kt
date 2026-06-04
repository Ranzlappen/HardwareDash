package dev.ranzlappen.gadget.core.widgetkit.provider

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackState
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetDispatchOutcome
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunction
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionBehavior
import dev.ranzlappen.gadget.core.widgetkit.function.WidgetFunctionDispatcher
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.render.hasPressFrame
import dev.ranzlappen.gadget.core.widgetkit.render.playTapPressFrame
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reusable [AppWidgetProvider] base for the kit-built **function-driven**
 * per-instance widget pattern. A single generic provider per feature serves
 * every function the user can pick: the tap resolves the config's bound
 * [WidgetFunction] and dispatches it through [WidgetFunctionDispatcher], so the
 * provider never hardcodes a hardware action.
 *
 * The base owns:
 *  - **[onReceive]** — feature pre-hook ([onBeforeReceive]) + tap-action filter
 *    + [dispatchTap].
 *  - **[onUpdate]** / **[onDeleted]** / **[onAppWidgetOptionsChanged]** — the
 *    lifecycle skeletons (render-all, purge, adaptive re-render on resize).
 *  - **[dispatchTap]** — resolve the function, dispatch it, then run the
 *    post-tap chain ([handleTapAfterAction]): feedback + repaint + press-frame.
 *  - **active-state + density** — computed generically from the function's
 *    [WidgetStateSource] and the launcher-reported size, so the feature only
 *    paints its own layout.
 *
 * The feature subclass owns:
 *  - the Hilt EntryPoint shape + the kit-instance accessors below,
 *  - [resolveFunction] (look up its `WidgetFunctionCatalog` by the config's
 *    action key) + [paramsOf] + [sizePresetOf],
 *  - [buildRemoteViews] for its own layout file + the [tapAction] string,
 *  - [defaultConfig] (the self-heal fallback).
 *
 * **Monitor / chart providers don't fit this pattern.** They read a shared
 * metric config, not a per-`appWidgetId` [WidgetKitConfig]; they stay as
 * standalone `AppWidgetProvider`s.
 */
abstract class BaseGadgetWidgetProvider<T : WidgetKitConfig> : AppWidgetProvider() {

    /** Logcat tag for this provider's lifecycle traces. */
    protected abstract val logTag: String

    /** The concrete provider's class — used to enumerate placed instances. */
    protected abstract val providerClass: Class<out AppWidgetProvider>

    /** Stable feature id selecting this feature's kit multibindings
     *  (icon resolver / feedback config / state sources). */
    protected abstract val featureId: String

    /** The custom broadcast action a tap on this provider's widgets fires
     *  (declared in the manifest `<intent-filter>`). */
    protected abstract val tapAction: String

    /** Hilt-resolved per-feature config store. */
    protected abstract fun configStore(context: Context): WidgetConfigStore<T>

    /** Hilt-resolved kit appearance renderer. */
    protected abstract fun appearanceRenderer(context: Context): WidgetAppearanceRenderer

    /** Hilt-resolved kit feedback dispatcher. */
    protected abstract fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher

    /** Hilt-resolved kit function dispatcher (routes taps to automation). */
    protected abstract fun functionDispatcher(context: Context): WidgetFunctionDispatcher

    /** Resolve the [WidgetFunction] this [config] is bound to (its
     *  `actionKey`) from the feature's `WidgetFunctionCatalog`, or `null` if
     *  the stored key is unknown (a removed/renamed function). */
    protected abstract fun resolveFunction(context: Context, config: T): WidgetFunction?

    /** The action params persisted on this [config], passed to dispatch. */
    protected abstract fun paramsOf(config: T): Map<String, String>

    /** The starting-size hint persisted on this [config], used as the
     *  fallback density before the launcher reports an actual size. */
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

    /** Feature pre-hook run on **every** broadcast before the tap filter —
     *  e.g. arm an external-state observer. Default no-op. */
    protected open fun onBeforeReceive(context: Context, intent: Intent) = Unit

    /** Build the feature-specific [RemoteViews] for one widget instance at the
     *  resolved [density]. Should paint via [WidgetAppearanceRenderer.apply],
     *  attach the tap PendingIntent when `config.appearance.tap.enabled`, show
     *  the name label per [WidgetRenderDensity.showLabel], and optionally call
     *  [WidgetAppearanceRenderer.applyPressedFrame] when `pressed`. */
    protected abstract fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: T,
        density: WidgetRenderDensity,
        pressed: Boolean,
    ): RemoteViews

    final override fun onReceive(context: Context, intent: Intent) {
        onBeforeReceive(context, intent)
        super.onReceive(context, intent)
        if (intent.action != tapAction) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        dispatchTap(context, appWidgetId)
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
        // The launcher reports a new size (placement or user resize) — repaint
        // this one instance so the adaptive density (label visibility) tracks
        // the actual cell footprint.
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
     * Resolve the tapped widget's function and dispatch it through
     * [WidgetFunctionDispatcher], then run [handleTapAfterAction]. Runs in the
     * receiver's `goAsync` window; the dispatch may start a foreground service
     * (strobe / morse) — that start happens inside the broadcast's temporary
     * FGS-allowlist window, which extends across `goAsync`.
     */
    protected fun dispatchTap(context: Context, appWidgetId: Int) {
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                val store = configStore(context)
                val config = (
                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        store.get(appWidgetId)
                    } else {
                        null
                    }
                    ) ?: defaultConfig(context)
                val function = resolveFunction(context, config)
                val outcome = if (function != null && !config.removed) {
                    functionDispatcher(context).dispatch(featureId, function, paramsOf(config))
                } else {
                    WidgetDispatchOutcome(active = false, result = ActionResult.Unsupported)
                }
                handleTapAfterAction(context, appWidgetId, function, outcome)
            } catch (t: Throwable) {
                Log.e(logTag, "widget tap dispatch failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Render every [appWidgetIds] instance from its persisted config. Reads via
     * [WidgetConfigStore.getAll] (not the hot cache) so a cold process paints
     * the saved appearance. A genuinely-absent config is rescued by
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
            val active = activeFor(context, config)
            val density = densityFor(appWidgetManager, id, config)
            appWidgetManager.updateAppWidget(
                id,
                buildRemoteViews(context, id, active, config, density, pressed = false),
            )
        }
    }

    /** The live active state for [config]'s function — the toggle's
     *  [WidgetStateSource] reading, or `false` for a momentary function. */
    protected fun activeFor(context: Context, config: T): Boolean {
        val function = resolveFunction(context, config) ?: return false
        return functionDispatcher(context).isActive(featureId, function)
    }

    /**
     * Post-dispatch chain: confirmation feedback (toast / notification on the
     * main thread), repaint every instance, then overlay the held tap-press
     * frame on the tapped instance.
     */
    protected suspend fun handleTapAfterAction(
        context: Context,
        appWidgetId: Int,
        function: WidgetFunction?,
        outcome: WidgetDispatchOutcome,
    ) {
        val store = configStore(context)
        val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            store.get(appWidgetId)
        } else {
            null
        }
        Log.d(
            logTag,
            "tap id=$appWidgetId active=${outcome.active} result=${outcome.result::class.simpleName} " +
                "config=${config != null}",
        )

        if (config != null) {
            val feedbackState = feedbackStateFor(function, outcome)
            withContext(Dispatchers.Main) {
                feedbackDispatcher(context).dispatch(
                    displayName = config.displayName,
                    state = feedbackState,
                    feedback = config.appearance.feedback,
                    featureId = featureId,
                )
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, providerClass)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) renderAll(context, appWidgetManager, ids)

        if (config != null &&
            appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
            config.appearance.tap.enabled &&
            config.appearance.tap.animation.hasPressFrame()
        ) {
            val density = densityFor(appWidgetManager, appWidgetId, config)
            playTapPressFrame(
                manager = appWidgetManager,
                appWidgetId = appWidgetId,
                pressedViews = buildRemoteViews(context, appWidgetId, outcome.active, config, density, pressed = true),
                restingViews = buildRemoteViews(context, appWidgetId, outcome.active, config, density, pressed = false),
            )
        }
    }

    private fun feedbackStateFor(function: WidgetFunction?, outcome: WidgetDispatchOutcome): WidgetFeedbackState =
        when (val result = outcome.result) {
            is ActionResult.Failure -> WidgetFeedbackState.Failed(result.reason)
            ActionResult.Unsupported -> WidgetFeedbackState.Failed("unavailable")
            ActionResult.Success ->
                if (function?.behavior is WidgetFunctionBehavior.Toggle) {
                    WidgetFeedbackState.Toggle(outcome.active)
                } else {
                    WidgetFeedbackState.Triggered
                }
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

        /** Below this reported size (dp ≈ 1 cell) the paint is the compact
         *  icon-only frame. */
        private const val COMPACT_MAX_DP = 72
    }
}
