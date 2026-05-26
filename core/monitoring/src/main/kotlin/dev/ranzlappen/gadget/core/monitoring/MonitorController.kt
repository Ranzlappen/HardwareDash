package dev.ranzlappen.gadget.core.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts [MonitorService] when a metric becomes enabled. The service
 * observes every metric's config and stops itself once none are enabled,
 * so callers only ever need to "ensure started" — they never stop it.
 */
@Singleton
class MonitorController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureStarted() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MonitorService::class.java),
        )
    }
}
