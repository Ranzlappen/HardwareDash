package dev.ranzlappen.gadget.core.root.core

/**
 * Result of a [RootShell.exec] call. `exitCode = -1` is reserved for the
 * standard-flavor no-op so callers can distinguish "shell rejected the
 * command" (exit code from libsu) from "no privileged shell exists on this
 * build at all".
 */
data class RootShellResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>,
    val durationMillis: Long,
) {
    val isSuccess: Boolean get() = exitCode == 0
    val isUnsupported: Boolean get() = exitCode == UNSUPPORTED

    companion object {
        const val UNSUPPORTED: Int = -1
    }
}
