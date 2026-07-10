package dev.ranzlappen.gadget.feature.adbdebug

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkPortRange
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbSetPropAllowList

/**
 * UI state for [AdbDebugScreen]. [adbEnabled] is the single ground-truth
 * readout of `Settings.Global.ADB_ENABLED` (readable on every flavor, no
 * special permission) — the standard tier's status card and the rooted
 * tier's toggle both render off this one field rather than tracking a
 * separate optimistic rooted value.
 */
@Immutable
data class AdbDebugState(
    val isRootedFlavor: Boolean = false,
    val adbEnabled: Boolean = false,
    val networkEnabled: Boolean = false,
    val networkPortText: String = AdbNetworkPortRange.DEFAULT.toString(),
    val persistDumpToStorage: Boolean = false,
    val lastDumpExcerpt: String? = null,
    val lastDumpPersistedPath: String? = null,
    val setPropKey: String = AdbSetPropAllowList.EXACT_KEYS.first(),
    val setPropUsingLogTag: Boolean = false,
    val setPropLogTagSuffix: String = "",
    val setPropValue: String = "",
    val busy: Boolean = false,
    val lastActionMessage: String? = null,
) {
    /** The key that would actually be submitted by [AdbDebugUiEvent.ApplySetProp]. */
    val resolvedSetPropKey: String
        get() = if (setPropUsingLogTag) {
            AdbSetPropAllowList.LOG_TAG_PREFIX + setPropLogTagSuffix.trim()
        } else {
            setPropKey
        }

    companion object {
        val Initial = AdbDebugState()
    }
}

/** UI-originated events dispatched from [AdbDebugScreenContent] to the ViewModel. */
sealed interface AdbDebugUiEvent {
    data class ToggleAdbEnabled(val enabled: Boolean) : AdbDebugUiEvent
    data class NetworkEnabledChange(val enabled: Boolean) : AdbDebugUiEvent
    data class NetworkPortChange(val portText: String) : AdbDebugUiEvent
    data object ApplyNetworkSettings : AdbDebugUiEvent
    data class PersistDumpChange(val persist: Boolean) : AdbDebugUiEvent
    data object DumpProperties : AdbDebugUiEvent
    data class SetPropKeyChange(val key: String) : AdbDebugUiEvent
    data object SetPropUseLogTag : AdbDebugUiEvent
    data class SetPropLogTagSuffixChange(val suffix: String) : AdbDebugUiEvent
    data class SetPropValueChange(val value: String) : AdbDebugUiEvent
    data object ApplySetProp : AdbDebugUiEvent
}
