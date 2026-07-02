package dev.ranzlappen.gadget.feature.storage.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.storage.control.MountEntry
import javax.inject.Inject
import javax.inject.Singleton

private const val MOUNTINFO_PATH = "/proc/mountinfo"

/**
 * Read-only `/proc/mountinfo` parser. Mountinfo format per
 * `Documentation/filesystems/proc.txt`:
 *
 *   36 35 98:0 /mnt1 /mnt2 rw,noatime - ext4 /dev/root rw,errors=continue
 *   (mount-id parent-id dev path-in-fs mount-point mount-flags - fs-type source super-flags)
 *
 * The split-on-` - ` is required because mount-flags can contain
 * arbitrary user-supplied tokens and the optional fields between
 * mount-flags and `- fs-type` change between kernel versions.
 */
@Singleton
class MountInfoHelper @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun enumerate(): List<MountEntry> {
        val result = shell.exec("cat $MOUNTINFO_PATH")
        if (!result.isSuccess) return emptyList()
        return result.stdout.mapNotNull(::parseLine)
    }

    private fun parseLine(line: String): MountEntry? {
        val sepIndex = line.indexOf(" - ")
        if (sepIndex < 0) return null
        val left = line.substring(0, sepIndex).split(" ")
        val right = line.substring(sepIndex + 3).split(" ")
        if (left.size < 6 || right.size < 3) return null
        val mountPoint = left[4]
        val mountFlags = left[5]
        val fsType = right[0]
        val source = right[1]
        val superFlags = right[2]
        val readOnly = mountFlags.split(",").contains("ro") ||
            superFlags.split(",").contains("ro")
        return MountEntry(
            mountPoint = mountPoint,
            source = source,
            fsType = fsType,
            flags = "$mountFlags|$superFlags",
            readOnly = readOnly,
        )
    }
}
