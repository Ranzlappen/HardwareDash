package com.gadget.storage

/**
 * Configures an `fstrim` invocation. The impl filters [partitions]
 * against an internal `/data`, `/cache` allow-list — anything else is
 * silently dropped regardless of caller input. `verbose = true` makes
 * the helper attach the `-v` flag for human-readable output in the
 * status line.
 */
data class FstrimConfig(
    val partitions: List<String>,
    val verbose: Boolean = true,
)

/**
 * Configures a `drop_caches` write. [mode] is informational only — the
 * helper always writes the literal `3` (page cache + dentries + inodes)
 * because the slab-tester values (`4`+) can corrupt running drivers.
 */
data class DropCachesConfig(
    val mode: DropCachesMode = DropCachesMode.PAGECACHE_DENTRIES_INODES,
)

enum class DropCachesMode {
    PAGECACHE_DENTRIES_INODES,
}
