package dev.ranzlappen.gadget.feature.motion

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination

/**
 * Wire :feature:motion into the Gadget NavGraph. Call from the
 * `GadgetApp { … }` builder in :app.
 *
 * Registers at [GadgetDestination.Motion]'s route so the rail's
 * `navigateTopLevel` and this composable can never drift apart —
 * a rename of either is a compile-time event, not a dead rail item.
 */
fun NavGraphBuilder.motionScreen() {
    composable(route = GadgetDestination.Motion.route) { MotionScreen() }
}
