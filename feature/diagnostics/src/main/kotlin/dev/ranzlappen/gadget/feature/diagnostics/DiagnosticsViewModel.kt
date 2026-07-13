package dev.ranzlappen.gadget.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsController
import dev.ranzlappen.gadget.feature.diagnostics.control.DiagnosticsControllerResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The rooted-tools panel state for the diagnostics screen. */
data class DiagnosticsRootToolsState(
    val memInfo: RootActionState = RootActionState(),
    val cpuInfo: RootActionState = RootActionState(),
    val procstats: RootActionState = RootActionState(),
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

    private fun DiagnosticsControllerResult.toActionState(): RootActionState = when (this) {
        is DiagnosticsControllerResult.MemInfoExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars of meminfo")
        is DiagnosticsControllerResult.CpuInfoExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars of cpuinfo")
        is DiagnosticsControllerResult.ProcstatsExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars of procstats")
        is DiagnosticsControllerResult.LogcatExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars")
        is DiagnosticsControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        DiagnosticsControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        DiagnosticsControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is DiagnosticsControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        is DiagnosticsControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is DiagnosticsControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
    }
}
