package com.gadget.sensors

import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

internal const val SENSORS_FUSION_HARD_CEILING_MILLIS = 60_000L

private val FUSION_DISABLE_NODE_CANDIDATES = listOf(
    "/sys/class/sensors/fusion/enable",
    "/sys/class/sensors/rotation_vector/enable",
    "/sys/class/sensors/game_rotation_vector/enable",
    "/sys/class/sensors/geomagnetic_rotation_vector/enable",
)

/**
 * Disables Android's hardware fusion by writing zero to vendor-specific
 * fusion-enable nodes where exposed. Most modern devices route fusion
 * through the Android Sensor HAL rather than exposing toggleable sysfs
 * nodes, so this is best-effort: returns
 * [SensorsControllerResult.Unsupported] cleanly if no candidate node
 * accepts the write.
 *
 * Snapshots and restores the original enable values in `NonCancellable`
 * finally so HAL fusion reactivates even on cancelled coroutines.
 */
@Singleton
class MultiFusionOverride @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun apply(config: FusionOverrideConfig): SensorsControllerResult {
        val snapshots = mutableListOf<NodeSnapshot>()
        for (candidate in FUSION_DISABLE_NODE_CANDIDATES) {
            val original = readNode(candidate) ?: continue
            mutationLog.register(candidate, original)
            val write = shell.exec("echo 0 > \"$candidate\"")
            if (write.isSuccess) {
                snapshots += NodeSnapshot(candidate, original)
            } else {
                mutationLog.unregister(candidate)
            }
        }
        if (snapshots.isEmpty()) return SensorsControllerResult.Unsupported

        val effectiveDuration = config.durationMillis.coerceAtMost(SENSORS_FUSION_HARD_CEILING_MILLIS)
        return try {
            delay(effectiveDuration)
            SensorsControllerResult.Ok()
        } finally {
            withContext(NonCancellable) {
                for (snapshot in snapshots) {
                    val restore = shell.exec("echo \"${snapshot.originalValue}\" > \"${snapshot.path}\"")
                    if (restore.isSuccess) mutationLog.unregister(snapshot.path)
                }
            }
        }
    }

    private suspend fun readNode(path: String): String? {
        val probe = shell.exec("test -w \"$path\" && cat \"$path\"")
        if (!probe.isSuccess) return null
        return probe.stdout.firstOrNull()?.trim()
    }

    private data class NodeSnapshot(val path: String, val originalValue: String)
}
