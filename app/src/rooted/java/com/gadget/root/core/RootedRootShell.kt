package com.gadget.root.core

import dev.ranzlappen.gadget.core.root.core.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * libsu-backed [RootShell]. Runs commands off-main. When [timeoutMillis] is
 * non-null and the shell exceeds it, the call returns whatever output had
 * accumulated so far with the special exit code [RootShellResult.UNSUPPORTED]
 * (so callers see `result.isUnsupported == true` and treat it as a hard
 * failure, not a "command exited 0").
 */
@Singleton
class RootedRootShell @Inject constructor() : RootShell {

    override suspend fun exec(command: String, timeoutMillis: Long?): RootShellResult =
        execInternal(listOf(command), timeoutMillis)

    override suspend fun exec(commands: List<String>, timeoutMillis: Long?): RootShellResult =
        execInternal(commands, timeoutMillis)

    private suspend fun execInternal(commands: List<String>, timeoutMillis: Long?): RootShellResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val out = mutableListOf<String>()
            val err = mutableListOf<String>()
            val joined = commands.joinToString(separator = "\n")
            val result = if (timeoutMillis != null) {
                withTimeoutOrNull(timeoutMillis) {
                    Shell.cmd(joined).to(out, err).exec()
                }
            } else {
                Shell.cmd(joined).to(out, err).exec()
            }
            RootShellResult(
                exitCode = result?.code ?: RootShellResult.UNSUPPORTED,
                stdout = out.toList(),
                stderr = err.toList(),
                durationMillis = System.currentTimeMillis() - start,
            )
        }
}
