package com.gadget.root.emergency

/**
 * Aggregated result of an [EmergencyResetCoordinator.resetEverything]
 * call. Every field is independent — partial successes are reported as
 * such (e.g. mutations reverted but service stop failed).
 */
sealed class EmergencyResetCoordinatorResult {
    data class Ok(
        val sysfsMutationsRestored: Int,
        val sysfsMutationsFailed: Int,
        val keepAliveStopped: Boolean,
        val dozeReset: Boolean,
        val batteryOptimizationReset: Boolean,
        val perFeatureOptOutsCleared: Int,
    ) : EmergencyResetCoordinatorResult()

    data object Unsupported : EmergencyResetCoordinatorResult()

    /**
     * Reported when one or more steps failed catastrophically (the shell
     * itself unavailable, the keep-alive service crashed, etc.). The
     * partial-success counts are still surfaced via [partial].
     */
    data class HardwareError(
        val message: String,
        val partial: Ok?,
    ) : EmergencyResetCoordinatorResult()
}
