package dev.ranzlappen.gadget.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level routes in the Gadget app.
 *
 * String-based routes for now — Navigation Compose 2.7.6 (the version
 * pinned in the catalog) predates the typesafe-route API that landed
 * in 2.8. Encapsulated behind a sealed interface so:
 *
 *   * The literal route string lives in exactly one place per route.
 *   * Per-destination metadata (label, filled/outlined icons) travels
 *     with the route — no parallel string-to-icon map to keep in sync.
 *   * Migrating to typesafe routes will be a cosmetic refactor when
 *     the catalog bumps Navigation Compose to 2.8+.
 *
 * Top-level destinations only. Sub-routes (e.g. "/sensors/{id}") will
 * be modelled as nested sealed entries on each top-level destination
 * as they're added in later batches.
 */
@Immutable
sealed interface GadgetDestination {
    /** The route string registered with NavHost. */
    val route: String

    /** Human-readable label, shown in the nav bar / rail. */
    val label: String

    /** Icon when the destination is selected (filled M3 variant). */
    val iconFilled: ImageVector

    /** Icon when the destination is unselected (outlined M3 variant). */
    val iconOutlined: ImageVector

    data object Dashboard : GadgetDestination {
        override val route = "dashboard"
        override val label = "Dashboard"
        override val iconFilled = Icons.Filled.Dashboard
        override val iconOutlined = Icons.Outlined.Dashboard
    }

    data object Sensors : GadgetDestination {
        override val route = "sensors"
        override val label = "Sensors"
        override val iconFilled = Icons.Filled.Sensors
        override val iconOutlined = Icons.Outlined.Sensors
    }

    data object Actuators : GadgetDestination {
        override val route = "actuators"
        override val label = "Actuators"
        override val iconFilled = Icons.Filled.Tune
        override val iconOutlined = Icons.Outlined.Tune
    }

    data object Automation : GadgetDestination {
        override val route = "automation"
        override val label = "Automation"
        override val iconFilled = Icons.Filled.Bolt
        override val iconOutlined = Icons.Outlined.Bolt
    }

    data object Settings : GadgetDestination {
        override val route = "settings"
        override val label = "Settings"
        override val iconFilled = Icons.Filled.Settings
        override val iconOutlined = Icons.Outlined.Settings
    }

    companion object {
        /**
         * Order of top-level destinations as they appear in the
         * navigation bar / rail. Index 0 is the canonical start
         * destination ([Dashboard]).
         */
        val topLevel: List<GadgetDestination> = listOf(
            Dashboard, Sensors, Actuators, Automation, Settings,
        )

        /**
         * Look up a [GadgetDestination] by its route string, or
         * `null` if the route doesn't match a known top-level entry.
         * Used by the bottom nav to decide which item is selected.
         */
        fun byRouteOrNull(route: String?): GadgetDestination? =
            route?.let { r -> topLevel.firstOrNull { it.route == r } }
    }
}
