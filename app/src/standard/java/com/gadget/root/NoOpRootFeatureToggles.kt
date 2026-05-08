package com.gadget.root

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Standard-flavor toggle store. Every feature is permanently OFF and writes
 * are silently dropped — the standard APK has no privileged shell, so a
 * toggle wouldn't change anything anyway.
 */
class NoOpRootFeatureToggles : RootFeatureToggles {
    override fun isEnabled(feature: RootFeatureKey): Flow<Boolean> = flowOf(false)
    override suspend fun setEnabled(feature: RootFeatureKey, enabled: Boolean) = Unit
    override fun isMonitorSafetyMode(): Flow<Boolean> = flowOf(false)
    override suspend fun setMonitorSafetyMode(enabled: Boolean) = Unit
}
