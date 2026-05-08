package com.gadget.root

import kotlinx.coroutines.flow.Flow

/**
 * User-facing per-feature opt-in toggles. The Settings UI binds to this for
 * its switch state, and the Torch/Vibration controllers consult it via
 * [RootSafetyGate] before every privileged call.
 *
 * Two implementations:
 *   - Standard flavor: every feature is always reported as `false`, every
 *     write is a no-op. The standard APK has no privileged shell, so toggle
 *     state has no effect.
 *   - Rooted flavor: backed by the `root_safety_ds` DataStore. The default
 *     for every feature is OFF — users opt in once and the choice
 *     persists.
 */
interface RootFeatureToggles {
    fun isEnabled(feature: RootFeatureKey): Flow<Boolean>
    suspend fun setEnabled(feature: RootFeatureKey, enabled: Boolean)

    /**
     * Single master switch for the rooted-monitor surface. When ON, the
     * safety gate short-circuits every descriptor flagged `isWriteCapable`,
     * leaving read-only diagnostics (sysfs reads, sensor enumeration, fuel
     * gauge dumps) fully usable. Default OFF.
     */
    fun isMonitorSafetyMode(): Flow<Boolean>
    suspend fun setMonitorSafetyMode(enabled: Boolean)
}
