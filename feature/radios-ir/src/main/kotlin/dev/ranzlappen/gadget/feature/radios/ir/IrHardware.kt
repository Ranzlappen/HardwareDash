package dev.ranzlappen.gadget.feature.radios.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standard-tier IR hardware access via [ConsumerIrManager].
 *
 * Migrated from `app/src/main/java/com/gadget/ir/IrTransmitter.kt`.
 * The rooted extreme-tier (custom carrier override via LIRC sysfs,
 * raw GPIO pulse) will ship in `:feature:radios-ir-rooted`.
 */
@Singleton
class IrHardware @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    fun hasEmitter(): Boolean = manager?.hasIrEmitter() == true

    fun supportedFrequencies(): List<IntRange> {
        val mgr = manager ?: return emptyList()
        if (!mgr.hasIrEmitter()) return emptyList()
        return try {
            mgr.carrierFrequencies?.map { it.minFrequency..it.maxFrequency } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun transmit(signal: IrSignal): String? = withContext(Dispatchers.IO) {
        val mgr = manager ?: return@withContext "No ConsumerIrManager on this device"
        if (!mgr.hasIrEmitter()) return@withContext "No IR emitter on this device"
        val result = IrCodecs.encode(signal.protocol.name, signal.payload, signal.carrierHz, signal.repeats)
        when (result) {
            is IrCodecs.Result.Error -> result.message
            is IrCodecs.Result.Ok -> runCatching {
                mgr.transmit(result.encoded.carrierHz, result.encoded.pattern)
                null
            }.getOrElse { e -> "Transmit failed: ${e.message}" }
        }
    }
}
