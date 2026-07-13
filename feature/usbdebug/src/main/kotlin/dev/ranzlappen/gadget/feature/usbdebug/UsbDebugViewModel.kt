package dev.ranzlappen.gadget.feature.usbdebug

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aggregating ViewModel for [UsbDebugScreen].
 *
 * Standard-tier: reads `Settings.Global.ADB_ENABLED` directly (no root
 * needed — Android has no separate "USB debugging" setting, it's the
 * same flag ADB reads) and offers a deep-link to Developer options.
 *
 * Rooted-tier: wires the four privileged [UsbDebuggingController] methods
 * (function-role switch + three read-only dumps) through to
 * [UsbDebugState]. On the standard flavor the controller no-ops to
 * [UsbDebuggingControllerResult.Unsupported] for all four, which
 * [describeError] surfaces as an inline message (the rooted-only cards
 * are hidden entirely on that flavor by [UsbDebugScreenContent], so in
 * practice these branches only run rooted).
 */
/** The rooted-tools panel state for the USB-debug screen (W6 in-screen surface). */
data class UsbDebugRootToolsState(
    val usb: RootActionState = RootActionState(),
    val serial: RootActionState = RootActionState(),
    val devices: RootActionState = RootActionState(),
)

@HiltViewModel
class UsbDebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: UsbDebuggingController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UsbDebugState(
            isRootedFlavor = rootCapabilityRegistry.isRootedFlavor,
            usbDebuggingEnabled = readUsbDebuggingEnabled(),
        ),
    )
    val state: StateFlow<UsbDebugState> = _state.asStateFlow()

    private val _rootTools = MutableStateFlow(UsbDebugRootToolsState())

    /** Live status of the three rooted read-only USB dumps (W6 surface). */
    val rootTools: StateFlow<UsbDebugRootToolsState> = _rootTools.asStateFlow()

    fun onDumpUsb() {
        viewModelScope.launch {
            _rootTools.update { it.copy(usb = it.usb.copy(running = true)) }
            val result = controller.dumpUsb()
            _rootTools.update { it.copy(usb = result.toActionState()) }
        }
    }

    fun onDumpSerial() {
        viewModelScope.launch {
            _rootTools.update { it.copy(serial = it.serial.copy(running = true)) }
            val result = controller.dumpSerialService()
            _rootTools.update { it.copy(serial = result.toActionState()) }
        }
    }

    fun onDumpDevices() {
        viewModelScope.launch {
            _rootTools.update { it.copy(devices = it.devices.copy(running = true)) }
            val result = controller.dumpUsbDevicesDebug()
            _rootTools.update { it.copy(devices = result.toActionState()) }
        }
    }

    private fun UsbDebuggingControllerResult.toActionState(): RootActionState = when (this) {
        is UsbDebuggingControllerResult.Ok ->
            RootActionState(message = statusNote ?: "Done")
        UsbDebuggingControllerResult.Unsupported ->
            RootActionState(message = "Requires the rooted app version", isError = true)
        is UsbDebuggingControllerResult.RateLimited ->
            RootActionState(message = "Rate limited — retry in ${retryAfterMillis}ms", isError = true)
        UsbDebuggingControllerResult.OptedOut ->
            RootActionState(message = "Blocked by your root-safety opt-out", isError = true)
        is UsbDebuggingControllerResult.HardwareError ->
            RootActionState(message = message, isError = true)
        is UsbDebuggingControllerResult.ResetCompleted ->
            RootActionState(message = "Reset $restored restored, $failed failed")
        is UsbDebuggingControllerResult.UsbFunctionSnapshot ->
            RootActionState(message = "Function $appliedFunction")
        is UsbDebuggingControllerResult.UsbDumpExcerpt ->
            RootActionState(message = "Captured ${excerpt.length} chars from $source")
    }

    fun onEvent(event: UsbDebugUiEvent) {
        when (event) {
            UsbDebugUiEvent.OpenDeveloperOptions -> onOpenDeveloperOptions()
            is UsbDebugUiEvent.SelectFunction -> onSelectFunction(event.function)
            UsbDebugUiEvent.DiagnosticsToggle -> onDiagnosticsToggle()
            UsbDebugUiEvent.RunUsbDump -> onRunDump(DumpTarget.Usb)
            UsbDebugUiEvent.RunSerialServiceDump -> onRunDump(DumpTarget.SerialService)
            UsbDebugUiEvent.RunUsbDevicesDebugDump -> onRunDump(DumpTarget.DebugfsDevices)
        }
    }

    /**
     * Re-reads the live setting. Called on `ON_RESUME` by [UsbDebugScreen]
     * so returning from the Developer-options deep link reflects a value
     * the user just changed there.
     */
    fun refreshUsbDebuggingEnabled() {
        _state.update { it.copy(usbDebuggingEnabled = readUsbDebuggingEnabled()) }
    }

    private fun readUsbDebuggingEnabled(): Boolean =
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    private fun onOpenDeveloperOptions() {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun onDiagnosticsToggle() {
        _state.update { it.copy(diagnosticsExpanded = !it.diagnosticsExpanded) }
    }

    private fun onSelectFunction(function: UsbFunctionType) {
        viewModelScope.launch {
            _state.update { it.copy(functionSwitchInFlight = true, functionSwitchError = null) }
            when (val result = controller.switchUsbFunction(function)) {
                is UsbDebuggingControllerResult.UsbFunctionSnapshot -> _state.update {
                    it.copy(
                        functionSwitchInFlight = false,
                        appliedFunction = result.appliedFunction,
                        priorFunction = result.priorFunction,
                    )
                }
                else -> _state.update {
                    it.copy(functionSwitchInFlight = false, functionSwitchError = result.describeError())
                }
            }
        }
    }

    private fun onRunDump(target: DumpTarget) {
        viewModelScope.launch {
            updateDump(target) { it.copy(loading = true, error = null) }
            val result = when (target) {
                DumpTarget.Usb -> controller.dumpUsb()
                DumpTarget.SerialService -> controller.dumpSerialService()
                DumpTarget.DebugfsDevices -> controller.dumpUsbDevicesDebug()
            }
            when (result) {
                is UsbDebuggingControllerResult.UsbDumpExcerpt -> updateDump(target) {
                    it.copy(loading = false, excerpt = result.excerpt, source = result.source)
                }
                else -> updateDump(target) {
                    it.copy(loading = false, error = result.describeError())
                }
            }
        }
    }

    private fun updateDump(target: DumpTarget, transform: (UsbDumpPanelState) -> UsbDumpPanelState) {
        _state.update { current ->
            when (target) {
                DumpTarget.Usb -> current.copy(usbDump = transform(current.usbDump))
                DumpTarget.SerialService -> current.copy(serialServiceDump = transform(current.serialServiceDump))
                DumpTarget.DebugfsDevices -> current.copy(debugfsDump = transform(current.debugfsDump))
            }
        }
    }

    private fun UsbDebuggingControllerResult.describeError(): String = when (this) {
        UsbDebuggingControllerResult.Unsupported -> context.getString(R.string.usbdebug_error_unsupported)
        UsbDebuggingControllerResult.OptedOut -> context.getString(R.string.usbdebug_error_opted_out)
        is UsbDebuggingControllerResult.RateLimited ->
            context.getString(R.string.usbdebug_error_rate_limited, retryAfterMillis)
        is UsbDebuggingControllerResult.HardwareError -> message
        is UsbDebuggingControllerResult.Ok,
        is UsbDebuggingControllerResult.ResetCompleted,
        is UsbDebuggingControllerResult.UsbFunctionSnapshot,
        is UsbDebuggingControllerResult.UsbDumpExcerpt,
        -> context.getString(R.string.usbdebug_error_generic)
    }

    private enum class DumpTarget { Usb, SerialService, DebugfsDevices }
}
