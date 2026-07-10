package dev.ranzlappen.gadget.feature.radios.cell

import androidx.compose.runtime.Immutable

/**
 * Standard-tier cellular readout, backed by [CellTelephonyTracker]. Mirrors
 * `GpsState` / `WifiState`'s shape: raw, presentation-agnostic values —
 * [CellScreen] resolves each field to a localized label via
 * `stringResource`.
 */
@Immutable
data class CellState(
    val permissionGranted: Boolean = false,
    val simState: SimStateUi = SimStateUi.Unknown,
    val carrierName: String? = null,
    val networkType: CellNetworkType = CellNetworkType.Unknown,
    /** [android.telephony.SignalStrength.getLevel] — 0 (none/unknown) .. 4 (great). */
    val signalLevel: Int = 0,
    val isRootedFlavor: Boolean = false,
)

/**
 * [android.telephony.TelephonyManager.getSimState] bucketed into the small
 * set of states the screen distinguishes. `getSimState()` requires no
 * runtime permission, so this is populated independently of
 * [CellState.permissionGranted].
 */
enum class SimStateUi { Unknown, Absent, Locked, NetworkLocked, NotReady, Ready }

/**
 * Readable network-generation bucket. Resolved from
 * [android.telephony.TelephonyDisplayInfo] (API 31+, via a
 * `TelephonyCallback.DisplayInfoListener`) for the more accurate "5G" /
 * "5G+" split, or from [android.telephony.TelephonyManager.getDataNetworkType]
 * on API 29-30 (this module's minSdk). Both paths require
 * `READ_PHONE_STATE`.
 */
enum class CellNetworkType { Unknown, Gsm2G, Umts3G, Lte4G, Nr5G, Nr5GPlus }
