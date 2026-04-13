package com.gadget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import com.gadget.ui.theme.LocalAccessibilityPreferences

@Composable
fun ManualScreen() {
    val manual = S.manual
    val nav = S.nav
    val hubs = S.hubs
    val accessibility = S.accessibility

    ScreenAnnouncement(accessibility.manualScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Title ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(
                Icons.Default.MenuBook, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                manual.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ── Getting Started ───────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.RocketLaunch,
            title = manual.gettingStartedTitle,
            initiallyExpanded = true,
        ) {
            Text(
                manual.gettingStartedBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Navigation ────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Explore,
            title = manual.navigationTitle,
        ) {
            Text(
                manual.navigationBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Home,
            title = nav.dashboard,
        ) {
            FeatureContent(
                description = manual.dashboard.description,
                howToUse = manual.dashboard.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }

        // ── Torch ─────────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.FlashlightOn,
            title = nav.torch,
        ) {
            FeatureContent(
                description = manual.torch.description,
                howToUse = manual.torch.howToUse,
                prerequisites = manual.torch.prerequisites,
                limitations = manual.torch.limitations,
            )
        }

        // ── Camera ────────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.CameraAlt,
            title = nav.camera,
        ) {
            FeatureContent(
                description = manual.camera.description,
                howToUse = manual.camera.howToUse,
                prerequisites = manual.camera.prerequisites,
                limitations = manual.camera.limitations,
            )
        }

        // ── Vibration ─────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Vibration,
            title = nav.vibration,
        ) {
            FeatureContent(
                description = manual.vibration.description,
                howToUse = manual.vibration.howToUse,
                prerequisites = manual.vibration.prerequisites,
                limitations = manual.vibration.limitations,
            )
        }

        // ── Mic ───────────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Mic,
            title = nav.mic,
        ) {
            FeatureContent(
                description = manual.mic.description,
                howToUse = manual.mic.howToUse,
                prerequisites = manual.mic.prerequisites,
                limitations = manual.mic.limitations,
            )
        }

        // ── Sensors ───────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Sensors,
            title = nav.sensors,
        ) {
            FeatureContent(
                description = manual.sensors.description,
                howToUse = manual.sensors.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }

        // ── Battery ───────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.BatteryFull,
            title = nav.battery,
        ) {
            FeatureContent(
                description = manual.battery.description,
                howToUse = manual.battery.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }

        // ── Radios ────────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.SettingsInputAntenna,
            title = nav.radios,
        ) {
            FeatureContent(
                description = manual.radios.description,
                howToUse = manual.radios.howToUse,
                prerequisites = manual.radios.prerequisites,
                limitations = manual.radios.limitations,
            )
        }

        // ── Logbook ───────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.CheckCircle,
            title = nav.logbook,
        ) {
            FeatureContent(
                description = manual.logbook.description,
                howToUse = manual.logbook.howToUse,
                prerequisites = manual.logbook.prerequisites,
                limitations = manual.logbook.limitations,
            )
        }

        // ── Notifications ─────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Notifications,
            title = hubs.notifications,
        ) {
            FeatureContent(
                description = manual.notifications.description,
                howToUse = manual.notifications.howToUse,
                prerequisites = manual.notifications.prerequisites,
                limitations = manual.notifications.limitations,
            )
        }

        // ── Automation ────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Link,
            title = hubs.automation,
        ) {
            FeatureContent(
                description = manual.automation.description,
                howToUse = manual.automation.howToUse,
                prerequisites = manual.automation.prerequisites,
                limitations = manual.automation.limitations,
            )
        }

        // ── Files ─────────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.InsertDriveFile,
            title = nav.fileMeta,
        ) {
            FeatureContent(
                description = manual.files.description,
                howToUse = manual.files.howToUse,
                prerequisites = manual.files.prerequisites,
                limitations = manual.files.limitations,
            )
        }

        // ── Widgets ───────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Widgets,
            title = manual.widgetsTitle,
        ) {
            FeatureContent(
                description = manual.widgets.description,
                howToUse = manual.widgets.howToUse,
                prerequisites = manual.widgets.prerequisites,
                limitations = manual.widgets.limitations,
            )
        }

        // ── Settings ──────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Settings,
            title = nav.settings,
        ) {
            FeatureContent(
                description = manual.settings.description,
                howToUse = manual.settings.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }

        // ── Bug Report ────────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.BugReport,
            title = nav.bug,
        ) {
            FeatureContent(
                description = manual.bugReport.description,
                howToUse = manual.bugReport.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }

        // ── Accessibility ─────────────────────────────────────────────
        ManualSectionCard(
            icon = Icons.Default.Accessibility,
            title = accessibility.accessibilityTitle,
        ) {
            FeatureContent(
                description = manual.accessibility.description,
                howToUse = manual.accessibility.howToUse,
                prerequisites = null,
                limitations = null,
            )
        }
    }
}

// ─── Expandable Section Card ────────────────────────────────────────────────

@Composable
private fun ManualSectionCard(
    icon: ImageVector,
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val reducedMotion = LocalAccessibilityPreferences.current.reducedMotion
    val expandedLabel = S.accessibility.expanded
    val collapsedLabel = S.accessibility.collapsed

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row — clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        stateDescription = if (expanded) expandedLabel else collapsedLabel
                    }
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .sectionHeading(),
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) S.accessibility.collapseSection
                    else S.accessibility.expandSection,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = if (reducedMotion) EnterTransition.None else expandVertically() + fadeIn(),
                exit = if (reducedMotion) ExitTransition.None else shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    content()
                }
            }
        }
    }
}

// ─── Feature Content Block ──────────────────────────────────────────────────

@Composable
private fun FeatureContent(
    description: String,
    howToUse: String,
    prerequisites: String?,
    limitations: String?,
) {
    val manual = S.manual

    // Description
    Text(
        description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // How to Use
    Spacer(Modifier.height(4.dp))
    Text(
        manual.howToUseLabel,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        howToUse,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Prerequisites (optional)
    if (prerequisites != null) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Key, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                manual.prerequisitesLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            prerequisites,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Limitations (optional)
    if (limitations != null) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                manual.limitationsLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            limitations,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
