package dev.ranzlappen.gadget.core.widgetkit.boot

import android.content.Context

/**
 * Per-feature boot-rearm hook fired by [BootCompletedReceiver] once the
 * device finishes booting. Features bind one of these into a
 * `Map<String, BootRearmHandler>` multibinding keyed by a stable
 * feature id (typically the feature's logcat tag) so the kit can
 * iterate them generically.
 *
 * Typical work: re-arm a foreground service that powers a placed
 * widget the launcher has reinstantiated, or restore any other
 * "this widget is on the home screen so the worker process needs to
 * be alive" invariant. The kit doesn't know what's been pinned;
 * each feature owns the gate (e.g. "if any monitor widget is placed,
 * start the monitor service").
 *
 * **Cost.** Boot broadcasts are user-perceived latency on the device's
 * first unlock. Implementations should:
 *  - Short-circuit fast when no work is needed (no placed widgets,
 *    no enabled metrics, …) — most boots will have nothing to do.
 *  - Avoid synchronous DataStore reads in [onBootCompleted] — the
 *    receiver runs the call inside its own `goAsync` coroutine so
 *    `suspend` is safe.
 */
fun interface BootRearmHandler {
    /**
     * Called from a `goAsync` coroutine in [BootCompletedReceiver]
     * after [android.content.Intent.ACTION_BOOT_COMPLETED] lands. Run
     * the feature's "is this widget alive again?" rearm logic here.
     */
    suspend fun onBootCompleted(context: Context)
}
