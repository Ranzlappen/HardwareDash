package dev.ranzlappen.gadget.feature.radios.cell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton wrapper around [TelephonyManager] backing the standard-tier
 * Cellular screen: SIM state, carrier name, a readable network-type label,
 * and 0-4 "bars" signal level. Mirrors `GpsLocationTracker`'s
 * start/stop-on-permission shape — call [startTracking] once
 * `READ_PHONE_STATE` is granted, [stopTracking] when the screen leaves
 * composition or the permission is revoked.
 *
 * **Signal + network-type listener.** API 31+ registers a
 * [TelephonyCallback] (`SignalStrengthsListener` + `DisplayInfoListener`,
 * the latter giving the accurate "5G" / "5G+" override type via
 * [TelephonyDisplayInfo]). This module's minSdk is 29 (see
 * `build-logic/convention/.../KotlinAndroid.kt`), so API 29-30 falls back
 * to the deprecated [PhoneStateListener] (`LISTEN_SIGNAL_STRENGTHS` +
 * `LISTEN_SERVICE_STATE`), reading [TelephonyManager.getDataNetworkType]
 * directly since [TelephonyDisplayInfo] doesn't exist pre-31.
 *
 * **Multi-SIM.** Defaults to [SubscriptionManager.getDefaultSubscriptionId]
 * and binds a subscription-scoped [TelephonyManager] via
 * [TelephonyManager.createForSubscriptionId] when a default subscription is
 * present. No multi-SIM picker UI — out of scope for this pass (see the
 * standard-tier implementation brief).
 */
@Singleton
class CellTelephonyTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val baseTelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    /** Subscription-scoped when a default SIM is present; else the base instance. */
    private val telephonyManager: TelephonyManager = run {
        val subId = SubscriptionManager.getDefaultSubscriptionId()
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            runCatching { baseTelephonyManager.createForSubscriptionId(subId) }
                .getOrDefault(baseTelephonyManager)
        } else {
            baseTelephonyManager
        }
    }

    private val _state = MutableStateFlow(CellState())
    val state: StateFlow<CellState> = _state.asStateFlow()

    private var tracking = false
    private var telephonyCallback: TelephonyCallback? = null
    private var legacyListener: PhoneStateListener? = null

    fun startTracking() {
        if (tracking) return
        if (!hasPermission()) {
            _state.update { CellState(permissionGranted = false) }
            return
        }
        tracking = true
        refreshStaticFields()
        registerListener()
    }

    fun stopTracking() {
        if (!tracking) return
        tracking = false
        unregisterListener()
        _state.update { it.copy(permissionGranted = false) }
    }

    private fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE,
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * SIM state needs no permission at all; carrier name
     * ([TelephonyManager.getNetworkOperatorName]) is also permission-free.
     * Read once on start (re-invoked on every `ON_RESUME` via the screen's
     * permission-refresh flow calling [startTracking] again — a no-op past
     * the first call because of the [tracking] guard, so this only runs
     * once per tracking session, which is fine: both values are static for
     * the life of a SIM insertion).
     */
    private fun refreshStaticFields() {
        _state.update {
            it.copy(
                permissionGranted = true,
                simState = baseTelephonyManager.simState.toSimStateUi(),
                carrierName = telephonyManager.networkOperatorName?.takeIf { name -> name.isNotBlank() },
                networkType = networkTypeFromRaw(telephonyManager.dataNetworkType),
            )
        }
    }

    private fun registerListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = CellTelephonyCallback()
            telephonyCallback = callback
            val registered = runCatching {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
            }
            if (registered.isFailure) {
                telephonyCallback = null
                tracking = false
            }
        } else {
            val listener = LegacyPhoneStateListener()
            legacyListener = listener
            val registered = runCatching {
                @Suppress("DEPRECATION")
                telephonyManager.listen(
                    listener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS or PhoneStateListener.LISTEN_SERVICE_STATE,
                )
            }
            if (registered.isFailure) {
                legacyListener = null
                tracking = false
            }
        }
    }

    private fun unregisterListener() {
        telephonyCallback?.let { callback ->
            runCatching { telephonyManager.unregisterTelephonyCallback(callback) }
        }
        telephonyCallback = null
        legacyListener?.let { listener ->
            @Suppress("DEPRECATION")
            runCatching { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }
        legacyListener = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class CellTelephonyCallback :
        TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.DisplayInfoListener {

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            _state.update { it.copy(signalLevel = signalStrength.level) }
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            _state.update { it.copy(networkType = telephonyDisplayInfo.toCellNetworkType()) }
        }
    }

    @Suppress("DEPRECATION")
    private inner class LegacyPhoneStateListener : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
            _state.update { it.copy(signalLevel = signalStrength.level) }
        }

        override fun onServiceStateChanged(serviceState: ServiceState) {
            _state.update { it.copy(networkType = networkTypeFromRaw(telephonyManager.dataNetworkType)) }
        }
    }
}

private fun Int.toSimStateUi(): SimStateUi = when (this) {
    TelephonyManager.SIM_STATE_ABSENT -> SimStateUi.Absent
    TelephonyManager.SIM_STATE_PIN_REQUIRED,
    TelephonyManager.SIM_STATE_PUK_REQUIRED,
    TelephonyManager.SIM_STATE_PERM_DISABLED,
    -> SimStateUi.Locked
    TelephonyManager.SIM_STATE_NETWORK_LOCKED -> SimStateUi.NetworkLocked
    TelephonyManager.SIM_STATE_NOT_READY -> SimStateUi.NotReady
    TelephonyManager.SIM_STATE_READY,
    TelephonyManager.SIM_STATE_LOADED,
    -> SimStateUi.Ready
    else -> SimStateUi.Unknown
}

/** API 31+ path: the display-info override type is what the OS status bar
 * actually shows as "5G" / "5G+", more accurate than the base radio type. */
@RequiresApi(Build.VERSION_CODES.S)
private fun TelephonyDisplayInfo.toCellNetworkType(): CellNetworkType = when (overrideNetworkType) {
    // mmWave NSA is the fastest 5G tier available (what carriers commonly
    // badge "5G+" / "5G UW"); non-mmWave NSA and standalone NR both read as
    // plain "5G". Deliberately sticks to the two OVERRIDE_NETWORK_TYPE_NR_*
    // constants that have shipped since TelephonyDisplayInfo's introduction
    // in API 30, rather than the API 33-only OVERRIDE_NETWORK_TYPE_NR_ADVANCED.
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE -> CellNetworkType.Nr5GPlus
    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> CellNetworkType.Nr5G
    else -> networkTypeFromRaw(networkType)
}

/** API 29-30 fallback (and the pre-first-callback default on API 31+):
 * [TelephonyManager.NETWORK_TYPE_*] bucketed into a readable generation. */
private fun networkTypeFromRaw(type: Int): CellNetworkType = when (type) {
    TelephonyManager.NETWORK_TYPE_NR -> CellNetworkType.Nr5G
    TelephonyManager.NETWORK_TYPE_LTE -> CellNetworkType.Lte4G
    TelephonyManager.NETWORK_TYPE_HSPAP,
    TelephonyManager.NETWORK_TYPE_HSPA,
    TelephonyManager.NETWORK_TYPE_HSUPA,
    TelephonyManager.NETWORK_TYPE_HSDPA,
    TelephonyManager.NETWORK_TYPE_UMTS,
    TelephonyManager.NETWORK_TYPE_EHRPD,
    TelephonyManager.NETWORK_TYPE_EVDO_0,
    TelephonyManager.NETWORK_TYPE_EVDO_A,
    TelephonyManager.NETWORK_TYPE_EVDO_B,
    -> CellNetworkType.Umts3G
    TelephonyManager.NETWORK_TYPE_GPRS,
    TelephonyManager.NETWORK_TYPE_EDGE,
    TelephonyManager.NETWORK_TYPE_CDMA,
    TelephonyManager.NETWORK_TYPE_1xRTT,
    TelephonyManager.NETWORK_TYPE_IDEN,
    TelephonyManager.NETWORK_TYPE_GSM,
    -> CellNetworkType.Gsm2G
    else -> CellNetworkType.Unknown
}
