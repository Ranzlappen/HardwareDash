package dev.ranzlappen.gadget.core.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.SectionHeader

/**
 * The reusable Permissions dashboard (W5) — a self-contained Hilt-injected
 * card the Settings screen drops in via a slot. Scans grant state through
 * [PermissionsViewModel], requests runtime permissions in-app, deep-links to
 * the Settings screen for special permissions, and re-scans on `ON_RESUME`
 * so grants made outside the app reflect on return.
 */
@Composable
fun PermissionsDashboardCard(
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = LocalGadgetTheme.current.spacing

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.permissions_card_title),
        icon = Icons.Outlined.Shield,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text = if (state.summary.allGranted) {
                    stringResource(R.string.permissions_all_granted)
                } else {
                    stringResource(
                        R.string.permissions_summary,
                        state.summary.granted,
                        state.summary.total,
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            state.groups.forEach { group ->
                if (state.groups.size > 1) {
                    SectionHeader(label = group.displayName, modifier = Modifier.fillMaxWidth())
                }
                group.runtime.forEach { row ->
                    PermissionRowUi(
                        label = row.permission.label,
                        rationale = row.permission.rationale,
                        granted = row.granted,
                        onGrant = { runtimeLauncher.launch(row.permission.permission) },
                    )
                }
                group.special.forEach { row ->
                    PermissionRowUi(
                        label = specialLabel(row.special),
                        rationale = stringResource(R.string.permissions_special_rationale),
                        granted = row.granted,
                        grantLabel = stringResource(R.string.permissions_manage),
                        onGrant = {
                            runCatching {
                                context.startActivity(
                                    SpecialPermissions.settingsIntent(context, row.special),
                                )
                            }
                        },
                    )
                }
            }
            if (!state.summary.allGranted) {
                GadgetTertiaryButton(
                    onClick = {
                        runCatching {
                            context.startActivity(SpecialPermissions.appDetailsIntent(context))
                        }
                    },
                    text = stringResource(R.string.permissions_open_settings),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PermissionRowUi(
    label: String,
    rationale: String,
    granted: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
    grantLabel: String = stringResource(R.string.permissions_grant),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (granted) {
            GadgetChip(
                selected = true,
                onClick = {},
                label = stringResource(R.string.permissions_granted),
                enabled = false,
            )
        } else {
            GadgetSecondaryButton(onClick = onGrant, text = grantLabel)
        }
    }
}

@Composable
private fun specialLabel(special: SpecialPermission): String = stringResource(
    when (special) {
        SpecialPermission.Overlay -> R.string.permissions_special_overlay
        SpecialPermission.ExactAlarm -> R.string.permissions_special_exact_alarm
        SpecialPermission.WriteSettings -> R.string.permissions_special_write_settings
        SpecialPermission.NotificationListener -> R.string.permissions_special_notification_listener
        SpecialPermission.AllFilesAccess -> R.string.permissions_special_all_files
    },
)
