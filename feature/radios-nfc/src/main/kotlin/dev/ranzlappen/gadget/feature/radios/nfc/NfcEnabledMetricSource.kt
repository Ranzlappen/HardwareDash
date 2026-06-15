package dev.ranzlappen.gadget.feature.radios.nfc

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class NfcEnabledMetricSource @Inject constructor(
    private val adapter: NfcAdapterWrapper,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "NFC enabled",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Network,
    )

    override suspend fun sample(): Float = if (adapter.isEnabled()) 1f else 0f

    override fun stream(): Flow<Float> = flow {
        while (true) {
            emit(sample())
            delay(POLL_INTERVAL_MS)
        }
    }

    companion object {
        const val METRIC_KEY = "nfc_enabled"
        private const val POLL_INTERVAL_MS = 5_000L
    }
}
