package dev.ranzlappen.gadget.feature.adbdebug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkConfig
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkPortRange
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbSetPropAllowList
import dev.ranzlappen.gadget.feature.adbdebug.control.SetPropConfig
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Wires [AdbDebuggingController] (rooted write path, no-op on standard) +
 * a direct `Settings.Global.ADB_ENABLED` poll (readable on every flavor, the
 * standard-tier "debug-state readout" the design brief calls for) into a
 * single [AdbDebugState].
 */
@HiltViewModel
class AdbDebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: AdbDebuggingController,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val isRootedFlavor: Boolean = rootCapabilityRegistry.isRootedFlavor

    /** Live `Settings.Global.ADB_ENABLED` readout, polled — there is no
     *  broadcast for this setting (mirrors [AdbEnabledMetricSource]). */
    private val adbEnabled: StateFlow<Boolean> = flow {
        while (true) {
            emit(AdbEnabledMetricSource.isAdbEnabled(context))
            delay(POLL_INTERVAL_MS)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = AdbEnabledMetricSource.isAdbEnabled(context),
    )

    /** Everything the screen edits that isn't the live system readout above. */
    private val form = MutableStateFlow(FormState())

    val state: StateFlow<AdbDebugState> = combine(adbEnabled, form) { enabled, f ->
        AdbDebugState(
            isRootedFlavor = isRootedFlavor,
            adbEnabled = enabled,
            networkEnabled = f.networkEnabled,
            networkPortText = f.networkPortText,
            persistDumpToStorage = f.persistDumpToStorage,
            lastDumpExcerpt = f.lastDumpExcerpt,
            lastDumpPersistedPath = f.lastDumpPersistedPath,
            setPropKey = f.setPropKey,
            setPropUsingLogTag = f.setPropUsingLogTag,
            setPropLogTagSuffix = f.setPropLogTagSuffix,
            setPropValue = f.setPropValue,
            busy = f.busy,
            lastActionMessage = f.lastActionMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = AdbDebugState(isRootedFlavor = isRootedFlavor),
    )

    fun onEvent(event: AdbDebugUiEvent) {
        when (event) {
            is AdbDebugUiEvent.ToggleAdbEnabled -> runControllerCall {
                controller.toggleAdbEnabled(event.enabled)
            }
            is AdbDebugUiEvent.NetworkEnabledChange ->
                form.update { it.copy(networkEnabled = event.enabled) }
            is AdbDebugUiEvent.NetworkPortChange ->
                form.update { it.copy(networkPortText = event.portText.filter(Char::isDigit).take(MAX_PORT_DIGITS)) }
            AdbDebugUiEvent.ApplyNetworkSettings -> applyNetworkSettings()
            is AdbDebugUiEvent.PersistDumpChange ->
                form.update { it.copy(persistDumpToStorage = event.persist) }
            AdbDebugUiEvent.DumpProperties -> dumpProperties()
            is AdbDebugUiEvent.SetPropKeyChange ->
                form.update { it.copy(setPropKey = event.key, setPropUsingLogTag = false) }
            AdbDebugUiEvent.SetPropUseLogTag ->
                form.update { it.copy(setPropUsingLogTag = true) }
            is AdbDebugUiEvent.SetPropLogTagSuffixChange ->
                form.update { it.copy(setPropLogTagSuffix = event.suffix) }
            is AdbDebugUiEvent.SetPropValueChange ->
                form.update { it.copy(setPropValue = event.value) }
            AdbDebugUiEvent.ApplySetProp -> applySetProp()
        }
    }

    private fun applyNetworkSettings() {
        val current = form.value
        val port = current.networkPortText.toIntOrNull() ?: AdbNetworkPortRange.DEFAULT
        runControllerCall {
            controller.toggleAdbOverNetwork(AdbNetworkConfig(enabled = current.networkEnabled, port = port))
        }
    }

    private fun dumpProperties() {
        val persist = form.value.persistDumpToStorage
        viewModelScope.launch {
            form.update { it.copy(busy = true) }
            when (val result = controller.dumpProperties(persist = persist)) {
                is AdbDebuggingControllerResult.PropertyDump -> form.update {
                    it.copy(
                        busy = false,
                        lastDumpExcerpt = result.excerpt,
                        lastDumpPersistedPath = result.persistedFile,
                        lastActionMessage = describe(result),
                    )
                }
                else -> form.update { it.copy(busy = false, lastActionMessage = describe(result)) }
            }
        }
    }

    private fun applySetProp() {
        val current = form.value
        val key = current.resolvedKey()
        if (key.isBlank()) {
            form.update { it.copy(lastActionMessage = "missing property key") }
            return
        }
        runControllerCall {
            controller.overrideSystemProperty(SetPropConfig(key = key, value = current.setPropValue))
        }
    }

    private fun runControllerCall(block: suspend () -> AdbDebuggingControllerResult) {
        viewModelScope.launch {
            form.update { it.copy(busy = true) }
            val result = block()
            form.update { it.copy(busy = false, lastActionMessage = describe(result)) }
        }
    }

    private fun describe(result: AdbDebuggingControllerResult): String = when (result) {
        is AdbDebuggingControllerResult.Ok -> result.statusNote ?: "Done"
        AdbDebuggingControllerResult.Unsupported -> "Requires the rooted app version"
        is AdbDebuggingControllerResult.RateLimited -> "Rate-limited; retry in ${result.retryAfterMillis}ms"
        AdbDebuggingControllerResult.OptedOut -> "Turned off in Settings"
        is AdbDebuggingControllerResult.HardwareError -> result.message
        is AdbDebuggingControllerResult.ResetCompleted ->
            "Restored ${result.restored}, failed ${result.failed}"
        is AdbDebuggingControllerResult.AdbToggleSnapshot ->
            "ADB ${if (result.appliedEnabled) "enabled" else "disabled"}"
        is AdbDebuggingControllerResult.AdbNetworkSnapshot ->
            if (result.appliedPort != null) "ADB over network on port ${result.appliedPort}" else "ADB over network disabled"
        is AdbDebuggingControllerResult.PropertyDump ->
            if (result.persistedFile != null) "Saved to ${result.persistedFile}" else "Dump captured"
        is AdbDebuggingControllerResult.SetpropSnapshot ->
            "${result.key} = ${result.appliedValue}"
    }

    private fun FormState.resolvedKey(): String =
        if (setPropUsingLogTag) {
            AdbSetPropAllowList.LOG_TAG_PREFIX + setPropLogTagSuffix.trim()
        } else {
            setPropKey
        }

    private data class FormState(
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
    )

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val MAX_PORT_DIGITS = 5
    }
}
