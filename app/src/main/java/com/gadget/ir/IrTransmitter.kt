package com.gadget.ir

import android.content.Context
import android.hardware.ConsumerIrManager

object IrTransmitter {

    private fun manager(context: Context): ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasEmitter(context: Context): Boolean =
        manager(context)?.hasIrEmitter() == true

    /**
     * Returns the supported (min, max) carrier frequency ranges in Hz.
     * Empty list means the device doesn't support IR or the API returned nothing.
     */
    fun carrierFrequencies(context: Context): List<IntRange> {
        val mgr = manager(context) ?: return emptyList()
        if (!mgr.hasIrEmitter()) return emptyList()
        return try {
            mgr.carrierFrequencies?.map { it.minFrequency..it.maxFrequency } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns null on success or an error message on failure.
     */
    fun transmit(context: Context, carrierHz: Int, pattern: IntArray): String? {
        val mgr = manager(context) ?: return "No ConsumerIrManager on this device"
        if (!mgr.hasIrEmitter()) return "No IR emitter on this device"
        if (pattern.isEmpty()) return "Empty IR pattern"
        return try {
            mgr.transmit(carrierHz, pattern)
            null
        } catch (e: Exception) {
            "Transmit failed: ${e.message}"
        }
    }
}
