package dev.ranzlappen.gadget.core.widgetkit.pin

import android.util.Log
import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Generic persistent bridge for the kit's pin flow.
 *
 * Problem this solves: [android.appwidget.AppWidgetManager.requestPinAppWidget]
 * doesn't return the new `appWidgetId` synchronously. Instead the OS
 * fires the caller-supplied success [android.app.PendingIntent]
 * **after** the user accepts the launcher's pin dialog, with the
 * newly-assigned ID attached as
 * [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID].
 *
 * To carry the user's pre-pin configuration through that round-trip:
 * 1. UI calls [enqueue] before invoking `requestPinAppWidget` —
 *    receives back a stable string token.
 * 2. The success-callback `PendingIntent` carries the token in its
 *    extras (the feature owns the extra key).
 * 3. The kit's [BaseWidgetPinSuccessReceiver] reads the token + the
 *    `appWidgetId`, calls [claim] to pop the config, and saves it
 *    via [dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore]
 *    keyed by `appWidgetId`.
 *
 * **Why persistent (not in-memory)?** Previously the bridge was a
 * `ConcurrentHashMap`. On low-RAM devices the OS sometimes evicts the
 * app process while the launcher pin dialog is on-screen; the OS
 * later fires the success callback into a freshly-spawned process
 * with no in-memory map — token mismatch → silent drop → "the widget
 * got pinned but never appeared in the in-app list". This
 * implementation persists every enqueued entry to a Preferences
 * DataStore so process death is survivable.
 *
 * The persisted entry carries a timestamp; the startup janitor drops
 * entries older than [staleThresholdMs] so cancelled pin dialogs
 * don't accumulate orphaned configs forever.
 *
 * **Monotonic-counter keying (P2-18).** [FeaturePreferences] uses
 * `Int` keys. The previous torch implementation reduced the UUID
 * token to a key via `token.hashCode().absoluteValue` — fine for a
 * small number of concurrent pins, but collision-prone in principle.
 * This implementation keeps a stored `maxKey + 1` counter so every
 * enqueue gets a guaranteed-unique integer key. The counter is
 * computed atomically inside a [Mutex] so two concurrent
 * `enqueue()` calls can't pick the same key.
 *
 * **Per-feature instance.** Each widget-bearing feature provides one
 * `PendingWidgetConfigs<TConfig>` from its Hilt module, passing in a
 * `FeaturePreferences<PendingEntry<TConfig>>` built from
 * `factory.create(...)` with the feature-specific filename + serializer
 * (note the `PendingEntry.serializer(TConfig.serializer())`).
 */
class PendingWidgetConfigs<T : WidgetKitConfig>(
    private val store: FeaturePreferences<PendingEntry<T>>,
    /** Logcat tag for this feature's pin flow — e.g. `"TorchPinFlow"`. */
    val tag: String,
    /** Pending entries older than this are dropped on the next startup
     *  janitor run. Default 1 hour — launcher pin dialogs typically
     *  resolve in seconds. */
    private val staleThresholdMs: Long = DEFAULT_STALE_THRESHOLD_MS,
) {
    /** Process-lifetime scope for the fire-and-forget startup janitor.
     *  Never blocks the injecting thread. */
    private val janitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Serialises [enqueue]s so two callers can't pick the same key
     *  via the read-max-then-write pattern. DataStore's `edit` is
     *  serialised internally, but the counter computation spans two
     *  ops (getAll + save), so we explicitly bracket it. */
    private val enqueueMutex = Mutex()

    init {
        // Best-effort startup janitor — drops pending entries older than
        // the stale threshold. Launched (not runBlocking) so the
        // constructor never blocks on DataStore IO.
        janitorScope.launch {
            runCatching { purgeStale() }
                .onFailure { Log.w(tag, "Stale-pending janitor skipped", it) }
        }
    }

    /**
     * Store a pending [config]; return the token to embed in the
     * success [android.app.PendingIntent]. Suspending because the
     * write hits DataStore.
     */
    suspend fun enqueue(config: T): String {
        val token = UUID.randomUUID().toString()
        enqueueMutex.withLock {
            val key = nextKey()
            val entry = PendingEntry(
                token = token,
                savedAtMs = System.currentTimeMillis(),
                config = config,
            )
            store.save(key, entry)
            Log.d(tag, "enqueue token=$token key=$key")
        }
        return token
    }

    /**
     * Pop the config registered under [token]. Returns `null` if the
     * token is unknown (already claimed, never registered, or persisted
     * entry was purged as stale).
     */
    suspend fun claim(token: String): T? {
        val snapshot = store.getAll()
        val match = snapshot.entries.firstOrNull { it.value.token == token }
        if (match == null) {
            Log.w(tag, "claim token=$token MISS — no pending entry")
            return null
        }
        store.delete(match.key)
        Log.d(tag, "claim token=$token HIT — key=${match.key}")
        return match.value.config
    }

    /**
     * Drop all pending entries older than [staleThresholdMs]. The
     * UI's pin dialog completes in seconds; entries older than an
     * hour (the default cutoff) are almost certainly abandoned
     * cancellations.
     */
    suspend fun purgeStale() {
        val cutoff = System.currentTimeMillis() - staleThresholdMs
        val snapshot = store.getAll()
        val stale = snapshot.filterValues { it.savedAtMs < cutoff }.keys
        if (stale.isEmpty()) return
        Log.d(tag, "purgeStale dropping ${stale.size} expired entries")
        stale.forEach { store.delete(it) }
    }

    /** Monotonic-counter key allocator: `maxExistingKey + 1`, or 1
     *  when the store is empty. Called under [enqueueMutex] so two
     *  concurrent enqueues can't collide. */
    private suspend fun nextKey(): Int {
        val snapshot = store.getAll()
        return (snapshot.keys.maxOrNull() ?: 0) + 1
    }

    companion object {
        /** Default cutoff for the startup janitor (1 hour). */
        const val DEFAULT_STALE_THRESHOLD_MS: Long = 60L * 60L * 1000L
    }
}
