package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.ScreenHeader
import dev.ranzlappen.gadget.core.ui.component.SectionHeader
import dev.ranzlappen.gadget.core.ui.component.SparklineChart
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Phase 1 mock dashboard.
 *
 * Renders a responsive grid of glassmorphic [DashCard]s populated from
 * a hardcoded [sampleTiles] set. Layout is intentionally adaptive:
 * [LazyVerticalGrid] with `GridCells.Adaptive(minSize)` grows columns
 * on wider screens (foldables open, tablets, landscape phones) without
 * any manual breakpoint logic.
 *
 * No real hardware access in Phase 1 — everything is static. A
 * `DashboardViewModel` + the live hardware registry land in Phase 2
 * when `core:hardware` exposes the `Sensor` / `Actuator` registries
 * and `core:data` knows how to read from them. The shape of this
 * composable's API (a single [onNavigate] callback) is the seam Phase
 * 2 will inject the ViewModel through, keeping the call site
 * unchanged.
 */
@Composable
fun DashboardScreen(
    onNavigate: (GadgetDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashboardContent(
        tiles = sampleTiles,
        onTileClick = { tile -> onNavigate(tile.destination) },
        modifier = modifier,
    )
}

@Composable
private fun DashboardContent(
    tiles: List<DashboardTile>,
    onTileClick: (DashboardTile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = GadgetSpacing.Medium,
            end = GadgetSpacing.Medium,
            top = 0.dp,
            bottom = GadgetSpacing.Large,
        ),
        horizontalArrangement = Arrangement.spacedBy(GadgetSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(GadgetSpacing.Small),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = "Dashboard",
                subtitle = "Live system overview",
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(label = "Live readouts")
        }
        items(items = tiles, key = { it.id }) { tile ->
            DashboardTileCard(
                tile = tile,
                onClick = { onTileClick(tile) },
            )
        }
    }
}

@Composable
private fun DashboardTileCard(
    tile: DashboardTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = tile.title,
        icon = tile.icon,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(GadgetSpacing.Micro),
        ) {
            Text(
                text = tile.primary,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = tile.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = tile.secondary,
            style = MaterialTheme.typography.bodySmall,
        )
        SparklineChart(
            samples = tile.samples,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(top = GadgetSpacing.Small),
        )
    }
}

// ─── Mock data ──────────────────────────────────────────────────────

@Immutable
private data class DashboardTile(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val primary: String,
    val unit: String,
    val secondary: String,
    val samples: List<Float>,
    val destination: GadgetDestination,
)

/**
 * Phase 1 hardcoded mock data. Numbers are plausible (CPU temp in a
 * normal range, battery climbing while charging, Wi-Fi RSSI in the
 * "good" band) so the dashboard looks like a real device's first
 * frame after the activity restores.
 *
 * Sparkline series are 24 samples each — roughly an hour of data at
 * 2.5-minute granularity, which is what the Phase-2 real sampler
 * will collect for the "last hour" overview.
 */
private val sampleTiles: List<DashboardTile> = listOf(
    DashboardTile(
        id = "battery",
        title = "Battery",
        icon = Icons.Filled.BatteryChargingFull,
        primary = "87",
        unit = "%",
        secondary = "Charging · 1h 22m to full",
        samples = listOf(
            72f, 73f, 74f, 75f, 76f, 77f, 78f, 79f,
            80f, 81f, 82f, 82f, 83f, 84f, 85f, 85f,
            86f, 86f, 86f, 87f, 87f, 87f, 87f, 87f,
        ),
        destination = GadgetDestination.Sensors,
    ),
    DashboardTile(
        id = "thermal",
        title = "Thermals",
        icon = Icons.Filled.DeviceThermostat,
        primary = "34.2",
        unit = "°C",
        secondary = "CPU pkg · normal",
        samples = listOf(
            33.1f, 33.4f, 33.6f, 33.8f, 34.1f, 34.4f, 34.7f, 35.0f,
            35.2f, 35.0f, 34.8f, 34.6f, 34.4f, 34.3f, 34.2f, 34.1f,
            34.0f, 34.1f, 34.2f, 34.3f, 34.3f, 34.2f, 34.2f, 34.2f,
        ),
        destination = GadgetDestination.Sensors,
    ),
    DashboardTile(
        id = "light",
        title = "Ambient",
        icon = Icons.Filled.WbSunny,
        primary = "142",
        unit = "lx",
        secondary = "Indoor · daylight",
        samples = listOf(
            45f, 48f, 52f, 58f, 67f, 75f, 84f, 95f,
            108f, 115f, 122f, 128f, 132f, 135f, 138f, 140f,
            141f, 142f, 142f, 143f, 142f, 142f, 142f, 142f,
        ),
        destination = GadgetDestination.Sensors,
    ),
    DashboardTile(
        id = "wifi",
        title = "Wi-Fi",
        icon = Icons.Filled.Wifi,
        primary = "−56",
        unit = "dBm",
        secondary = "Good · 866 Mbps link",
        samples = listOf(
            -58f, -57f, -59f, -56f, -55f, -56f, -57f, -58f,
            -56f, -55f, -54f, -56f, -57f, -56f, -55f, -56f,
            -57f, -56f, -55f, -56f, -57f, -56f, -56f, -56f,
        ),
        destination = GadgetDestination.Sensors,
    ),
    DashboardTile(
        id = "steps",
        title = "Steps",
        icon = Icons.Filled.DirectionsWalk,
        primary = "6,432",
        unit = "today",
        secondary = "Step counter · idle",
        samples = listOf(
            0f, 120f, 340f, 580f, 820f, 1100f, 1450f, 1880f,
            2310f, 2740f, 3120f, 3580f, 4020f, 4380f, 4720f, 5040f,
            5310f, 5620f, 5870f, 6080f, 6240f, 6360f, 6420f, 6432f,
        ),
        destination = GadgetDestination.Sensors,
    ),
    DashboardTile(
        id = "network",
        title = "Network",
        icon = Icons.Filled.NetworkCheck,
        primary = "12.4",
        unit = "Mbps",
        secondary = "Down · last 30s",
        samples = listOf(
            8.4f, 9.1f, 11.2f, 14.6f, 18.3f, 15.7f, 12.8f, 10.4f,
            8.9f, 11.5f, 14.2f, 17.6f, 16.1f, 13.4f, 11.7f, 10.9f,
            12.1f, 13.5f, 14.8f, 13.2f, 11.9f, 11.4f, 12.2f, 12.4f,
        ),
        destination = GadgetDestination.Sensors,
    ),
)

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@Composable
private fun DashboardScreenPreview() = GadgetThemedPreview {
    Column {
        DashboardScreen(onNavigate = {})
    }
}
