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
 * Permission readiness as a metric (`bugreport_permission_readiness`, key
 * kept stable for saved rules and history): the percentage of a fixed
 * diagnostic permission set — camera, microphone, fine location, phone
 * state, plus SDK-gated Bluetooth-connect and post-notifications — that is
 * currently granted. Since v3 B8 the screen itself shows the shared
 * `PermissionsDashboardCard`; this source keeps its own small list so the
 * chartable percentage stays comparable with v2 history.
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

        /** The diagnostic permission set the percentage is computed over. */
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
