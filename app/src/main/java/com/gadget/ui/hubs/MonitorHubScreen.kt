package com.gadget.ui.hubs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.gadget.localization.S
import com.gadget.ui.navigation.Routes
import com.gadget.ui.screens.*
import com.gadget.widget.WidgetMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MonitorHubScreen() {
    val nestedNavController = rememberNavController()

    NavHost(
        navController = nestedNavController,
        startDestination = Routes.MONITOR_GRID,
    ) {
        composable(Routes.MONITOR_GRID) {
            MonitorGridScreen(onItemSelected = { route ->
                nestedNavController.navigate(route)
            })
        }
        composable(Routes.SENSORS) { SensorsScreen() }
        composable(Routes.BATTERY) { BatteryScreen() }
        composable(Routes.RADIOS)  { RadiosScreen() }
    }
}

@Composable
private fun MonitorGridScreen(onItemSelected: (String) -> Unit) {
    val hubs = S.hubs
    val nav = S.nav
    val context = LocalContext.current

    // Live metric previews
    var batteryLevel by remember { mutableStateOf("--") }
    var batteryStatus by remember { mutableStateOf("--") }
    var wifiSsid by remember { mutableStateOf("--") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            batteryLevel = WidgetMetric.BATTERY_LEVEL.fetch(context)
            batteryStatus = WidgetMetric.BATTERY_STATUS.fetch(context)
            try { wifiSsid = WidgetMetric.WIFI_SSID.fetch(context) } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.MonitorHeart, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                hubs.monitorTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(4.dp))

        MonitorCard(
            icon = Icons.Default.Analytics,
            title = nav.sensors,
            subtitle = hubs.sensorsSubtitle,
            onClick = { onItemSelected(Routes.SENSORS) },
        )

        MonitorCard(
            icon = Icons.Default.BatteryStd,
            title = nav.battery,
            subtitle = hubs.batterySubtitle,
            preview = "$batteryLevel \u2022 $batteryStatus",
            onClick = { onItemSelected(Routes.BATTERY) },
        )

        MonitorCard(
            icon = Icons.Default.Wifi,
            title = nav.radios,
            subtitle = hubs.radiosSubtitle,
            preview = wifiSsid,
            onClick = { onItemSelected(Routes.RADIOS) },
        )
    }
}

@Composable
private fun MonitorCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    preview: String? = null,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon, contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (preview != null && preview != "--") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preview,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
