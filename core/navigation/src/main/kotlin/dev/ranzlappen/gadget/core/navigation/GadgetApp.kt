package dev.ranzlappen.gadget.core.navigation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetCustomTheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.ui.LocalNavigateUp
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.adaptive.LocalWindowSizeClass
import dev.ranzlappen.gadget.core.ui.module.LocalShowExperimentalRootTools

/**
 * Top-level Gadget app shell.
 *
 * Five-section layout (v3): [GadgetTheme] wraps the [GadgetNavHost] plus
 * either a labelled [GadgetSectionBar] at the bottom (Compact width) or a
 * labelled [GadgetSectionRail] on the start edge (Medium / Expanded). Each
 * [GadgetSection] except Home gets a hub screen registered automatically
 * ([sectionHubScreens]); feature modules contribute their screens via the
 * [builder] block, exactly like vanilla [GadgetNavHost].
 *
 * Typical usage from `:app`'s `MainActivity`:
 *
 * ```kotlin
 * setContent {
 *     when (val resolved = outcome) {
 *         LaunchGateOutcome.Allowed -> GadgetApp {
 *             dashboardScreen(onNavigate = { /* … */ })
 *             placeholderScreen(GadgetDestination.Sensors)
 *             // …
 *         }
 *         // …
 *     }
 * }
 * ```
 *
 * Until every top-level destination has a real feature module, fill
 * the gaps with [placeholderScreen]; tapping a rail item that isn't
 * registered would otherwise crash NavHost.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun GadgetApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: GadgetDestination = GadgetDestination.Dashboard,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    customTheme: GadgetCustomTheme = GadgetCustomTheme.Default,
    customColorSchemeOverride: androidx.compose.material3.ColorScheme? = null,
    reducedMotionOverride: Boolean? = null,
    reducedTransparency: Boolean = false,
    // v3 B13: Settings → "Show experimental root tools" (rooted flavor only).
    showExperimentalRootTools: Boolean = false,
    builder: NavGraphBuilder.() -> Unit,
) {
    // Compute the WindowSizeClass once at the top of the shell and
    // propagate via LocalWindowSizeClass. calculateWindowSizeClass needs
    // an Activity — LocalContext provides one when GadgetApp is hosted
    // inside MainActivity.setContent. If the cast fails (e.g. a
    // headless / non-activity host), we skip the provider and any
    // downstream consumer that reads LocalWindowSizeClass.current will
    // throw — explicit beats silently rendering the wrong layout.
    val activity = LocalContext.current as? Activity
    GadgetTheme(
        customColorSchemeOverride = customColorSchemeOverride,
        useDarkTheme = useDarkTheme,
        useDynamicColor = useDynamicColor,
        customTheme = customTheme,
        reducedMotionOverride = reducedMotionOverride,
        reducedTransparency = reducedTransparency,
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            // The shell pads in from status bar / nav bar / display cutout so
            // the navigation chrome and the host content sit clear of the
            // (transparent, edge-to-edge) system bars. windowInsetsPadding
            // CONSUMES the insets it applies; the section bar is given zero
            // insets of its own, so there's no double padding. The outer
            // Surface stays full-bleed so the theme background colour paints
            // behind the transparent bars.
            //
            // v3 (MASTER-PLAN §5.1): five labelled sections. Compact width →
            // bottom bar; Medium / Expanded → rail on the start edge. The
            // NavHost keeps the same position in the composition for both
            // layouts (Row → Column → host), so a width change (fold / unfold,
            // multi-window) never recreates the graph or drops the back stack.
            val shell: @Composable (compact: Boolean) -> Unit = { compact ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    if (!compact) {
                        GadgetSectionRail(navController = navController)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        // Up arrow for every screen that isn't a section root
                        // and has something to go back to (read by
                        // ModuleScreenScaffold via LocalNavigateUp).
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val route = backStackEntry?.destination?.route
                        val canNavigateUp = route != null &&
                            !GadgetSection.isRoot(route) &&
                            navController.previousBackStackEntry != null
                        val navigateUp: (() -> Unit)? =
                            if (canNavigateUp) ({ navController.navigateUp() }) else null
                        CompositionLocalProvider(
                            LocalNavigateUp provides navigateUp,
                            LocalShowExperimentalRootTools provides showExperimentalRootTools,
                        ) {
                            GadgetNavHost(
                                modifier = Modifier.weight(1f),
                                navController = navController,
                                startDestination = startDestination,
                            ) {
                                sectionHubScreens(onOpen = { destination ->
                                    navController.navigate(destination.route)
                                })
                                builder()
                            }
                        }
                        if (compact) {
                            GadgetSectionBar(navController = navController)
                        }
                    }
                }
            }
            if (activity != null) {
                val windowSizeClass = calculateWindowSizeClass(activity)
                val compact = windowSizeClass.widthSizeClass ==
                    androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    shell(compact)
                }
            } else {
                // No activity context — render the phone layout without a
                // WindowSizeClass provider. Downstream consumers that read
                // LocalWindowSizeClass.current will throw with the
                // explanatory error defined on the local.
                shell(true)
            }
        }
    }
}

/**
 * Register a [GadgetDestination] with a "coming soon" placeholder
 * screen. Use for top-level routes whose real feature module hasn't
 * shipped yet so [GadgetApp]'s rail has somewhere to land.
 */
fun NavGraphBuilder.placeholderScreen(destination: GadgetDestination) {
    composable(route = destination.route) {
        ComingSoonScreen(destination)
    }
}

/**
 * "Coming soon" placeholder rendered via [ModuleScreenScaffold].
 *
 * Each placeholder destination gets its own short, module-appropriate
 * blurb in the scaffold's `functional` slot; the `moduleInfo` block
 * and `disclaimer` slot are deliberately left empty in Phase 1 and
 * will be populated as each real feature module replaces the
 * placeholder.
 *
 * Public so previews can render it in isolation; production callers
 * normally reach it via [placeholderScreen].
 */
@Composable
fun ComingSoonScreen(
    destination: GadgetDestination,
    modifier: Modifier = Modifier,
) {
    val message = when (destination) {
        GadgetDestination.Dashboard -> "Dashboard overview coming in Phase 2"
        GadgetDestination.Sensors -> "Sensor overview coming in Phase 2"
        GadgetDestination.Automation -> "Automation rules coming in Phase 2"
        GadgetDestination.Settings -> "Settings coming in Phase 2"
        GadgetDestination.Torch -> "Torch is wired to its own screen"
        GadgetDestination.Vibration -> "Vibration is wired to its own screen"
        GadgetDestination.Apps -> "Apps is wired to its own screen"
        else -> "${destination.label} is wired to its own screen"
    }
    ModuleScreenScaffold(
        title = destination.label,
        modifier = modifier,
        functional = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
