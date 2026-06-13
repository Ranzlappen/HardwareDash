package dev.ranzlappen.gadget.feature.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Polls [StorageManager] + [StatFs] for every mounted volume. The primary
 * internal volume is always included; removable volumes (SD cards) appear
 * when mounted. No permissions required — [StatFs] reads public block
 * counts and [StorageManager.getStorageVolumes] is unrestricted.
 */
@Singleton
class StorageMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val volumes: Flow<List<StorageVolumeInfo>> = flow {
        while (true) {
            emit(readVolumes())
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun readVolumes(): List<StorageVolumeInfo> {
        val manager = context.getSystemService(StorageManager::class.java)
        return manager.storageVolumes.mapNotNull { volume ->
            val dir = volumePath(volume) ?: return@mapNotNull null
            val stat = runCatching { StatFs(dir) }.getOrNull() ?: return@mapNotNull null
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            StorageVolumeInfo(
                label = volume.getDescription(context),
                totalBytes = total,
                usedBytes = total - free,
                freeBytes = free,
                isRemovable = volume.isRemovable,
            )
        }
    }

    private fun volumePath(volume: android.os.storage.StorageVolume): String? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return volume.directory?.absolutePath
        }
        if (volume.isPrimary) return Environment.getExternalStorageDirectory().absolutePath
        // For removable volumes on API 29: derive path via external files dirs.
        val uuid = volume.uuid ?: return null
        return context.getExternalFilesDirs(null)
            .firstOrNull { it?.absolutePath?.contains(uuid) == true }
            ?.let { externalFilesDir ->
                // /storage/<uuid>/Android/data/<pkg> → /storage/<uuid>
                var f = externalFilesDir
                repeat(3) { f = f.parentFile ?: return@let null }
                f?.absolutePath
            }
    }

    fun internalUsedPercent(): Float {
        val stat = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath)
        }.getOrNull() ?: return 0f
        val total = stat.blockCountLong
        if (total == 0L) return 0f
        val used = total - stat.availableBlocksLong
        return used.toFloat() / total * 100f
    }

    fun internalFreeBytes(): Long {
        val stat = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath)
        }.getOrNull() ?: return 0L
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    private companion object {
        const val POLL_INTERVAL_MS = 5_000L
    }
}
