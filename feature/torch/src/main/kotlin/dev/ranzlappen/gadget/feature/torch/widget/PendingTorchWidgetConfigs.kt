package dev.ranzlappen.gadget.feature.torch.widget

import android.util.Log
import dev.ranzlappen.gadget.core.datastore.FeaturePreferences
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * Persistent bridge for [TorchWidgetCreator]'s pin flow.
 *
 * Problem this solves: [android.appwidget.AppWidgetManager.requestPinAppWidget]
 * doesn't return the new `appWidgetId` synchronously. Instead, the
 * OS fires the caller-supplied success [android.app.PendingIntent]
 * **after** the user accepts the launcher's pin dialog, with the
 * newly-assigned ID attached as
 * [android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID].
 *
 * To carry the user's pre-pin configuration through that round-trip:
 * 1. UI calls [enqueue] before invoking `requestPinAppWidget` —
 *    receives back a stable string token.
 * 2. The success-callback `PendingIntent` carries the token in its
 *    extras (`EXTRA_PENDING_CONFIG_TOKEN`).
 * 3. [WidgetPinSuccessReceiver.onReceive] reads the token + the
 *    `appWidgetId`, calls [claim] to pop the config, and saves it
 *    to [TorchWidgetConfigRepository] keyed by `appWidgetId`.
 *
 * **Why persistent (not in-memory)?** Previously the bridge was a
 * `ConcurrentHashMap`. On low-RAM devices the OS sometimes evicts
 * our process while the launcher pin dialog is on-screen; the OS
 * later fires the success callback into a freshly-spawned process
 * with no in-memory map — token mismatch → silent drop → "the
 * widget got pinned but never appeared in the in-app list". This
 * implementation persists every enqueued entry to a Preferences
 * DataStore (`torch_pending_widgets`) so process death is survivable.
 *
 * The persisted entry carries a timestamp; a janitor invocation
 * ([purgeStale]) drops entries older than [STALE_THRESHOLD_MS]
 * (1 hour) so cancelled pin dialogs don't accumulate orphaned
 * configs forever.
 *
 * **Token → integer key bridge:** [FeaturePreferences] uses Int
 * keys. The UUID token is reduced to a stable Int via
 * `token.hashCode().absoluteValue` — collisions across the small
 * number of concurrently in-flight pins are vanishingly rare.
 */
@Singleton
class PendingTorchWidgetConfigs @Inject constructor(
    factory: FeaturePreferencesFactory,
) {
    private val store: FeaturePreferences<PendingEntry> = factory.create(
        fileName = "torch_pending_widgets",
        keyPrefix = "pending_",
        serializer = PendingEntry.serializer(),
    )

    /** Process-lifetime scope for the fire-and-forget startup janitor.
     *  Never blocks the injecting thread. */
    private val janitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Best-effort startup janitor — drops pending entries older than
        // the stale threshold. Launched (not runBlocking) so Hilt's
        // constructor-injection path never blocks on DataStore IO.
        janitorScope.launch {
            runCatching { purgeStale() }
                .onFailure { Log.w(TAG, "Stale-pending janitor skipped", it) }
        }
    }

    /**
     * Store a pending config; return the token to embed in the
     * success [android.app.PendingIntent]. Suspending because the
     * write hits DataStore. Callers that aren't inside a coroutine
     * scope can wrap in `runBlocking` — the write is single-digit-ms
     * on commit hardware.
     */
    suspend fun enqueue(config: TorchWidgetConfig): String {
        val token = UUID.randomUUID().toString()
        val entry = PendingEntry(
            token = token,
            savedAtMs = System.currentTimeMillis(),
            config = config,
        )
        store.save(keyFor(token), entry)
        Log.d(TAG, "enqueue token=$token type=${config.type}")
        return token
    }

    /**
     * Pop the config registered under [token]. Returns `null` if
     * the token is unknown (already claimed, never registered, or
     * persisted entry was purged as stale).
     */
    suspend fun claim(token: String): TorchWidgetConfig? {
        val key = keyFor(token)
        val entry = store.get(key)
        if (entry == null) {
            Log.w(TAG, "claim token=$token MISS — no pending entry")
            return null
        }
        store.delete(key)
        Log.d(TAG, "claim token=$token HIT — type=${entry.config.type}")
        return entry.config
    }

    /**
     * Drop all pending entries older than [STALE_THRESHOLD_MS]. The
     * UI's pin dialog completes in seconds; entries older than an
     * hour are almost certainly abandoned cancellations.
     */
    suspend fun purgeStale() {
        val cutoff = System.currentTimeMillis() - STALE_THRESHOLD_MS
        val snapshot = store.getAll()
        val stale = snapshot.filterValues { it.savedAtMs < cutoff }.keys
        if (stale.isEmpty()) return
        Log.d(TAG, "purgeStale dropping ${stale.size} expired entries")
        stale.forEach { store.delete(it) }
    }

    private fun keyFor(token: String): Int = token.hashCode().absoluteValue

    /** Wrapper that survives schema drift on the pending-config
     *  side independently of [TorchWidgetConfig] proper. The two
     *  serialization surfaces are deliberately separate. */
    @Serializable
    data class PendingEntry(
        val token: String,
        val savedAtMs: Long,
        val config: TorchWidgetConfig,
    )

    companion object {
        /** Logcat tag — `adb logcat -s TorchPinFlow:D` traces the
         *  full pin flow end-to-end. Shared with [TorchWidgetCreator]
         *  and [WidgetPinSuccessReceiver]. */
        const val TAG = "TorchPinFlow"

        /** Pending entries older than this are dropped on next
         *  startup. One hour is plenty — launcher pin dialogs
         *  typically resolve in seconds. */
        const val STALE_THRESHOLD_MS: Long = 60L * 60L * 1000L
    }
}
