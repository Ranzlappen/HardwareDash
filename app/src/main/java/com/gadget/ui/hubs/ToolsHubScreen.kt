package com.gadget.ui.hubs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.gadget.localization.S
import com.gadget.ui.navigation.Routes
import com.gadget.ui.screens.*

@Composable
fun ToolsHubScreen() {
    val nestedNavController = rememberNavController()

    NavHost(
        navController = nestedNavController,
        startDestination = Routes.TOOLS_GRID,
    ) {
        composable(Routes.TOOLS_GRID) {
            ToolsGridScreen(onToolSelected = { route ->
                nestedNavController.navigate(route)
            })
        }
        composable(Routes.TORCH)     { TorchScreen() }
        composable(Routes.CAMERA)    { CameraScreen() }
        composable(Routes.VIBRATION) { VibrationScreen() }
        composable(Routes.MIC)       { MicScreen() }
    }
}

@Composable
private fun ToolsGridScreen(onToolSelected: (String) -> Unit) {
    val hubs = S.hubs
    val nav = S.nav

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Build, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                hubs.toolsTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // 2x2 grid of tool cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FlashlightOn,
                title = nav.torch,
                subtitle = hubs.torchSubtitle,
                onClick = { onToolSelected(Routes.TORCH) },
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
                title = nav.camera,
                subtitle = hubs.cameraSubtitle,
                onClick = { onToolSelected(Routes.CAMERA) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Vibration,
                title = nav.vibration,
                subtitle = hubs.vibrationSubtitle,
                onClick = { onToolSelected(Routes.VIBRATION) },
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Mic,
                title = nav.mic,
                subtitle = hubs.micSubtitle,
                onClick = { onToolSelected(Routes.MIC) },
            )
        }
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon, contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
