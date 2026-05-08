package com.gadget.storage

/**
 * Rooted-only Storage capability surface. Standard flavor returns
 * [StorageControllerResult.Unsupported] for every method.
 *
 * Privileged surface: `dumpsys diskstats` (read-only), `/proc/mountinfo`
 * (read-only), `fstrim -v` against a hard `/data` `/cache` allow-list, and
 * `echo 3 > /proc/sys/vm/drop_caches`. Nothing else — no `mount` /
 * `umount` / mount-namespace / loop-device tricks (vendor-locked on most
 * devices and far too easy to corrupt the userdata partition).
 */
interface StorageController {

    /**
     * Read-only `dumpsys diskstats` snapshot. The impl tail-caps the output
     * to 8 KB and optionally persists a structured JSON copy to the
     * Logbook directory (via [StorageControllerResult.DiskstatsExcerpt.persistedFile]).
     */
    suspend fun dumpDiskstats(persist: Boolean = false): StorageControllerResult

    /**
     * Read-only `/proc/mountinfo` enumeration. Returns one
     * [StorageControllerResult.MountList] entry per mount. Benign info
     * disclosure — every mount point + flag is already visible to any
     * shell user via `mount`.
     */
    suspend fun enumerateMounts(): StorageControllerResult

    /**
     * Issues `fstrim -v <partition>` against the partitions in
     * [FstrimConfig.partitions], filtered by an internal hard allow-list
     * (`/data`, `/cache`). Anything outside the allow-list is silently
     * skipped — callers cannot trim `/`, `/system`, or `/vendor`.
     *
     * fstrim is intrinsically non-reversible (it tells the FS to discard
     * unallocated blocks); that's why this entry sets
     * `requiresExplicitConfirm = true` in the registry.
     */
    suspend fun trimFilesystem(config: FstrimConfig): StorageControllerResult

    /**
     * Writes the literal `3` to `/proc/sys/vm/drop_caches`, dropping the
     * page cache + dentries + inodes. Caller-supplied values are ignored —
     * the helper hard-codes `3` regardless of [DropCachesConfig.mode].
     * Auto-revert is a no-op (caches re-warm naturally).
     */
    suspend fun dropKernelCaches(config: DropCachesConfig): StorageControllerResult

    /** Reverts every Storage-surface mutation registered with the log. */
    suspend fun resetAllStorageMutations(): StorageControllerResult

    /**
     * Auto-revert path called on `FileMetadataScreen` dispose. fstrim and
     * drop_caches are intrinsically non-reversible, so this method only
     * filters the log by `mount://` prefix — which is currently unused.
     * It exists so the screen-exit hook is shape-identical to the other
     * Batch-7/8 surfaces.
     */
    suspend fun revertOnScreenExit(): StorageControllerResult
}
