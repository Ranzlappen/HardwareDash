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
     * Pop the pending config whose payload matches [predicate] **only when it
     * is the sole match** — i.e. exactly one unclaimed entry of that kind is
     * pending. Returns `null` when nothing matches **or when the match is
     * ambiguous** (two+ entries match).
     *
     * This is the recovery path for a pin whose OS success callback never
     * fired (a known flakiness on some OEM launchers) or whose [claim] missed
     * after process death. The provider's self-heal calls this for a
     * brand-new `appWidgetId` that has no persisted config yet, rescuing the
     * user's real config instead of writing a blank default.
     *
     * **Why sole-match, not most-recent.** Without the OS success callback's
     * token we cannot correlate a specific `appWidgetId` to a specific pending
     * entry. When a single widget is pinned (the overwhelmingly common case,
     * and exactly the bug this fixes) there is exactly one matching entry, so
     * the correlation is unambiguous. But if the user pins **two** widgets of
     * the same kind back-to-back on a broken-callback launcher, guessing
     * "most recent" would actively **swap** their configs (widget A shows B's
     * settings and vice-versa). Deferring when ambiguous degrades that rare
     * case to a self-healed default the user can re-edit — strictly better
     * than a confusing cross-assignment.
     *
     * **Idempotent against [claim].** Both delete the popped entry under
     * [enqueueMutex], so whichever path runs second misses (`null`): a pending
     * entry is consumed **at most once**. If the success callback already
     * claimed + saved the config before the provider's `onUpdate` runs, the
     * store already has the entry under its `appWidgetId` and the self-heal
     * branch — hence this method — never runs.
     *
     * The [predicate] lets a feature with multiple widget kinds (torch has
     * Flashlight + Strobe) filter to the matching provider type so a strobe
     * self-heal can't consume a pending flashlight entry. Generic: every
     * feature supplies its own predicate over its `WidgetKitConfig`.
     *
     * Runs under [enqueueMutex] so it can't race the key allocator.
     */
    suspend fun claimSolePending(predicate: (T) -> Boolean): T? = enqueueMutex.withLock {
        val snapshot = store.getAll()
        val match = selectSolePending(snapshot, predicate)
        if (match == null) {
            Log.d(tag, "claimSolePending MISS — no unambiguous matching entry")
            return@withLock null
        }
        store.delete(match.key)
        Log.d(tag, "claimSolePending HIT — key=${match.key}")
        match.value.config
    }

    /**
     * Drop all pending entries older than [staleThresholdMs]. The
     * UI's pin dialog completes in seconds; entries older than an
     * hour (the default cutoff) are almost certainly abandoned
     * cancellations.
     *
     * Runs under [enqueueMutex] — the same lock [enqueue] holds while
     * allocating its monotonic key. Without it, a purge that deletes the
     * current max entry *between* [nextKey]'s `maxOrNull()` read and the
     * matching `store.save` could let the next enqueue reuse a key that
     * collides with a still-in-flight pin. The lock keeps the
     * read-max-then-write counter invariant intact.
     */
    suspend fun purgeStale() {
        enqueueMutex.withLock {
            val cutoff = System.currentTimeMillis() - staleThresholdMs
            val snapshot = store.getAll()
            val stale = snapshot.filterValues { it.savedAtMs < cutoff }.keys
            if (stale.isEmpty()) return@withLock
            Log.d(tag, "purgeStale dropping ${stale.size} expired entries")
            stale.forEach { store.delete(it) }
        }
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

        /**
         * Pure selection helper behind [claimSolePending]: the single entry
         * whose config satisfies [predicate], or `null` when zero **or more
         * than one** match. Extracted so the sole-match + predicate-filter
         * logic is unit-testable without a DataStore (the store/IO layer is
         * exercised by `FeaturePreferencesTest` in `:core:datastore`).
         */
        internal fun <T : WidgetKitConfig> selectSolePending(
            snapshot: Map<Int, PendingEntry<T>>,
            predicate: (T) -> Boolean,
        ): Map.Entry<Int, PendingEntry<T>>? =
            snapshot.entries
                .filter { predicate(it.value.config) }
                // Sole match only — an ambiguous (2+) match can't be
                // correlated to a specific appWidgetId without the OS
                // callback's token, so defer rather than risk swapping two
                // widgets' configs.
                .singleOrNull()
    }
}
