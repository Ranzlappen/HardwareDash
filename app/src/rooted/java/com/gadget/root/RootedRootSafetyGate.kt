package com.gadget.root

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real safety gate consulted before every rooted operation. Composes three
 * checks in order:
 *
 *   1. Capability — is root actually granted right now? If not, [Unsupported].
 *   2. User opt-out — has the user disabled this specific feature?
 *      If yes, [BlockedByUser]. The default for every feature is OFF; the
 *      user must opt in once via the Settings card.
 *   3. Soft limiter — is there room in the rolling window? If not,
 *      [BlockedByLimiter] with a retry hint.
 *
 *  [recordInvocation] only emits an audit event; the slot was already
 *  consumed by [RootedRootSoftLimiter.tryAcquire] inside [check].
 */
@Singleton
class RootedRootSafetyGate @Inject constructor(
    private val capabilityRegistry: RootCapabilityRegistry,
    private val featureRegistry: RootFeatureRegistry,
    private val limiter: RootSoftLimiter,
    @RootSafetyPrefs private val dataStore: DataStore<Preferences>,
) : RootSafetyGate {

    override suspend fun check(feature: RootFeatureKey): RootGateDecision {
        if (!capabilityRegistry.hasRootAccess()) return RootGateDecision.Unsupported

        val descriptor = featureRegistry.descriptor(feature)

        if (!isFeatureEnabled(feature, descriptor)) return RootGateDecision.BlockedByUser

        val limit = descriptor.limit ?: return RootGateDecision.Allowed
        return when (val outcome = limiter.tryAcquire(feature, limit)) {
            RootSoftLimiter.LimitOutcome.Granted -> RootGateDecision.Allowed
            is RootSoftLimiter.LimitOutcome.Throttled ->
                RootGateDecision.BlockedByLimiter(outcome.retryAfterMillis)
        }
    }

    override suspend fun recordInvocation(feature: RootFeatureKey) {
        Timber.d("Root feature invoked: %s", feature.id)
    }

    private suspend fun isFeatureEnabled(
        feature: RootFeatureKey,
        descriptor: RootFeatureDescriptor,
    ): Boolean {
        val prefs = dataStore.data.first()
        return prefs[RootPrefKeys.featureEnabledKey(feature)] ?: descriptor.defaultOn
    }
}
