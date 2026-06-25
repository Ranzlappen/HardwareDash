package dev.ranzlappen.gadget.feature.youtubedownloader.cookies

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.feature.youtubedownloader.R

/**
 * In-app YouTube/Google sign-in. The user logs in inside a WebView; "Done"
 * snapshots the session cookies into a Netscape jar via [CookieCapture] and
 * hands them to [onCaptured]. Nothing leaves the device — the jar is written
 * to app-private storage for yt-dlp's `--cookies`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CookieLoginScreen(
    onCaptured: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val cookieManager = remember { CookieManager.getInstance() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.ytdl_login_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GadgetTertiaryButton(onClick = onClose, text = stringResource(R.string.ytdl_login_cancel))
            GadgetPrimaryButton(
                onClick = {
                    cookieManager.flush()
                    CookieCapture.toNetscape(cookieManager)?.let(onCaptured)
                    onClose()
                },
                text = stringResource(R.string.ytdl_login_done),
            )
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    loadUrl("https://accounts.google.com/ServiceLogin?continue=https://www.youtube.com/")
                }
            },
        )
    }
}
