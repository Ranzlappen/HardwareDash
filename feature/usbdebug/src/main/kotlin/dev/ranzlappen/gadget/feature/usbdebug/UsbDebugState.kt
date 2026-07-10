package dev.ranzlappen.gadget.feature.usbdebug

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType

/**
 * Stateless view-state container consumed by [UsbDebugScreenContent].
 *
 * Produced by [UsbDebugViewModel.state] from two sources: the live
 * `Settings.Global.ADB_ENABLED` read (standard + rooted, no privileged
 * shell needed) and the rooted [dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController]
 * ([appliedFunction] / dump panel state, standard-flavor no-op).
 */
@Immutable
data class UsbDebugState(
    val isRootedFlavor: Boolean = false,
    /** Mirrors `Settings.Global.ADB_ENABLED` — Android has no separate
     *  "USB debugging" setting distinct from ADB access. */
    val usbDebuggingEnabled: Boolean = false,
    /** The function role last successfully applied via [UsbFunctionType]
     *  this session; `null` until the user switches one. */
    val appliedFunction: UsbFunctionType? = null,
    /** Raw pre-mutation `cmd usb get-functions` snapshot, echoed back by
     *  the controller alongside [appliedFunction]. */
    val priorFunction: String? = null,
    val functionSwitchInFlight: Boolean = false,
    val functionSwitchError: String? = null,
    val usbDump: UsbDumpPanelState = UsbDumpPanelState(),
    val serialServiceDump: UsbDumpPanelState = UsbDumpPanelState(),
    val debugfsDump: UsbDumpPanelState = UsbDumpPanelState(),
    /** Persisted-in-memory expand state for the "USB Diagnostics" card;
     *  ephemeral (not saved across process death) — mirrors the simplest
     *  [dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard] usage. */
    val diagnosticsExpanded: Boolean = false,
) {
    companion object {
        val Initial = UsbDebugState()
    }
}

/**
 * Per-probe panel state for one of the three read-only USB dumps
 * ([UsbDebugState.usbDump] / [UsbDebugState.serialServiceDump] /
 * [UsbDebugState.debugfsDump]). [source] is the controller's
 * `UsbDumpExcerpt.source` string — the sub-section's own label already
 * says which command was *requested*; [source] reports which probe
 * actually *answered* (relevant for the debugfs dump, which silently
 * falls back to `dumpsys usb` when debugfs isn't mounted).
 */
@Immutable
data class UsbDumpPanelState(
    val loading: Boolean = false,
    val excerpt: String? = null,
    val source: String? = null,
    val error: String? = null,
)
