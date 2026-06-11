package dev.ranzlappen.gadget.core.automation.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only start path for [AutomationService] (mirrors `MonitorController`):
 * callers "ensure started" and never stop it — the service observes the rule
 * set and self-stops once no metric-stream rule is enabled.
 *
 * Call [ensureStarted] whenever a rule with a [dev.ranzlappen.gadget.core.automation.model.Trigger.MetricThreshold]
 * trigger becomes enabled (the rule-builder save path and the boot re-arm
 * wire this in later F-slices).
 */
@Singleton
class AutomationController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureStarted() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AutomationService::class.java),
        )
    }
}
