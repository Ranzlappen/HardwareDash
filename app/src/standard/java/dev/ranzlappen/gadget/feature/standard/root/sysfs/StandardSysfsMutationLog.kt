package dev.ranzlappen.gadget.feature.standard.root.sysfs

import dev.ranzlappen.gadget.core.root.sysfs.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor mutation log. The standard APK has no privileged shell so
 * no mutations can ever happen — every method is a no-op. Returning empty
 * lists / zero counts keeps callers' control flow uniform across flavors.
 */
@Singleton
class StandardSysfsMutationLog @Inject constructor() : SysfsMutationLog {
    override fun register(path: String, originalValue: String) = Unit
    override fun unregister(path: String) = Unit
    override fun snapshot(prefixes: List<String>): List<MutatedNode> = emptyList()
    override suspend fun revertAll(prefixes: List<String>): RevertOutcome =
        RevertOutcome(restored = 0, failed = 0)
}
