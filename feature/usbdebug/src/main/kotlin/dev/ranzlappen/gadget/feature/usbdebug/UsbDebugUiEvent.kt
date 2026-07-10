package dev.ranzlappen.gadget.feature.usbdebug

import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType

/**
 * Every user-initiated event surfaced by [UsbDebugScreenContent].
 *
 * The screen flattens its public API to a single
 * `onEvent: (UsbDebugUiEvent) -> Unit`; [UsbDebugViewModel.onEvent] then
 * dispatches each variant to the matching typed handler. Mirrors
 * `TorchUiEvent` (the reference shape from the Module Authoring Contract).
 */
sealed interface UsbDebugUiEvent {
    /** Standard-tier deep-link to `Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS`. */
    data object OpenDeveloperOptions : UsbDebugUiEvent

    /** Rooted-tier USB function-role chip tap. */
    data class SelectFunction(val function: UsbFunctionType) : UsbDebugUiEvent

    /** Rooted-tier "USB Diagnostics" panel expand/collapse toggle. */
    data object DiagnosticsToggle : UsbDebugUiEvent

    /** Rooted-tier per-sub-section "Run" taps — one per dump probe. */
    data object RunUsbDump : UsbDebugUiEvent
    data object RunSerialServiceDump : UsbDebugUiEvent
    data object RunUsbDevicesDebugDump : UsbDebugUiEvent
}
