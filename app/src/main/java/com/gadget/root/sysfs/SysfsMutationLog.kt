package com.gadget.root.sysfs

/**
 * Process-lifetime registry of every sysfs / IIO / power-supply node a rooted
 * helper has mutated. Helpers register the original value before each write
 * and unregister it after a successful local restore in their own
 * `NonCancellable` finally — the log is a belt-and-suspenders global safety
 * net for the path where the user kills the process while a mutation is live.
 *
 * The "Reset all to defaults" button in each extras Card calls
 * [revertAll] (filtered by path-prefix in the controller) to write every
 * tracked original value back via `RootShell` and clear the entry on success.
 */
interface SysfsMutationLog {
    /**
     * Records that [path] is about to be mutated and that [originalValue] is
     * its pre-mutation state. Idempotent on the same path — the *first* call
     * wins so chained writes don't drop the true original.
     */
    fun register(path: String, originalValue: String)

    /**
     * Removes [path] from the log after a successful local restore. Helpers
     * call this from their own finally so the global revert path doesn't
     * double-write.
     */
    fun unregister(path: String)

    /**
     * Snapshot of every currently-tracked mutation, optionally filtered by
     * path prefixes. Order is undefined.
     */
    fun snapshot(prefixes: List<String> = emptyList()): List<MutatedNode>

    /**
     * Writes every tracked original value back via the privileged shell.
     * Filtered by [prefixes] (a node matches if its path starts with any of
     * the prefixes; an empty list matches all). Successfully reverted entries
     * are removed from the log; failed ones are retained so the user can try
     * again.
     */
    suspend fun revertAll(prefixes: List<String> = emptyList()): RevertOutcome
}

data class MutatedNode(val path: String, val originalValue: String)

data class RevertOutcome(val restored: Int, val failed: Int) {
    val isClean: Boolean get() = failed == 0
}
