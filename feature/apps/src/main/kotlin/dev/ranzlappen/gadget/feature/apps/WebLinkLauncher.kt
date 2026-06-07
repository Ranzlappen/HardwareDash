package dev.ranzlappen.gadget.feature.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens a user-defined web-link "app" in whichever browser/app the system is
 * configured to handle the URL. Web-link apps live in folders alongside real
 * launcher apps and use this dispatcher when tapped.
 */
@Singleton
class WebLinkLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Returns true if an Activity was successfully started. */
    fun launch(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Timber.w(t, "WebLinkLauncher: failed to launch %s", url)
            false
        }
    }
}
