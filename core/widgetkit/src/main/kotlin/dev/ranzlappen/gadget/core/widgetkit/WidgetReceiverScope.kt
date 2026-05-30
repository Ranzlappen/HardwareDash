package dev.ranzlappen.gadget.core.widgetkit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * One process-lifetime [CoroutineScope] shared by every
 * `AppWidgetProvider` / `BroadcastReceiver` for the async work they kick off
 * from `onUpdate` / `onReceive` (paired with `goAsync()`'s
 * `PendingResult.finish()`).
 *
 * Replaces the anti-pattern of allocating a fresh
 * `CoroutineScope(SupervisorJob() + Dispatchers.IO)` on **every** broadcast —
 * those scopes were never cancelled, so the scope object lingered after each
 * tap. A single shared, never-cancelled scope is the correct shape for
 * receiver work: the work items complete on their own (and `goAsync` bounds
 * the broadcast's lifetime), while the scope itself is a process singleton.
 *
 * `SupervisorJob` so one receiver's failure can't cancel sibling work.
 * `Dispatchers.IO` because receiver work is DataStore reads + RemoteViews
 * updates; switch to `Dispatchers.Main` locally (via `withContext`) for the
 * few UI-thread calls (e.g. `Toast`).
 */
object WidgetReceiverScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
