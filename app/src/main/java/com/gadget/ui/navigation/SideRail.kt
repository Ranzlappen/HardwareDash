package com.gadget.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.gadget.localization.S
import com.gadget.ui.theme.LocalAccessibilityPreferences

/** Single entry in the [SideRail]. Icon + route + a11y label. */
data class SideRailItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)

/**
 * Returns the rail item list in display order. Read inside a Composable so
 * localized labels resolve correctly. The 14 main items scroll vertically;
 * Search is rendered separately, anchored at the bottom of the rail.
 */
@Composable
fun rememberSideRailItems(): Pair<List<SideRailItem>, SideRailItem> {
    val nav = S.nav
    val hubs = S.hubs
    val common = S.common

    val main = listOf(
        SideRailItem(Routes.TORCH,         Icons.Filled.FlashlightOn,   nav.torch),
        SideRailItem(Routes.VIBRATION,     Icons.Filled.Vibration,      nav.vibration),
        SideRailItem(Routes.CAMERA,        Icons.Filled.CameraAlt,      nav.camera),
        SideRailItem(Routes.MIC,           Icons.Filled.Mic,            nav.mic),
        SideRailItem(Routes.SENSORS,       Icons.Filled.Analytics,      nav.sensors),
        SideRailItem(Routes.BATTERY,       Icons.Filled.BatteryStd,     nav.battery),
        SideRailItem(Routes.RADIOS,        Icons.Filled.Wifi,           nav.radios),
        SideRailItem(Routes.LOGBOOK,       Icons.Filled.CheckCircle,    nav.logbook),
        SideRailItem(Routes.LOCKSCREEN,    Icons.Filled.Notifications,  hubs.notifications),
        SideRailItem(Routes.FILE_META,     Icons.Filled.InsertDriveFile, nav.fileMeta),
        SideRailItem(Routes.LINK,          Icons.Filled.Link,           hubs.automation),
        SideRailItem(Routes.APPS,          Icons.Filled.Apps,           nav.apps),
        SideRailItem(Routes.SETTINGS,      Icons.Filled.Settings,       nav.settings),
        SideRailItem(Routes.MANUAL,        Icons.Filled.MenuBook,       nav.manual),
        SideRailItem(Routes.BUG,           Icons.Filled.BugReport,      nav.bug),
    )
    val search = SideRailItem(Routes.SEARCH, Icons.Filled.Search, common.search)
    return main to search
}

/** Width of the rail. Kept as a public constant so callers can offset content. */
val SideRailWidth = 56.dp

/**
 * Ultra-thin vertical navigation rail. 56 dp wide, symbol-only.
 *
 * The 14 main items scroll vertically inside a [LazyColumn]; a divider and
 * the Search action sit anchored at the bottom of the rail (always visible).
 *
 * @param currentRoute Currently selected route, used to highlight one item.
 * @param onItemClick  Invoked when the user taps an item; receives the route.
 */
@Composable
fun SideRail(
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (mainItems, searchItem) = rememberSideRailItems()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(SideRailWidth)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(items = mainItems, key = { it.route }) { item ->
                SideRailItemView(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onItemClick(item.route) },
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        SideRailItemView(
            item = searchItem,
            selected = currentRoute == searchItem.route,
            onClick = { onItemClick(searchItem.route) },
        )
    }
}

@Composable
private fun SideRailItemView(
    item: SideRailItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = LocalAccessibilityPreferences.current.reducedMotion

    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(if (reducedMotion) 0 else 180),
        label = "rail-tint",
    )
    val indicatorHeight by animateDpAsState(
        targetValue = if (selected) 24.dp else 0.dp,
        animationSpec = tween(if (reducedMotion) 0 else 220),
        label = "rail-indicator",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable {
                if (!reducedMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics {
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        // Selection indicator pill on the left edge.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(indicatorHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )

        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
