package dev.ranzlappen.gadget.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The five top-level sections of the app shell (v3, MASTER-PLAN §5.1).
 *
 * Phones show them as a labelled bottom bar, wider windows as a labelled
 * rail. [Home] is the dashboard itself; every other section opens a hub
 * screen ([route]) listing its [members]. Tools are pushed on top of the
 * hub, so system / predictive back returns to it.
 *
 * Every [GadgetDestination.railDestinations] entry belongs to exactly one
 * section (pinned by `GadgetSectionTest`). When modules are merged in later
 * v3 batches, their destinations simply disappear from these lists.
 */
@Immutable
enum class GadgetSection(
    /** Route of the section root: the dashboard for [Home], a hub otherwise. */
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    /** Destinations listed on the hub, in display order. Empty for [Home]. */
    val members: List<GadgetDestination>,
) {
    Home(
        route = GadgetDestination.Dashboard.route,
        label = "Home",
        iconFilled = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home,
        members = emptyList(),
    ),
    Tools(
        route = "section/tools",
        label = "Tools",
        iconFilled = Icons.Filled.Build,
        iconOutlined = Icons.Outlined.Build,
        members = listOf(
            GadgetDestination.Torch,
            GadgetDestination.Vibration,
            GadgetDestination.Camera,
            GadgetDestination.Audio,
            GadgetDestination.Display,
            GadgetDestination.Apps,
            GadgetDestination.Logbook,
            // Merged into Audio in V3-2 (B7b).
            GadgetDestination.Microphone,
        ),
    ),
    Sensors(
        route = "section/sensors",
        label = "Sensors",
        iconFilled = Icons.Filled.Sensors,
        iconOutlined = Icons.Outlined.Sensors,
        members = listOf(
            GadgetDestination.Sensors,
            GadgetDestination.Battery,
            GadgetDestination.Storage,
            GadgetDestination.Gps,
        ),
    ),
    Connect(
        route = "section/connect",
        label = "Connect",
        iconFilled = Icons.Filled.SettingsInputAntenna,
        iconOutlined = Icons.Outlined.SettingsInputAntenna,
        members = listOf(
            GadgetDestination.RadiosWifi,
            GadgetDestination.RadiosBt,
            GadgetDestination.RadiosCell,
            GadgetDestination.RadiosNfc,
            GadgetDestination.RadiosIr,
            GadgetDestination.RadiosSubghz,
            GadgetDestination.Flipper,
        ),
    ),
    More(
        route = "section/more",
        label = "More",
        iconFilled = Icons.Filled.MoreHoriz,
        iconOutlined = Icons.Outlined.MoreHoriz,
        members = listOf(
            GadgetDestination.Automation,
            GadgetDestination.Notification,
            GadgetDestination.Lock,
            // Merged into one "Device & debug" tool in V3-2 (B8).
            GadgetDestination.Diagnostics,
            GadgetDestination.BugReport,
            GadgetDestination.AdbDebug,
            GadgetDestination.UsbDebug,
            // Kept, frozen (MASTER-PLAN §8.1).
            GadgetDestination.Youtubedownloader,
            GadgetDestination.Manual,
            GadgetDestination.Settings,
        ),
    ),
    ;

    /** `true` when this section has its own hub screen (all but [Home]). */
    val hasHub: Boolean get() = members.isNotEmpty()

    /** `true` when [route] is this section's root or one of its tools (incl. sub-routes). */
    fun owns(route: String): Boolean =
        route == this.route || members.any { member -> route.matchesDestination(member) }

    companion object {
        /**
         * The section that owns [route], or `null` for routes outside every
         * section (e.g. a screen registered without a destination). Sub-routes
         * such as `apps/editor/{id}` resolve to their parent tool's section.
         */
        fun forRoute(route: String?): GadgetSection? =
            route?.let { r -> entries.firstOrNull { it.owns(r) } }

        /** `true` when [route] is a section root (Home's dashboard or a hub) — no up arrow. */
        fun isRoot(route: String): Boolean = entries.any { it.route == route }

        private fun String.matchesDestination(destination: GadgetDestination): Boolean =
            this == destination.route ||
                startsWith(destination.route + "/") ||
                startsWith(destination.route + "?")
    }
}
