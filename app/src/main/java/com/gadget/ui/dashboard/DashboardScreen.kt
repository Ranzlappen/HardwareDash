package com.gadget.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import com.gadget.ui.logbook.LogbookRepository
import com.gadget.ui.logbook.LogbookEntry
import com.gadget.ui.navigation.Routes
import com.gadget.widget.WidgetMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val strings = S.dashboard
    val nav = S.nav

    // Live status data
    var batteryLevel by remember { mutableStateOf("--") }
    var batteryStatus by remember { mutableStateOf("--") }
    var wifiSsid by remember { mutableStateOf("--") }
    var wifiSignal by remember { mutableStateOf("--") }
    var recentEntry by remember { mutableStateOf<LogbookEntry?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            batteryLevel = WidgetMetric.BATTERY_LEVEL.fetch(context)
            batteryStatus = WidgetMetric.BATTERY_STATUS.fetch(context)
            try { wifiSsid = WidgetMetric.WIFI_SSID.fetch(context) } catch (_: Exception) {}
            try { wifiSignal = WidgetMetric.WIFI_SIGNAL.fetch(context) } catch (_: Exception) {}
            try {
                val repo = LogbookRepository(context)
                val store = repo.storeFlow.firstOrNull()
                recentEntry = store?.entries
                    ?.sortedByDescending { it.isoDate }
                    ?.firstOrNull()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        Column {
            Text(
                strings.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                strings.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Status Summary ──────────────────────────────────────────
        Text(
            strings.status,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                listOf(
                    StatusCardData(Icons.Default.BatteryStd, "Battery", batteryLevel, batteryStatus),
                    StatusCardData(Icons.Default.Wifi, "WiFi", wifiSsid, wifiSignal),
                )
            ) { data ->
                StatusCard(data)
            }
        }

        // ── Quick Actions ───────────────────────────────────────────
        Text(
            strings.quickActions,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        // Row 1: Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FlashlightOn,
                label = nav.torch,
                onClick = { onNavigate(Routes.TOOLS) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
                label = nav.camera,
                onClick = { onNavigate(Routes.TOOLS) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Vibration,
                label = nav.vibration,
                onClick = { onNavigate(Routes.TOOLS) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Mic,
                label = nav.mic,
                onClick = { onNavigate(Routes.TOOLS) },
            )
        }

        // Row 2: Monitor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                label = nav.sensors,
                onClick = { onNavigate(Routes.MONITOR) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.BatteryStd,
                label = nav.battery,
                onClick = { onNavigate(Routes.MONITOR) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Wifi,
                label = nav.radios,
                onClick = { onNavigate(Routes.MONITOR) },
            )
        }

        // Row 3: Logbook + More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                label = nav.logbook,
                onClick = { onNavigate(Routes.LOGBOOK) },
            )
            QuickActionChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Apps,
                label = nav.more,
                onClick = { onNavigate(Routes.MORE) },
            )
        }

        // ── Recent Logbook Entry ────────────────────────────────────
        recentEntry?.let { entry ->
            Text(
                strings.recentLogEntry,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Routes.LOGBOOK) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        entry.text.ifEmpty { "Log entry" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (entry.isoDate.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formatTimestamp(entry.isoDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } ?: run {
            Text(
                strings.recentLogEntry,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                strings.noRecentEntries,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Composable helpers ──────────────────────────────────────────────────────

private data class StatusCardData(
    val icon: ImageVector,
    val title: String,
    val primary: String,
    val secondary: String,
)

@Composable
private fun StatusCard(data: StatusCardData) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.width(160.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                data.icon,
                contentDescription = data.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                data.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                data.secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatTimestamp(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val local = instant.atZone(ZoneId.systemDefault())
        local.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    } catch (_: Exception) {
        isoDate
    }
}
