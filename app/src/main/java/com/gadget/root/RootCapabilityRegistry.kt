package com.gadget.root

import com.gadget.root.core.RootDetection

/**
 * Single source of truth for "is the rooted code path available on this build
 * and this device?". Implementations are flavor-scoped: the standard flavor
 * always reports unsupported; the rooted flavor delegates to the libsu-backed
 * [com.gadget.root.core.RootDetector] and caches the result.
 *
 * Feature code MUST NOT branch on `BuildConfig.IS_ROOTED` — it must consult
 * this interface (and [RootSafetyGate]) so flavor differences flow through a
 * single Hilt-bound implementation.
 *
 * Lifecycle: [probe] runs once at app launch (driven by
 * [com.gadget.root.launch.LaunchGate]) and seeds the cache. After that, the
 * non-suspend [hasRootAccess] / [isFeatureAvailable] read from the cached
 * detection result and are safe to call from any thread.
 */
interface RootCapabilityRegistry {
    /** Whether this build was compiled as the rooted flavor. */
    val isRootedFlavor: Boolean

    /**
     * One-shot, idempotent root probe. The first caller blocks while libsu
     * negotiates a shell; subsequent calls return the cached result.
     */
    suspend fun probe(): RootDetection

    /**
     * Cached answer to "do we have a usable root shell right now?". Returns
     * false until [probe] has resolved at least once.
     */
    fun hasRootAccess(): Boolean

    /** Whether [feature] has a real (non-stub) implementation in this build. */
    fun isFeatureAvailable(feature: RootFeatureKey): Boolean
}
