package dev.ranzlappen.gadget.feature.dashboard

import dev.ranzlappen.gadget.core.datastore.DashboardLayout
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.feature.dashboard.DashboardViewModel.Companion.resolveEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardResolutionTest {

    private val catalog: List<GadgetDestination> = GadgetDestination.modules
    private val routes: List<String> = catalog.map { it.route }

    @Test
    fun `empty layout yields the full catalog in natural order`() {
        val entries = resolveEntries(catalog, DashboardLayout())
        assertEquals(routes, entries.map { it.destination.route })
        assertTrue(entries.none { it.hidden || it.pinned })
    }

    @Test
    fun `saved order is honoured and new routes append in catalog order`() {
        // Only two routes saved, reversed; the rest must append in catalog order.
        val saved = listOf(routes[1], routes[0])
        val entries = resolveEntries(catalog, DashboardLayout(order = saved))
        assertEquals(routes[1], entries[0].destination.route)
        assertEquals(routes[0], entries[1].destination.route)
        // Remaining routes follow in the original catalog order.
        assertEquals(routes.drop(2), entries.drop(2).map { it.destination.route })
    }

    @Test
    fun `unknown saved routes are ignored`() {
        val entries = resolveEntries(catalog, DashboardLayout(order = listOf("not_a_real_route", routes[0])))
        assertEquals(catalog.size, entries.size)
        assertEquals(routes[0], entries[0].destination.route)
    }

    @Test
    fun `hidden and pinned flags are applied by route`() {
        val entries = resolveEntries(
            catalog,
            DashboardLayout(hidden = setOf(routes[0]), pinned = setOf(routes[1])),
        )
        assertTrue(entries.first { it.destination.route == routes[0] }.hidden)
        assertTrue(entries.first { it.destination.route == routes[1] }.pinned)
    }

    @Test
    fun `visible floats pinned first and drops hidden`() {
        val state = DashboardUiState(
            resolveEntries(
                catalog,
                DashboardLayout(hidden = setOf(routes[0]), pinned = setOf(routes[2])),
            ),
        )
        assertEquals(routes[2], state.visible.first().destination.route)
        assertFalse(state.visible.any { it.destination.route == routes[0] })
        assertEquals(catalog.size - 1, state.visible.size)
    }
}
