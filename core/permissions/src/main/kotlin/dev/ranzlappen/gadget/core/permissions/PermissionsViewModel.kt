package dev.ranzlappen.gadget.core.permissions

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** One rendered runtime-permission row in the dashboard. */
data class RuntimePermissionRow(
    val permission: RuntimePermission,
    val granted: Boolean,
)

/** One rendered special-permission row in the dashboard. */
data class SpecialPermissionRow(
    val special: SpecialPermission,
    val granted: Boolean,
)

/** A feature's rendered permission group. */
data class PermissionGroupUi(
    val featureId: String,
    val displayName: String,
    val runtime: List<RuntimePermissionRow>,
    val special: List<SpecialPermissionRow>,
)

/** The whole dashboard render state. */
data class PermissionsUiState(
    val groups: List<PermissionGroupUi> = emptyList(),
    val summary: GrantSummary = GrantSummary(0, 0),
)

/**
 * ViewModel for the Permissions dashboard: scans grant state through
 * [PermissionRegistry] and re-scans on demand ([refresh]) so the dashboard
 * reflects grants made in a system dialog or a Settings round-trip after the
 * user returns (the `ON_RESUME` re-scan pattern).
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val registry: PermissionRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(scan())
    val state: StateFlow<PermissionsUiState> = _state

    fun refresh() {
        _state.value = scan()
    }

    private fun scan(): PermissionsUiState {
        val groups = registry.featurePermissions().map { group ->
            PermissionGroupUi(
                featureId = group.featureId,
                displayName = group.displayName,
                runtime = group.runtime.map {
                    RuntimePermissionRow(it, registry.isRuntimeGranted(it.permission))
                },
                special = group.special.map {
                    SpecialPermissionRow(it, registry.isSpecialGranted(it))
                },
            )
        }
        return PermissionsUiState(groups = groups, summary = registry.grantSummary())
    }
}
