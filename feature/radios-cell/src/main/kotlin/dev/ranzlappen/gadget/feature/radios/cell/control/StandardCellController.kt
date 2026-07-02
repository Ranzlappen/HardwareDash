package dev.ranzlappen.gadget.feature.radios.cell.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Cellular controller. Every method returns
 * [CellControllerResult.Unsupported].
 */
@Singleton
class StandardCellController @Inject constructor() : CellController {

    override suspend fun rawModemDump(): CellControllerResult =
        CellControllerResult.Unsupported

    override suspend fun signalDeepDump(): CellControllerResult =
        CellControllerResult.Unsupported

    override suspend fun resetAllCellMutations(): CellControllerResult =
        CellControllerResult.ResetCompleted(restored = 0, failed = 0)
}
