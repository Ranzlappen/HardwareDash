package dev.ranzlappen.gadget.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GadgetSectionTest {

    @Test
    fun `every destination belongs to exactly one section`() {
        GadgetDestination.railDestinations.forEach { destination ->
            val owners = GadgetSection.entries.filter { it.owns(destination.route) }
            assertEquals("${destination.route} owners: $owners", 1, owners.size)
        }
    }

    @Test
    fun `dashboard is the home section root`() {
        assertEquals(GadgetSection.Home, GadgetSection.forRoute(GadgetDestination.Dashboard.route))
        assertEquals(GadgetDestination.Dashboard.route, GadgetSection.Home.route)
    }

    @Test
    fun `hub routes are unique and never shadow a destination`() {
        val hubRoutes = GadgetSection.entries.filter { it.hasHub }.map { it.route }
        assertEquals(hubRoutes.size, hubRoutes.toSet().size)
        val destinationRoutes = GadgetDestination.railDestinations.map { it.route }.toSet()
        assertTrue(hubRoutes.none { it in destinationRoutes })
    }

    @Test
    fun `sub-routes resolve to their tool's section`() {
        assertEquals(GadgetSection.Tools, GadgetSection.forRoute("apps/editor/{folderId}"))
        assertEquals(GadgetSection.Tools, GadgetSection.forRoute("apps/editor/42"))
    }

    @Test
    fun `unknown and null routes resolve to no section`() {
        assertNull(GadgetSection.forRoute(null))
        assertNull(GadgetSection.forRoute("does-not-exist"))
    }

    @Test
    fun `only home has no hub`() {
        assertEquals(listOf(GadgetSection.Home), GadgetSection.entries.filter { !it.hasHub })
    }
}
