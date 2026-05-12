package dev.ranzlappen.gadget.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetMotion

/**
 * Top-level [NavHost] for Gadget.
 *
 * Per-route content is contributed by `NavGraphBuilder.<feature>Screen`
 * extension functions defined in each feature module. This file is the
 * orchestrator — it owns the host, the transitions, and the start
 * destination, and is intentionally feature-agnostic.
 *
 * Transitions are M3-Expressive: a quarter-width slide + fade, with
 * decelerated easing on enter and accelerated easing on exit. The
 * quarter slide distance is deliberately short — full-width slides
 * feel teleport-y between sibling top-level destinations.
 *
 * Most callers should use [GadgetAppShell] instead, which wraps this
 * NavHost in a [androidx.compose.material3.Scaffold] with the bottom
 * navigation bar. Call this directly only when you need a NavHost
 * without the surrounding shell (tests, embedded surfaces).
 */
@Composable
fun GadgetNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: GadgetDestination = GadgetDestination.Dashboard,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
        enterTransition = { slideAndFadeIn(SlideDirection.Start, GadgetMotion.EmphasizedDecelerate) },
        exitTransition = { slideAndFadeOut(SlideDirection.Start, GadgetMotion.EmphasizedAccelerate) },
        popEnterTransition = { slideAndFadeIn(SlideDirection.End, GadgetMotion.EmphasizedDecelerate) },
        popExitTransition = { slideAndFadeOut(SlideDirection.End, GadgetMotion.EmphasizedAccelerate) },
        builder = builder,
    )
}

/**
 * Navigate to a top-level destination, popping back to the start so
 * the back stack doesn't grow indefinitely from tab switching, and
 * restoring saved state when returning to a previously-visited tab.
 *
 * Used by [GadgetAppShell]'s bottom nav. Direct sub-route navigation
 * (e.g. dashboard → "/sensors/{id}") should call
 * [androidx.navigation.NavController.navigate] directly, not this
 * helper — sub-routes belong on the back stack.
 */
fun NavHostController.navigateTopLevel(destination: GadgetDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

// ─── Transition helpers ─────────────────────────────────────────────

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideAndFadeIn(
    direction: SlideDirection,
    easing: Easing,
): EnterTransition = slideIntoContainer(
    towards = direction,
    animationSpec = tween(durationMillis = GadgetMotion.DurationMedium, easing = easing),
    initialOffset = { it / 4 },
) + fadeIn(
    animationSpec = tween(durationMillis = GadgetMotion.DurationMedium, easing = easing),
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideAndFadeOut(
    direction: SlideDirection,
    easing: Easing,
): ExitTransition = slideOutOfContainer(
    towards = direction,
    animationSpec = tween(durationMillis = GadgetMotion.DurationMedium, easing = easing),
    targetOffset = { it / 4 },
) + fadeOut(
    animationSpec = tween(durationMillis = GadgetMotion.DurationMedium, easing = easing),
)
