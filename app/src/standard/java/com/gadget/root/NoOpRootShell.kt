package com.gadget.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.core.RootShellResult

/**
 * Returns [RootShellResult.UNSUPPORTED] for every call. Shared code that
 * accidentally invokes the shell on the standard flavor will see
 * `result.isUnsupported == true` and can branch without crashing.
 */
class NoOpRootShell : RootShell {
    override suspend fun exec(command: String, timeoutMillis: Long?): RootShellResult =
        unsupported()

    override suspend fun exec(commands: List<String>, timeoutMillis: Long?): RootShellResult =
        unsupported()

    private fun unsupported() = RootShellResult(
        exitCode = RootShellResult.UNSUPPORTED,
        stdout = emptyList(),
        stderr = emptyList(),
        durationMillis = 0L,
    )
}
