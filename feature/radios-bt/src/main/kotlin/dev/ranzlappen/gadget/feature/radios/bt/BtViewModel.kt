package dev.ranzlappen.gadget.feature.radios.bt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class BtViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: BluetoothAdapterWrapper,
) : ViewModel() {

    private val _state = MutableStateFlow(BtState())
    val state: StateFlow<BtState> = _state

    init { refresh() }

    fun refresh() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _state.update {
            BtState(
                adapterAvailable = adapter.isAvailable(),
                adapterEnabled = adapter.isEnabled(),
                adapterName = if (hasPermission) adapter.name() else null,
                bondedDevices = if (hasPermission) adapter.bondedDevices() else emptyList(),
                permissionGranted = hasPermission,
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (granted) refresh()
    }
}
