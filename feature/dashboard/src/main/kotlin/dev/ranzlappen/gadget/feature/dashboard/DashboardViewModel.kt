package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ranzlappen.gadget.core.datastore.DashboardLayout
import dev.ranzlappen.gadget.core.datastore.UserPreferencesRepository
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One dashboard module entry resolved against the user's saved layout. */
@Immutable
data class DashboardEntry(
    val destination: GadgetDestination,
    val hidden: Boolean,
    val pinned: Boolean,
)

/**
 * Dashboard render state. [entries] is the full catalog in the user's saved
 * order (what the editor arranges); [visible] is what the dashboard paints —
 * pinned entries floated to the top, hidden entries dropped.
 */
@Immutable
data class DashboardUiState(
    val entries: List<DashboardEntry> = emptyList(),
) {
    val visible: List<DashboardEntry>
        get() = (entries.filter { it.pinned } + entries.filter { !it.pinned })
            .filter { !it.hidden }
}

/**
 * Backs the dashboard (W9). Resolves the persisted [DashboardLayout] against
 * the [GadgetDestination.modules] catalog into an ordered list with per-entry
 * hidden/pinned flags, and exposes reorder / hide / pin / reset mutations that
 * persist through [UserPreferencesRepository]. A passthrough over the
 * repository, mirroring `SettingsViewModel`.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private val catalog: List<GadgetDestination> = GadgetDestination.modules

    val uiState: StateFlow<DashboardUiState> = repository.flow
        .map { DashboardUiState(resolveEntries(catalog, it.dashboardLayout)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_FLOW_TIMEOUT_MS),
            initialValue = DashboardUiState(resolveEntries(catalog, DashboardLayout())),
        )

    fun moveUp(route: String) = mutateOrder { entries ->
        val i = entries.indexOfFirst { it.destination.route == route }
        if (i <= 0) entries else entries.toMutableList().apply { add(i - 1, removeAt(i)) }
    }

    fun moveDown(route: String) = mutateOrder { entries ->
        val i = entries.indexOfFirst { it.destination.route == route }
        if (i < 0 || i >= entries.lastIndex) entries else entries.toMutableList().apply { add(i + 1, removeAt(i)) }
    }

    fun setHidden(route: String, hidden: Boolean) = mutateOrder { entries ->
        entries.map { if (it.destination.route == route) it.copy(hidden = hidden) else it }
    }

    fun setPinned(route: String, pinned: Boolean) = mutateOrder { entries ->
        entries.map { if (it.destination.route == route) it.copy(pinned = pinned) else it }
    }

    fun resetLayout() {
        viewModelScope.launch { repository.setDashboardLayout(DashboardLayout()) }
    }

    private fun mutateOrder(transform: (List<DashboardEntry>) -> List<DashboardEntry>) {
        viewModelScope.launch {
            repository.setDashboardLayout(layoutOf(transform(uiState.value.entries)))
        }
    }

    private fun layoutOf(entries: List<DashboardEntry>): DashboardLayout = DashboardLayout(
        order = entries.map { it.destination.route },
        hidden = entries.filter { it.hidden }.map { it.destination.route }.toSet(),
        pinned = entries.filter { it.pinned }.map { it.destination.route }.toSet(),
    )

    companion object {
        private const val STATE_FLOW_TIMEOUT_MS = 5_000L

        /**
         * Resolve [layout] against [catalog]: the saved order first (unknown
         * routes dropped), then any catalog routes not yet in the order
         * appended in catalog order, each tagged hidden/pinned. Pure so it's
         * unit-testable without a repository.
         */
        internal fun resolveEntries(
            catalog: List<GadgetDestination>,
            layout: DashboardLayout,
        ): List<DashboardEntry> {
            val known = catalog.associateBy { it.route }
            val ordered = (
                layout.order.filter { known.containsKey(it) } +
                    catalog.map { it.route }.filterNot { it in layout.order }
                ).distinct()
            return ordered.mapNotNull { route ->
                known[route]?.let {
                    DashboardEntry(it, hidden = route in layout.hidden, pinned = route in layout.pinned)
                }
            }
        }
    }
}
