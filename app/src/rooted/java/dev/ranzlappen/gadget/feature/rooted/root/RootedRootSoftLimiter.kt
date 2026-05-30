package dev.ranzlappen.gadget.feature.rooted.root

import dev.ranzlappen.gadget.core.root.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rolling-window invocation counter. Each [tryAcquire] both peeks AND
 * reserves a slot — there is no separate "commit" step. If the caller fails
 * mid-operation, the slot stays consumed: this is intentional for rate
 * limiting, so a buggy retry loop cannot bypass the gate.
 *
 * Persistence is via the rooted-flavor DataStore qualified by
 * [RootSafetyPrefs]. A per-feature [Mutex] serialises the
 * read-modify-write cycle so concurrent callers cannot double-count.
 */
@Singleton
class RootedRootSoftLimiter @Inject constructor(
    @RootSafetyPrefs private val dataStore: DataStore<Preferences>,
) : RootSoftLimiter {

    private val locks = ConcurrentHashMap<String, Mutex>()

    override suspend fun tryAcquire(
        feature: RootFeatureKey,
        policy: RootLimitPolicy,
    ): RootSoftLimiter.LimitOutcome {
        val mutex = locks.computeIfAbsent(feature.id) { Mutex() }
        return mutex.withLock {
            val prefs = dataStore.data.first()
            val now = System.currentTimeMillis()
            val windowMs = policy.window.inWholeMilliseconds
            val windowStart = prefs[RootPrefKeys.featureWindowStartKey(feature)] ?: 0L
            val count = prefs[RootPrefKeys.featureInvocationCountKey(feature)] ?: 0L

            val (newWindowStart, newCount) = if (now - windowStart >= windowMs) {
                now to 1L
            } else {
                windowStart to (count + 1)
            }

            if (newCount > policy.maxInvocations) {
                val retryAfter = (windowStart + windowMs - now).coerceAtLeast(0L)
                return@withLock RootSoftLimiter.LimitOutcome.Throttled(retryAfter)
            }

            dataStore.edit { mutable ->
                mutable[RootPrefKeys.featureWindowStartKey(feature)] = newWindowStart
                mutable[RootPrefKeys.featureInvocationCountKey(feature)] = newCount
            }
            RootSoftLimiter.LimitOutcome.Granted
        }
    }
}
