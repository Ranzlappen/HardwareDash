package com.gadget.ui.dashboard

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.data.repository.MetricRepository
import com.gadget.ui.logbook.LogbookEntry
import com.gadget.ui.logbook.LogbookRepository
import com.gadget.widget.WidgetMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val metricRepository: MetricRepository,
    private val logbookRepository: LogbookRepository,
    application: Application,
) : AndroidViewModel(application) {

    // ── Hero metrics ────────────────────────────────────────────────
    private val _heroMetrics = MutableStateFlow<List<HeroMetric>>(emptyList())
    val heroMetrics: StateFlow<List<HeroMetric>> = _heroMetrics.asStateFlow()

    // ── Insights ────────────────────────────────────────────────────
    private val _insights = MutableStateFlow<List<Insight>>(emptyList())
    val insights: StateFlow<List<Insight>> = _insights.asStateFlow()

    // ── Recent logbook entry ────────────────────────────────────────
    val recentEntry: StateFlow<LogbookEntry?> = logbookRepository.storeFlow
        .map { store ->
            store.entries
                .sortedByDescending { it.isoDate }
                .firstOrNull()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Activity feed ───────────────────────────────────────────────
    val activityFeed: StateFlow<List<ActivityItem>> = logbookRepository.storeFlow
        .map { store ->
            store.entries
                .sortedByDescending { it.isoDate }
                .take(MAX_ACTIVITY_ITEMS)
                .map { entry ->
                    ActivityItem(
                        id = entry.id,
                        text = entry.text.ifEmpty { "Log entry" },
                        timestamp = formatTimestamp(entry.isoDate),
                        type = ActivityType.LOG_ENTRY,
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Init ────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            loadHeroMetrics()
        }

        // Refresh sparkline data every 30 seconds
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                loadHeroMetrics()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun loadHeroMetrics() {
        withContext(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()

                // Fetch current values
                val batteryLevel = try {
                    WidgetMetric.BATTERY_LEVEL.fetch(ctx)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch battery level")
                    "--"
                }

                val wifiSignal = try {
                    WidgetMetric.WIFI_SIGNAL.fetch(ctx)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch WiFi signal")
                    "--"
                }

                // Load sparkline data from recent Room readings
                val batterySparkline = loadSparklineData(WidgetMetric.BATTERY_LEVEL.key)
                val wifiSparkline = loadSparklineData(WidgetMetric.WIFI_SIGNAL.key)

                val heroes = listOf(
                    HeroMetric(
                        key = WidgetMetric.BATTERY_LEVEL.key,
                        label = "Battery",
                        currentValue = batteryLevel,
                        icon = Icons.Default.BatteryStd,
                        sparklineData = batterySparkline,
                    ),
                    HeroMetric(
                        key = WidgetMetric.WIFI_SIGNAL.key,
                        label = "WiFi Signal",
                        currentValue = wifiSignal,
                        icon = Icons.Default.Wifi,
                        sparklineData = wifiSparkline,
                    ),
                )

                _heroMetrics.value = heroes

                // Generate insights from the data
                generateInsights(batterySparkline)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load hero metrics")
            }
        }
    }

    private suspend fun loadSparklineData(metricKey: String): List<Float> {
        return try {
            val readings = metricRepository.getRecentReadings(metricKey, SPARKLINE_POINTS)
            readings
                .sortedBy { it.timestamp }
                .map { it.rawValue.toFloat() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load sparkline data for %s", metricKey)
            emptyList()
        }
    }

    private fun generateInsights(batteryReadings: List<Float>) {
        val newInsights = mutableListOf<Insight>()
        val now = System.currentTimeMillis()

        if (batteryReadings.size >= 2) {
            val first = batteryReadings.first()
            val last = batteryReadings.last()
            val diff = last - first

            if (diff < -5f) {
                newInsights.add(
                    Insight(
                        id = UUID.randomUUID().toString(),
                        message = "Battery dropped ${"%.0f".format(-diff)}% recently",
                        icon = Icons.Default.TrendingDown,
                        timestamp = now,
                    )
                )
            } else if (diff > 5f) {
                newInsights.add(
                    Insight(
                        id = UUID.randomUUID().toString(),
                        message = "Battery gained ${"%.0f".format(diff)}% recently",
                        icon = Icons.Default.TrendingUp,
                        timestamp = now,
                    )
                )
            }
        }

        _insights.value = newInsights
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
        private const val SPARKLINE_POINTS = 20
        private const val MAX_ACTIVITY_ITEMS = 10

        fun formatTimestamp(isoDate: String): String {
            return try {
                val instant = Instant.parse(isoDate)
                val local = instant.atZone(ZoneId.systemDefault())
                local.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
            } catch (_: Exception) {
                isoDate
            }
        }
    }
}
