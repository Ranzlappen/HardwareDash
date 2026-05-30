package dev.ranzlappen.gadget.feature.standard.root.emergency

import dev.ranzlappen.gadget.core.root.emergency.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor emergency reset. Returns `Ok` with all zero counts.
 * The standard APK has no privileged shell so there's nothing to
 * revert — and no foreground keep-alive service worth stopping.
 */
@Singleton
class StandardEmergencyResetCoordinator @Inject constructor() : EmergencyResetCoordinator {

    override suspend fun resetEverything(
        options: EmergencyResetOptions,
    ): EmergencyResetCoordinatorResult = EmergencyResetCoordinatorResult.Ok(
        sysfsMutationsRestored = 0,
        sysfsMutationsFailed = 0,
        keepAliveStopped = false,
        dozeReset = false,
        batteryOptimizationReset = false,
        perFeatureOptOutsCleared = 0,
    )
}
