package dev.ranzlappen.gadget.feature.lock

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LockViewModel @Inject constructor(
    monitor: LockMonitor,
    rootCapabilityRegistry: RootCapabilityRegistry,
    private val deviceLockController: DeviceLockController,
) : ViewModel() {

    private val isRootedFlavor = rootCapabilityRegistry.isRootedFlavor

    val state: StateFlow<LockState> = monitor.state
        .map { it.copy(isRootedFlavor = isRootedFlavor) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LockState(isRootedFlavor = isRootedFlavor),
        )

    private val _lockAction = MutableStateFlow(RootActionState())

    /** Last-run status of the device-admin "lock now" action. */
    val lockAction: StateFlow<RootActionState> = _lockAction.asStateFlow()

    /** True once the user has activated the force-lock device admin. */
    fun isDeviceAdminActive(): Boolean = deviceLockController.isAdminActive()

    /** The system "activate device admin" intent, with a why-we-want-it string. */
    fun adminActivationIntent(explanation: String): Intent =
        deviceLockController.adminActivationIntent(explanation)

    /** Lock the screen now (device-admin, no root); confirm-gated by the UI. */
    fun onLockNow() {
        viewModelScope.launch {
            _lockAction.update { it.copy(running = true) }
            _lockAction.update { deviceLockController.lockNow().toActionState() }
        }
    }

    private fun DeviceLockResult.toActionState(): RootActionState = when (this) {
        DeviceLockResult.Ok -> RootActionState(message = "Device locked")
        DeviceLockResult.NotAdmin ->
            RootActionState(message = "Enable device admin first", isError = true)
        is DeviceLockResult.Error -> RootActionState(message = message, isError = true)
    }
}
