package dev.ranzlappen.gadget.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsController
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Last-run status of one rooted diagnostics dump (W6 in-screen surface). */
data class DiagActionState(
    val message: String? = null,
    val isError: Boolean = false,
    val running: Boolean = false,
)

/** The rooted-tools panel state for the diagnostics screen. */
data class DiagnosticsRootToolsState(
    val memInfo: DiagActionState = DiagActionState(),
    val cpuInfo: DiagActionState = DiagActionState(),
    val procstats: DiagActionState = DiagActionState(),
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnosticsController: DiagnosticsController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(
        DiagnosticsState(isRootedFlavor = rootCapabilityRegistry.isRootedFlavor),
    )
    val state: StateFlow<DiagnosticsState> = _state

    private val _rootTools = MutableStateFlow(DiagnosticsRootToolsState())

    /** Live status of the rooted read-only dump actions. */
    val rootTools: StateFlow<DiagnosticsRootToolsState> = _rootTools.asStateFlow()

    fun onDumpMemInfo() {
        viewModelScope.launch {
            _rootTools.update { it.copy(memInfo = it.memInfo.copy(running = true)) }
            val result = diagnosticsController.dumpMemInfo()
            _rootTools.update { it.copy(memInfo = result.toActionState()) }
        }
    }

    fun onDumpCpuInfo() {
        viewModelScope.launch {
            _rootTools.update { it.copy(cpuInfo = it.cpuInfo.copy(running = true)) }
            val result = diagnosticsController.dumpCpuInfo()
            _rootTools.update { it.copy(cpuInfo = result.toActionState()) }
        }
    }

    fun onDumpProcstats() {
        viewModelScope.launch {
            _rootTools.update { it.copy(procstats = it.procstats.copy(running = true)) }
            val result = diagnosticsController.dumpProcstats()
            _rootTools.update { it.copy(procstats = result.toActionState()) }
        }
    }

    private fun DiagnosticsControllerResult.toActionState(): DiagActionState = when (this) {
        is DiagnosticsControllerResult.MemInfoExcerpt ->
            DiagActionState(message = "Captured ${excerpt.length} chars of meminfo")
        is DiagnosticsControllerResult.CpuInfoExcerpt ->
            DiagActionState(message = "Captured ${excerpt.length} chars of cpuinfo")
        is DiagnosticsControllerResult.ProcstatsExcerpt ->
            DiagActionState(message = "Captured ${excerpt.length} chars of procstats")
        is DiagnosticsControllerResult.LogcatExcerpt ->
            DiagActionState(message = "Captured ${excerpt.length} chars")
        is DiagnosticsControllerResult.Ok ->
            DiagActionState(message = statusNote ?: "Done")
        DiagnosticsControllerResult.Unsupported ->
            DiagActionState(message = "Requires the rooted app version", isError = true)
        DiagnosticsControllerResult.OptedOut ->
            DiagActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is DiagnosticsControllerResult.RateLimited ->
            DiagActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        is DiagnosticsControllerResult.HardwareError ->
            DiagActionState(message = message, isError = true)
        is DiagnosticsControllerResult.ResetCompleted ->
            DiagActionState(message = "Reset $restored restored, $failed failed")
    }
}
