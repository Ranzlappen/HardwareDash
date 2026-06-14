package dev.ranzlappen.gadget.feature.storage

import androidx.compose.runtime.Immutable

@Immutable
data class StorageVolumeInfo(
    val label: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean,
) {
    val usedPercent: Float
        get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes * 100f
}

internal fun Long.toDisplayGb(): String {
    val gb = this / (1024.0 * 1024.0 * 1024.0)
    return "%.1f GB".format(gb)
}
