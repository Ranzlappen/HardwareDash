package dev.ranzlappen.gadget.feature.bugreport

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class BugReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    rootCapabilityRegistry: RootCapabilityRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(buildState(context, rootCapabilityRegistry.isRootedFlavor))
    val state: StateFlow<BugReportState> = _state

    fun refresh() {
        _state.value = buildState(context, _state.value.isRootedFlavor)
    }

    companion object {
        private fun buildState(context: Context, isRootedFlavor: Boolean): BugReportState {
            val perms = mutableListOf(
                Manifest.permission.CAMERA to R.string.bugreport_perm_camera,
                Manifest.permission.RECORD_AUDIO to R.string.bugreport_perm_microphone,
                Manifest.permission.ACCESS_FINE_LOCATION to R.string.bugreport_perm_location,
                Manifest.permission.READ_PHONE_STATE to R.string.bugreport_perm_phone,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms += Manifest.permission.BLUETOOTH_CONNECT to R.string.bugreport_perm_bluetooth
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += Manifest.permission.POST_NOTIFICATIONS to R.string.bugreport_perm_notifications
            }
            val entries = perms.map { (perm, label) ->
                PermissionEntry(
                    label = label,
                    permission = perm,
                    granted = ContextCompat.checkSelfPermission(context, perm) ==
                        PackageManager.PERMISSION_GRANTED,
                )
            }
            return BugReportState(permissions = entries, isRootedFlavor = isRootedFlavor)
        }
    }
}
