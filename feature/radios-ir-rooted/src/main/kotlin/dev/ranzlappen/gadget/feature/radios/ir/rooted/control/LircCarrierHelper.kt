package dev.ranzlappen.gadget.feature.radios.ir.rooted.control

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

internal const val IR_CARRIER_HARD_LOW_HZ = 20_000
internal const val IR_CARRIER_HARD_HIGH_HZ = 100_000
internal const val IR_CARRIER_HARD_CEILING_MILLIS = 30_000L
private val LIRC_CARRIER_NODE_CANDIDATES = listOf(
    "/sys/class/lirc/lirc0/carrier",
    "/sys/class/lirc/lirc0/frequency",
    "/sys/class/leds/ir-led/carrier",
)

/**
 * Writes `/sys/class/lirc/.../carrier` (or vendor equivalents) to
 * override the IR carrier outside `ConsumerIrManager` ranges.
 * Snapshot+restore via the shared mutation log; carrier clamped to
 * 20–100 kHz inside the helper.
 */
@Singleton
class LircCarrierHelper @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun setCarrier(carrierHz: Int): CarrierHandle? {
        val effectiveHz = carrierHz.coerceIn(IR_CARRIER_HARD_LOW_HZ, IR_CARRIER_HARD_HIGH_HZ)
        val targetNode = LIRC_CARRIER_NODE_CANDIDATES.firstOrNull { isWritable(it) } ?: return null
        val original = readNode(targetNode) ?: return null
        mutationLog.register(targetNode, original)
        val write = shell.exec("echo $effectiveHz > \"$targetNode\"")
        if (!write.isSuccess) {
            mutationLog.unregister(targetNode)
            return null
        }
        return CarrierHandle(targetNode, original)
    }

    suspend fun restoreCarrier(handle: CarrierHandle) {
        val ok = shell.exec("echo \"${handle.originalValue}\" > \"${handle.path}\"").isSuccess
        if (ok) mutationLog.unregister(handle.path)
    }

    private suspend fun readNode(path: String): String? {
        val r = shell.exec("cat \"$path\" 2>/dev/null")
        if (!r.isSuccess) return null
        return r.stdout.firstOrNull()?.trim()
    }

    private suspend fun isWritable(path: String): Boolean {
        val probe = shell.exec("test -w \"$path\" && echo ok")
        return probe.isSuccess && probe.stdout.firstOrNull()?.trim() == "ok"
    }
}

data class CarrierHandle(val path: String, val originalValue: String)
