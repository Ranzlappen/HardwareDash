package dev.ranzlappen.gadget.feature.bugreport.monitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.bugreport.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bugreport's readable signal for the monitoring framework (and, later, for
 * automation triggers): the percentage of the diagnostic permission set
 * ([BugReportState.permissions] in the screen's own `BugReportViewModel`)
 * that is currently granted — the same "N of M granted" readiness the
 * `bugreport_cap_health_*` capability row already surfaces, made chartable.
 *
 * The permission list mirrors `BugReportViewModel.buildState`'s exactly
 * (camera / microphone / fine location / phone state, plus the
 * SDK-gated Bluetooth-connect and post-notifications permissions) so the
 * metric and the screen never drift. It's duplicated rather than shared
 * because the ViewModel is `@HiltViewModel`-scoped to a screen while this
 * source is an app-`@Singleton` sampled independently of any UI being open;
 * both read the same un-privileged `PackageManager` grant state, so there is
 * no risk of disagreement, only of the two lists diverging if one is edited
 * without the other.
 *
 * `100` means every diagnostic permission is granted (the bug-report bundle
 * can capture everything it might need); a drop below `100` is exactly the
 * "action needed" moment the Health screen's warning chip already flags.
 *
 * Polled (not push): there is no OS broadcast for a permission grant/revoke
 * outside this process (the screen instead re-scans on `ON_RESUME`), so a
 * regular sample on the monitor's cadence is the only way to keep the chart
 * current.
 */
@Singleton
class BugreportMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.bugreport_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float {
        val permissions = diagnosticPermissions()
        if (permissions.isEmpty()) return 100f
        val grantedCount = permissions.count {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return grantedCount.toFloat() / permissions.size.toFloat() * 100f
    }

    companion object {
        const val METRIC_KEY = "bugreport_permission_readiness"

        /**
         * The same diagnostic permission set `BugReportViewModel.buildState`
         * scans, kept in lockstep by hand (see class doc for why it isn't a
         * shared reference).
         */
        private fun diagnosticPermissions(): List<String> {
            val perms = mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms += Manifest.permission.BLUETOOTH_CONNECT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms += Manifest.permission.POST_NOTIFICATIONS
            }
            return perms
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface BugreportMonitorModule {

    @Binds
    @IntoMap
    @StringKey(BugreportMetricSource.METRIC_KEY)
    fun bindBugreportMetricSource(source: BugreportMetricSource): MetricSource
}
