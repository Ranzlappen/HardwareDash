package dev.ranzlappen.gadget.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
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

    /**
     * Torch / Flashlight feature module. A first-class rail entry in
     * the scrollable [modules] region. Also reachable from QS tile /
     * home-screen widget interactions (those drive the controller
     * directly, not this route) and from a deep-link / notification
     * action.
     *
     * Torch is the reference implementation of the module blueprint —
     * it ships a `ModuleInfo` (permissions / OS-compatibility /
     * firmware) consumed by `ModuleScreenScaffold`.
     */
    data object Torch : GadgetDestination {
        override val route = "torch"
        override val label = "Torch"
        override val iconFilled = Icons.Filled.FlashlightOn
        override val iconOutlined = Icons.Outlined.FlashlightOn
    }

    /**
     * Vibration / haptics feature module — the second real module after
     * [Torch], a first-class entry in the scrollable [modules] region. Ships
     * the full module blueprint (ModuleInfo + monitoring + automation + a
     * 4-widget surface + a rooted extreme-tier).
     */
    data object Vibration : GadgetDestination {
        override val route = "vibration"
        override val label = "Vibration"
        override val iconFilled = Icons.Filled.Vibration
        override val iconOutlined = Icons.Outlined.Vibration
    }

    /**
     * App-Organizer feature module — folders of installed apps / PWAs / web
     * links that double as designable home-screen folder widgets. Migrated out
     * of the legacy monolith; a first-class entry in the scrollable [modules]
     * region. Owns a top-level folder grid + a per-folder editor sub-route.
     */
    data object Apps : GadgetDestination {
        override val route = "apps"
        override val label = "Apps"
        override val iconFilled = Icons.Filled.Apps
        override val iconOutlined = Icons.Outlined.Apps
    }

    /**
     * Battery feature module — level, charging state, temperature, voltage,
     * and health readouts from [android.os.BatteryManager] broadcasts. Live
     * and history monitoring via the shared monitoring framework. The rooted
     * extreme-tier (fuel-gauge, cell monitor, charging-profile override) ships
     * separately as `:feature:battery-rooted`.
     */
    data object Battery : GadgetDestination {
        override val route = "battery"
        override val label = "Battery"
        override val iconFilled = Icons.Filled.BatteryFull
        override val iconOutlined = Icons.Outlined.BatteryFull
    }

    companion object {
        /**
         * Destinations pinned to the **top** of the rail, above the
         * scrollable [modules] region. Index 0 is the canonical start
         * destination ([Dashboard]).
         */
        val pinnedTop: List<GadgetDestination> = listOf(Dashboard)

        /**
         * Feature modules shown in the rail's **scrollable middle**
         * region between [pinnedTop] and [pinnedBottom]. Each entry
         * gets a rail button — a module is never dashboard-only.
         *
         * Real modules replace the abstract placeholder areas as they
         * land: [Torch], [Vibration], [Apps], [Sensors], and
         * [Automation] (the rules list + builder) are live;
         * [Actuators] remains a coming-soon placeholder until its
         * feature module ships. Append new / legacy-migrated modules
         * here.
         */
        val modules: List<GadgetDestination> = listOf(
            Torch, Vibration, Apps, Sensors, Battery, Actuators, Automation,
        )

        /**
         * Destinations pinned to the **bottom** of the rail, below the
         * scrollable [modules] region.
         */
        val pinnedBottom: List<GadgetDestination> = listOf(Settings)

        /**
         * Every destination that owns a rail button, in render order
         * (pinned-top → modules → pinned-bottom). Used for selection
         * highlighting and back-stack-trimming navigation decisions.
         */
        val railDestinations: List<GadgetDestination> = pinnedTop + modules + pinnedBottom

        /**
         * Look up a [GadgetDestination] by its route string, or
         * `null` if the route doesn't match a known rail entry. Used
         * to decide which rail item is selected.
         */
        fun byRouteOrNull(route: String?): GadgetDestination? =
            route?.let { r -> railDestinations.firstOrNull { it.route == r } }
    }
}
