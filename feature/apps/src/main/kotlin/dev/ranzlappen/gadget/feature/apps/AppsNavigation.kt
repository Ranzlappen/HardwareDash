package dev.ranzlappen.gadget.feature.apps

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.feature.apps.ui.AppsScreen
import dev.ranzlappen.gadget.feature.apps.ui.FolderEditorScreen
import dev.ranzlappen.gadget.feature.apps.ui.FolderEditorViewModel

/**
 * Wire `:feature:apps`'s routes into the Gadget NavGraph.
 *
 * Two routes: the top-level folder grid ([GadgetDestination.Apps]) and the
 * per-folder editor sub-route (`apps/editor/{folderId}`). Call from the app's
 * `GadgetApp { … }` builder, passing the host `navController` so the grid can
 * push the editor and the editor can pop back:
 *
 * ```kotlin
 * GadgetApp { appsScreen(navController) }
 * ```
 *
 * Apps is a [GadgetDestination.modules] entry, so the rail renders its icon
 * and the route is reachable via `navigateTopLevel`.
 */
fun NavGraphBuilder.appsScreen(navController: NavController) {
    composable(route = GadgetDestination.Apps.route) {
        AppsScreen(onOpenFolder = { folderId -> navController.navigate(editorRoute(folderId)) })
    }
    composable(
        route = EDITOR_ROUTE,
        arguments = listOf(
            navArgument(FolderEditorViewModel.ARG_FOLDER_ID) { type = NavType.LongType },
        ),
    ) {
        FolderEditorScreen(onBack = { navController.popBackStack() })
    }
}

private const val EDITOR_ROUTE = "apps/editor/{${FolderEditorViewModel.ARG_FOLDER_ID}}"

private fun editorRoute(folderId: Long): String = "apps/editor/$folderId"
