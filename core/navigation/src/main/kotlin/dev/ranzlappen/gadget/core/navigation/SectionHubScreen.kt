package dev.ranzlappen.gadget.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Register a hub screen for every [GadgetSection] that has one (all but
 * Home, which is the dashboard). Called by [GadgetApp] before the
 * feature-contributed routes, so `:app` doesn't need to know about hubs.
 *
 * [onOpen] pushes the chosen tool on top of the hub (plain `navigate`), so
 * back returns to the hub.
 */
fun NavGraphBuilder.sectionHubScreens(onOpen: (GadgetDestination) -> Unit) {
    GadgetSection.entries.filter { it.hasHub }.forEach { section ->
        composable(route = section.route) {
            SectionHubScreen(section = section, onOpen = onOpen)
        }
    }
}

/**
 * A section's tool list: one tappable row per member destination with
 * its icon, name and a one-line summary of what it does.
 */
@Composable
fun SectionHubScreen(
    section: GadgetSection,
    onOpen: (GadgetDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    ModuleScreenScaffold(
        title = section.label,
        modifier = modifier,
        functional = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                section.members.forEach { destination ->
                    CompactCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = destination.label,
                        subtitle = destination.hubSummary(),
                        leadingIcon = destination.iconOutlined,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                // Decorative: the row's title names the target.
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { onOpen(destination) },
                    )
                }
            }
        },
    )
}

/**
 * One-line "what is this for" text shown under each tool on its hub.
 * English-only like [GadgetDestination.label]; moves to resources together
 * with the labels.
 */
internal fun GadgetDestination.hubSummary(): String? = when (this) {
    GadgetDestination.Torch -> "Flashlight, strobe, brightness"
    GadgetDestination.Vibration -> "Effects, patterns, draw your own"
    GadgetDestination.Camera -> "Barcode and QR scanner"
    GadgetDestination.Audio -> "Sound level meter and recorder"
    GadgetDestination.Display -> "Screen brightness"
    GadgetDestination.Apps -> "App folders and folder widgets"
    GadgetDestination.Logbook -> "Timestamped notes and checkpoints"
    GadgetDestination.Actuators -> "Haptic feedback test"
    GadgetDestination.Microphone -> "Advanced microphone tools (root)"
    GadgetDestination.Sensors -> "Every sensor: live values, charts, specs"
    GadgetDestination.Battery -> "Level, health, temperature, voltage"
    GadgetDestination.Storage -> "Volumes, used and free space"
    GadgetDestination.Gps -> "Position, speed, altitude, map"
    GadgetDestination.RadiosWifi -> "Network, signal, frequency"
    GadgetDestination.RadiosBt -> "Adapter and paired devices"
    GadgetDestination.RadiosCell -> "Carrier, network type, signal"
    GadgetDestination.RadiosNfc -> "Read tags, emulate cards"
    GadgetDestination.RadiosIr -> "Infrared remote codes"
    GadgetDestination.RadiosSubghz -> "SDR and transceiver detection"
    GadgetDestination.Flipper -> "Flipper Zero over USB or Bluetooth"
    GadgetDestination.Automation -> "Rules: when this, do that"
    GadgetDestination.Notification -> "Channels and test notifications"
    GadgetDestination.Lock -> "Lock state and lock now"
    GadgetDestination.Diagnostics -> "Memory and system dumps"
    GadgetDestination.BugReport -> "Permission health"
    GadgetDestination.AdbDebug -> "ADB debugging state"
    GadgetDestination.UsbDebug -> "USB debugging state"
    GadgetDestination.Youtubedownloader -> "Video and audio downloads"
    GadgetDestination.Manual -> "How every tool works"
    GadgetDestination.Settings -> "Theme, backup, permissions"
    else -> null
}

@GadgetPreviewLightDark
@Composable
private fun SectionHubScreenPreview() = GadgetThemedPreview {
    SectionHubScreen(section = GadgetSection.Tools, onOpen = {})
}
