package com.gadget.sensors

import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

internal const val SENSORS_OVERCLOCK_HARD_CEILING_MILLIS = 30_000L
private const val I2CGET_BIN = "i2cget"
private const val I2CSET_BIN = "i2cset"
private const val I2C_TRANSFER_FORMAT_BYTE = "b"

/**
 * Pushes ODR (output data rate) / range registers via `i2cset` beyond the
 * driver's stock configuration. Snapshots the original register value with
 * `i2cget` before writing, registers the synthesized "i2c-${bus}-${addr}-${reg}"
 * path with [SysfsMutationLog] for the global reset path, and restores the
 * original value in a `NonCancellable` finally on every exit path.
 *
 * `requiresExplicitConfirm = true` on the descriptor — bad register writes
 * can permanently shift MEMS calibration on some sensor parts. Hard 30 s
 * active window enforced via `withTimeoutOrNull`.
 *
 * On devices without `i2cget` / `i2cset` (most stock Android kernels lack
 * `i2c-tools`), surfaces [SensorsControllerResult.Unsupported] cleanly.
 */
@Singleton
class SensorOverclocker @Inject constructor(
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
) {
    suspend fun overclock(config: OverclockConfig): SensorsControllerResult {
        val toolCheck = shell.exec("which $I2CGET_BIN")
        if (!toolCheck.isSuccess || toolCheck.stdout.firstOrNull()?.trim().isNullOrEmpty()) {
            return SensorsControllerResult.Unsupported
        }
        val original = readRegister(config) ?: return SensorsControllerResult.HardwareError(
            "i2cget failed for bus=${config.i2cBus} addr=0x${config.i2cAddress.toHex()} reg=0x${config.odrRegister.toHex()}",
        )
        val pseudoPath = pseudoPath(config)
        mutationLog.register(pseudoPath, original)

        val effectiveDuration = config.durationMillis.coerceAtMost(SENSORS_OVERCLOCK_HARD_CEILING_MILLIS)
        return try {
            val write = writeRegister(config, config.odrValue)
            if (!write) {
                mutationLog.unregister(pseudoPath)
                return SensorsControllerResult.HardwareError(
                    "i2cset failed for reg=0x${config.odrRegister.toHex()}",
                )
            }
            withTimeoutOrNull(effectiveDuration) { delay(effectiveDuration) }
            SensorsControllerResult.Ok()
        } finally {
            withContext(NonCancellable) {
                val originalInt = original.removePrefix("0x").toIntOrNull(16)
                if (originalInt != null && writeRegister(config, originalInt)) {
                    mutationLog.unregister(pseudoPath)
                }
            }
        }
    }

    private suspend fun readRegister(config: OverclockConfig): String? {
        val command = buildString {
            append(I2CGET_BIN)
            append(" -y ")
            append(config.i2cBus)
            append(" 0x")
            append(config.i2cAddress.toHex())
            append(" 0x")
            append(config.odrRegister.toHex())
            append(' ')
            append(I2C_TRANSFER_FORMAT_BYTE)
        }
        val result = shell.exec(command)
        if (!result.isSuccess) return null
        return result.stdout.firstOrNull()?.trim()
    }

    private suspend fun writeRegister(config: OverclockConfig, value: Int): Boolean {
        val command = buildString {
            append(I2CSET_BIN)
            append(" -y ")
            append(config.i2cBus)
            append(" 0x")
            append(config.i2cAddress.toHex())
            append(" 0x")
            append(config.odrRegister.toHex())
            append(" 0x")
            append(value.toHex())
            append(' ')
            append(I2C_TRANSFER_FORMAT_BYTE)
        }
        return shell.exec(command).isSuccess
    }

    private fun pseudoPath(config: OverclockConfig): String =
        "i2c://${config.i2cBus}/0x${config.i2cAddress.toHex()}/0x${config.odrRegister.toHex()}"

    private fun Int.toHex(): String = Integer.toHexString(this)
}
