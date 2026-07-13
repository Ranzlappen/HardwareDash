package dev.ranzlappen.gadget.core.navigation

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetCustomTheme
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.adaptive.LocalWindowSizeClass

/**
 * Top-level Gadget app shell.
 *
 * Side-rail layout: [GadgetTheme] wraps a [Row] containing the icon-only
 * [GadgetNavRail] on the left and a [GadgetNavHost] body that takes the
 * remaining width. Feature modules contribute screens via the [builder]
 * block, exactly like vanilla [GadgetNavHost].
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
            // Row pads in from status bar / nav bar / display cutout so
            // the rail icons and the host content sit clear of the
            // (transparent, edge-to-edge) system bars. windowInsetsPadding
            // CONSUMES the insets it applies; the custom GadgetNavRail
            // adds no insets of its own, so there's no double padding in
            // landscape / 3-button-nav. The outer Surface stays full-bleed
            // so the theme background colour paints behind the transparent
            // bars.
            val shell: @Composable (showLabels: Boolean) -> Unit = { showLabels ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    GadgetNavRail(
                        navController = navController,
                        topAnchors = GadgetDestination.pinnedTop,
                        modules = GadgetDestination.modules,
                        bottomAnchors = GadgetDestination.pinnedBottom,
                        showLabels = showLabels,
                    )
                    GadgetNavHost(
                        modifier = Modifier.weight(1f),
                        navController = navController,
                        startDestination = startDestination,
                        builder = builder,
                    )
                }
            }
            if (activity != null) {
                val windowSizeClass = calculateWindowSizeClass(activity)
                // Show rail labels on Expanded widths (tablets,
                // chromebooks, foldable open). On Compact / Medium
                // the rail stays icon-only to maximise content area.
                // Compact landscape → bottom-bar collapse is a
                // Phase-2 refinement.
                val showLabels = windowSizeClass.widthSizeClass ==
                    androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    shell(showLabels)
                }
            } else {
                // No activity context — render the shell without a
                // WindowSizeClass provider. Downstream consumers that
                // read LocalWindowSizeClass.current will throw with the
                // explanatory error defined on the local. Rail stays
                // icon-only (showLabels = false) without size-class
                // info to derive labelling decision.
                shell(false)
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
        GadgetDestination.Actuators -> "Actuator controls coming in Phase 2"
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
