package dev.ranzlappen.gadget.feature.adbdebug

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `adb_enabled` metric — 0/1 read from `Settings.Global.ADB_ENABLED`.
 *
 * Readable without any special permission on every flavor (the standard-
 * tier "debug-state readout" the module design brief calls for), so this
 * source works identically regardless of [dev.ranzlappen.gadget.core.root.RootCapabilityRegistry.isRootedFlavor] —
 * unlike the rest of the module's rooted-only write surface.
 *
 * Poll source (no [stream] override): there is no broadcast for a
 * `Settings.Global` change to this key, so the shared monitoring sampler
 * polls [sample] on its regular cadence — the same pattern
 * `NfcEnabledMetricSource` uses for `Settings.Global`-backed adapter state.
 * This also makes `adb_enabled` a strong automation trigger per the design
 * brief: any rule builder condition against this metric key observes the
 * live system setting, not a stale in-app toggle.
 */
@Singleton
class AdbEnabledMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "ADB debugging enabled",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = if (isAdbEnabled(context)) 1f else 0f

    companion object {
        const val METRIC_KEY = "adb_enabled"

        /**
         * Reads `Settings.Global.ADB_ENABLED` directly — no special
         * permission required. Shared by the metric source and
         * [dev.ranzlappen.gadget.feature.adbdebug.AdbDebugViewModel] so both
         * read the exact same signal.
         */
        fun isAdbEnabled(context: Context): Boolean =
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
    }
}
