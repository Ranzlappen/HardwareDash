package dev.ranzlappen.gadget.feature.bugreport

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.permissions.PermissionsDashboardCard
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold

/**
 * Permission health, opened from Device & debug (v3 B8).
 *
 * v2 kept its own hardcoded six-permission list here next to the richer,
 * registry-driven `PermissionsDashboardCard` in Settings. v3 shows that one
 * shared card in both places — it requests runtime permissions in-app,
 * deep-links special ones and re-scans on resume.
 */
@Composable
fun BugReportScreen(modifier: Modifier = Modifier) {
    ModuleScreenScaffold(
        title = stringResource(R.string.bugreport_screen_title),
        modifier = modifier,
        functional = {
            PermissionsDashboardCard(modifier = Modifier.fillMaxWidth())
        },
    )
}
