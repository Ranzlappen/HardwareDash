package com.gadget.ui.hubs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.ui.components.sectionHeading
import com.gadget.ui.navigation.Routes
import com.gadget.ui.screens.*
import com.gadget.ui.link.LinkScreen

@Composable
fun MoreHubScreen() {
    val nestedNavController = rememberNavController()

    NavHost(
        navController = nestedNavController,
        startDestination = Routes.MORE_GRID,
    ) {
        composable(Routes.MORE_GRID) {
            MoreGridScreen(onItemSelected = { route ->
                nestedNavController.navigate(route)
            })
        }
        composable(Routes.LOCKSCREEN) { LockScreenScreen() }
        composable(Routes.LINK)       { LinkScreen() }
        composable(Routes.FILE_META)  { FileMetadataScreen() }
        composable(Routes.SETTINGS)   { SettingsScreen() }
        composable(Routes.BUG)        { BugReportScreen() }
        composable(Routes.MANUAL)     { ManualScreen() }
    }
}

@Composable
private fun MoreGridScreen(onItemSelected: (String) -> Unit) {
    val hubs = S.hubs
    val nav = S.nav

    ScreenAnnouncement(S.accessibility.moreScreen)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Icon(
                Icons.Default.Apps, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                hubs.moreTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.sectionHeading(),
            )
        }

        Spacer(Modifier.height(8.dp))

        MoreListItem(
            icon = Icons.Default.MenuBook,
            title = hubs.userManual,
            subtitle = hubs.userManualSubtitle,
            onClick = { onItemSelected(Routes.MANUAL) },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        MoreListItem(
            icon = Icons.Default.Notifications,
            title = hubs.notifications,
            subtitle = hubs.notificationsSubtitle,
            onClick = { onItemSelected(Routes.LOCKSCREEN) },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        MoreListItem(
            icon = Icons.Default.Link,
            title = hubs.automation,
            subtitle = hubs.automationSubtitle,
            onClick = { onItemSelected(Routes.LINK) },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        MoreListItem(
            icon = Icons.Default.InsertDriveFile,
            title = nav.fileMeta,
            subtitle = hubs.fileMetaSubtitle,
            onClick = { onItemSelected(Routes.FILE_META) },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        MoreListItem(
            icon = Icons.Default.Settings,
            title = nav.settings,
            subtitle = hubs.settingsSubtitle,
            onClick = { onItemSelected(Routes.SETTINGS) },
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        MoreListItem(
            icon = Icons.Default.BugReport,
            title = nav.bug,
            subtitle = hubs.bugReportSubtitle,
            onClick = { onItemSelected(Routes.BUG) },
        )
    }
}

@Composable
private fun MoreListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title, $subtitle"
            }
            .clickable(onClick = onClick),
        headlineContent = {
            Text(title, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics { },
            )
        },
    )
}
