package dev.ranzlappen.gadget.core.root.core

/**
 * Privileged shell abstraction. The rooted flavor wraps libsu's
 * `Shell.cmd(...)` API; the standard flavor returns [RootShellResult.UNSUPPORTED]
 * so accidental callers fail loudly instead of silently corrupting state.
 *
 * Always go through [dev.ranzlappen.gadget.core.root.RootSafetyGate] before calling [exec] —
 * the shell does not consult the user-opt-out preferences on its own.
 */
interface RootShell {
    /**
     * Executes [command] under root. If [timeoutMillis] is non-null and elapses
     * before completion, the implementation cancels the shell call and returns
     * a result with whatever output was buffered so far.
     */
    suspend fun exec(command: String, timeoutMillis: Long? = null): RootShellResult

    /**
     * Convenience for batched commands run as a single shell invocation.
     * Implementations join with `\n`.
     */
    suspend fun exec(commands: List<String>, timeoutMillis: Long? = null): RootShellResult
}
