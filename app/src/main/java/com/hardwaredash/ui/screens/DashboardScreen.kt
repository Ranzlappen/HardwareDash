package com.hardwaredash.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hardwaredash.ui.navigation.Routes

// ─── Data model for each feature tile ────────────────────────────────────────
private data class FeatureCard(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val available: Boolean = true,
)

private val features = listOf(
    FeatureCard(Routes.TORCH,        "Torch",         "Toggle flash LED",            Icons.Default.FlashlightOn),
    FeatureCard(Routes.CAMERA,       "Camera",        "Preview & capture",           Icons.Default.CameraAlt),
    FeatureCard(Routes.VIBRATION,    "Vibration",     "Patterns & waveforms",        Icons.Default.Vibration),
    FeatureCard(Routes.MIC,          "Microphone",    "Live amplitude meter",        Icons.Default.Mic),
    FeatureCard(Routes.RADIOS,       "Radios",        "WiFi · BT · NFC status",      Icons.Default.Wifi),
    FeatureCard(Routes.SENSORS,      "Sensors",       "Gyro · Accel · Light…",       Icons.Default.Analytics),
    FeatureCard(Routes.NOTIFICATIONS,"Notifications", "Rich + heads-up demos",       Icons.Default.Notifications),
    FeatureCard(Routes.LOCKSCREEN,   "Lock Screen",   "Admin lock + overlay",        Icons.Default.Lock),
)

@Composable
fun DashboardScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Text(
            text       = "HardwareDash",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text  = "Tap a module to interact with your device hardware.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(16.dp))

        // ── Feature grid ─────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns         = GridCells.Fixed(2),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(features) { card ->
                FeatureTile(card) { navController.navigate(card.route) }
            }
        }
    }
}

@Composable
private fun FeatureTile(card: FeatureCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.Start,
        ) {
            Icon(
                imageVector        = card.icon,
                contentDescription = card.title,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(36.dp),
            )
            Column {
                Text(
                    text       = card.title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text  = card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
