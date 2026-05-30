package dev.ranzlappen.gadget.feature.rooted.root.sysfs

import dev.ranzlappen.gadget.core.root.sysfs.*
import dev.ranzlappen.gadget.core.root.core.RootShell
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor mutation log. Backed by a process-lifetime
 * [ConcurrentHashMap] keyed by absolute sysfs path. [revertAll] writes each
 * stored original value back via `RootShell` (`echo <value> > <path>`) and
 * removes the entry on success. Failed reverts are retained so the user can
 * tap "Reset" again later — losing track of an unrestored mutation is worse
 * than retrying a write that already succeeded.
 */
@Singleton
class RootedSysfsMutationLog @Inject constructor(
    private val shell: RootShell,
) : SysfsMutationLog {

    private val tracked = ConcurrentHashMap<String, String>()

    override fun register(path: String, originalValue: String) {
        tracked.putIfAbsent(path, originalValue)
    }

    override fun unregister(path: String) {
        tracked.remove(path)
    }

    override fun snapshot(prefixes: List<String>): List<MutatedNode> =
        tracked.entries
            .asSequence()
            .filter { (path, _) -> matchesPrefixes(path, prefixes) }
            .map { (path, value) -> MutatedNode(path, value) }
            .toList()

    override suspend fun revertAll(prefixes: List<String>): RevertOutcome {
        val candidates = snapshot(prefixes)
        var restored = 0
        var failed = 0
        for (node in candidates) {
            val command = "echo \"${node.originalValue}\" > \"${node.path}\""
            val result = shell.exec(command)
            if (result.isSuccess) {
                tracked.remove(node.path)
                restored++
            } else {
                failed++
            }
        }
        return RevertOutcome(restored = restored, failed = failed)
    }

    private fun matchesPrefixes(path: String, prefixes: List<String>): Boolean {
        if (prefixes.isEmpty()) return true
        return prefixes.any { path.startsWith(it) }
    }
}
