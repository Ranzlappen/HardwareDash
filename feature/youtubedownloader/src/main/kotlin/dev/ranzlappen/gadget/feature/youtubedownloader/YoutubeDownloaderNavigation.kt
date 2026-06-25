package dev.ranzlappen.gadget.feature.youtubedownloader

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.feature.youtubedownloader.cookies.CookieLoginScreen
import dev.ranzlappen.gadget.feature.youtubedownloader.cookies.CookieLoginViewModel

/** Sub-route for the in-app cookie-capture login WebView. */
const val YOUTUBEDOWNLOADER_LOGIN_ROUTE: String = "youtube_downloader/login"

/**
 * Wire `:feature:youtubedownloader` into the Gadget NavGraph: the main screen
 * plus the cookie-login sub-route. Call from the `GadgetApp { … }` builder in
 * `:app`, passing the shared [navController] so the screen can open the login
 * route and the login screen can pop back.
 */
fun NavGraphBuilder.youtubeDownloaderScreen(navController: NavController) {
    composable(route = GadgetDestination.Youtubedownloader.route) {
        YoutubeDownloaderScreen(
            onNavigateToLogin = { navController.navigate(YOUTUBEDOWNLOADER_LOGIN_ROUTE) },
        )
    }
    composable(route = YOUTUBEDOWNLOADER_LOGIN_ROUTE) {
        CookieLoginRoute(onClose = { navController.popBackStack() })
    }
}

@Composable
private fun CookieLoginRoute(
    onClose: () -> Unit,
    viewModel: CookieLoginViewModel = hiltViewModel(),
) {
    CookieLoginScreen(
        onCaptured = viewModel::saveCookies,
        onClose = onClose,
    )
}
