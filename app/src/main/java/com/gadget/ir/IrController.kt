package com.gadget.ir

/**
 * Rooted-only IR transmitter capability surface. Standard flavor
 * returns [IrControllerResult.Unsupported] for every method.
 *
 * Baseline IR transmit continues to flow through `ConsumerIrManager`
 * in `RadiosScreen` — this controller is for the rooted "Root extras"
 * surface only.
 */
interface IrController {

    /**
     * Sets a custom carrier frequency outside the framework's
     * supported range. Clamped to 20–100 kHz inside the helper.
     */
    suspend fun customCarrier(config: IrCarrierConfig): IrControllerResult

    /**
     * Direct GPIO toggling of the IR LED for arbitrary timing
     * patterns. ≤ 50 % duty cycle and 5-second hard burst ceiling
     * enforced inside the helper.
     */
    suspend fun rawGpioPattern(config: IrRawPatternConfig): IrControllerResult

    /** Reverts every IR-surface mutation. */
    suspend fun resetAllIrMutations(): IrControllerResult
}
