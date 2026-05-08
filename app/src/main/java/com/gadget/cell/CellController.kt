package com.gadget.cell

/**
 * Rooted-only Cellular diagnostics surface. Read-only — no AT-command
 * write path. Standard flavor returns [CellControllerResult.Unsupported].
 */
interface CellController {

    /**
     * Read-only walk over `/sys/class/qcom_smd*`, `/proc/qmi_devices`,
     * `/sys/class/net/rmnet<N>/`. Surfaces `Unsupported` cleanly on
     * non-Qualcomm devices.
     */
    suspend fun rawModemDump(): CellControllerResult

    /**
     * Read-only deep RSRP / RSRQ / SINR per-band breakdown via vendor
     * sysfs nodes if present. Surfaces `Unsupported` cleanly on
     * platforms without the diagnostic nodes.
     */
    suspend fun signalDeepDump(): CellControllerResult

    /** Always `ResetCompleted(0, 0)` — no mutations to revert. */
    suspend fun resetAllCellMutations(): CellControllerResult
}
