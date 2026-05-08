package com.gadget.ir

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor IR controller. Every method returns
 * [IrControllerResult.Unsupported].
 */
@Singleton
class StandardIrController @Inject constructor() : IrController {

    override suspend fun customCarrier(config: IrCarrierConfig): IrControllerResult =
        IrControllerResult.Unsupported

    override suspend fun rawGpioPattern(config: IrRawPatternConfig): IrControllerResult =
        IrControllerResult.Unsupported

    override suspend fun resetAllIrMutations(): IrControllerResult =
        IrControllerResult.ResetCompleted(restored = 0, failed = 0)
}
