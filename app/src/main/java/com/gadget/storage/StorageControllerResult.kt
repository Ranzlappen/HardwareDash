package com.gadget.storage

/**
 * Result returned by every [StorageController] privileged method.
 * Same shape as the Batch-6 / Batch-7 controller result types.
 */
sealed class StorageControllerResult {
    data class Ok(val statusNote: String? = null) : StorageControllerResult()
    data object Unsupported : StorageControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : StorageControllerResult()
    data object OptedOut : StorageControllerResult()
    data class HardwareError(val message: String) : StorageControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : StorageControllerResult()

    /**
     * `dumpsys diskstats` excerpt. [persistedFile] is the absolute path
     * of the JSON snapshot if the caller passed `persist = true` and the
     * Logbook write succeeded; null otherwise.
     */
    data class DiskstatsExcerpt(
        val excerpt: String,
        val persistedFile: String? = null,
    ) : StorageControllerResult()

    /**
     * `/proc/mountinfo` parse result. One [MountEntry] per mount point.
     */
    data class MountList(val mounts: List<MountEntry>) : StorageControllerResult()
}

/**
 * Parsed `/proc/mountinfo` row. Field naming follows
 * `Documentation/filesystems/proc.txt` — see kernel docs.
 */
data class MountEntry(
    val mountPoint: String,
    val source: String,
    val fsType: String,
    val flags: String,
    val readOnly: Boolean,
)
