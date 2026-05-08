package com.gadget.storage

import com.gadget.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

internal val FSTRIM_PARTITION_ALLOWLIST = setOf("/data", "/cache")
internal const val FSTRIM_TIMEOUT_MILLIS = 60_000L

/**
 * Issues `fstrim -v <partition>` against partitions in the
 * [FSTRIM_PARTITION_ALLOWLIST]. Anything outside the allow-list is
 * silently dropped — callers cannot pass `/`, `/system`, or `/vendor`
 * regardless of input. Read-only partitions are skipped (the kernel
 * rejects fstrim on RO mounts anyway, but we filter early to avoid
 * noisy stderr).
 */
@Singleton
class FstrimHelper @Inject constructor(
    private val shell: RootShell,
    private val mountInfo: MountInfoHelper,
) {
    suspend fun trim(partitions: List<String>, verbose: Boolean): TrimOutcome {
        val mounts = mountInfo.enumerate().associateBy { it.mountPoint }
        val safe = partitions.filter { it in FSTRIM_PARTITION_ALLOWLIST }
        if (safe.isEmpty()) {
            return TrimOutcome(
                trimmed = emptyList(),
                skipped = partitions,
                output = "no allow-listed partitions in request",
            )
        }
        val trimmed = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val output = StringBuilder()
        val flag = if (verbose) "-v " else ""
        for (partition in safe) {
            val mount = mounts[partition]
            if (mount?.readOnly == true) {
                skipped += partition
                continue
            }
            val result = shell.exec("fstrim $flag\"$partition\"", timeoutMillis = FSTRIM_TIMEOUT_MILLIS)
            if (result.isSuccess) {
                trimmed += partition
                output.append(result.stdout.joinToString("\n"))
                output.append('\n')
            } else {
                skipped += partition
            }
        }
        for (rejected in partitions) {
            if (rejected !in FSTRIM_PARTITION_ALLOWLIST) skipped += rejected
        }
        return TrimOutcome(trimmed = trimmed, skipped = skipped, output = output.toString())
    }
}

data class TrimOutcome(
    val trimmed: List<String>,
    val skipped: List<String>,
    val output: String,
)
