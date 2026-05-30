package com.gadget.storage

import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

private const val DROP_CACHES_PATH = "/proc/sys/vm/drop_caches"
private const val DROP_CACHES_VALUE = "3"

/**
 * Writes the literal `3` to `/proc/sys/vm/drop_caches`, dropping the
 * page cache + dentries + inodes. The helper hard-codes the value — the
 * caller has no influence over what gets written, regardless of the
 * config it passes. Slab-tester values (`4`+) are *not* supported because
 * they can corrupt running drivers.
 */
@Singleton
class DropCachesHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun drop(): Boolean {
        val cmd = "echo $DROP_CACHES_VALUE > $DROP_CACHES_PATH"
        val result = shell.exec(cmd)
        return result.isSuccess
    }
}
