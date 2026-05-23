package dev.ranzlappen.gadget.core.ui.module

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.R
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusDot
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Permissions section of the module blueprint.
 *
 * Renders one status row per [permission][ModulePermission] (a coloured
 * [GadgetStatusDot] + label + rationale), then — when any required
 * permission is missing — a primary in-app **Grant** request button and
 * a secondary **Open app settings** link (the link is always offered so
 * users can review or revoke an already-granted permission).
 *
 * When [permissions] is empty the card shows a "no permissions required"
 * state instead — this is the path Torch (the reference module) takes.
 *
 * Grant state is queried live and refreshed both on the
 * permission-request callback and on `ON_RESUME` (so it updates when the
 * user returns from the system app-settings screen).
 */
@Composable
fun ModulePermissionsSection(
    permissions: List<ModulePermission>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val spacing = LocalGadgetTheme.current.spacing

    // Bumped on permission-request result and on ON_RESUME to re-query
    // the live grant state below.
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val grantState: Map<String, Boolean> = remember(permissions, refreshKey) {
        permissions.associate { it.permission to context.isPermissionGranted(it.permission) }
    }
    val ungranted = remember(grantState) {
        permissions.filterNot { grantState[it.permission] == true }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshKey++ }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.module_permissions_title),
        icon = Icons.Outlined.Lock,
    ) {
        if (permissions.isEmpty()) {
            Text(
                text = stringResource(R.string.module_permissions_none),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                permissions.forEach { permission ->
                    PermissionRow(
                        permission = permission,
                        granted = grantState[permission.permission] == true,
                    )
                }
                if (ungranted.isEmpty()) {
                    Text(
                        text = stringResource(R.string.module_permission_all_granted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (ungranted.isNotEmpty()) {
                        GadgetPrimaryButton(
                            onClick = {
                                launcher.launch(ungranted.map { it.permission }.toTypedArray())
                            },
                            text = stringResource(R.string.module_permission_grant),
                        )
                    }
                    GadgetTertiaryButton(
                        onClick = { context.openAppSettings() },
                        text = stringResource(R.string.module_permission_open_settings),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: ModulePermission,
    granted: Boolean,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val statusLabel = stringResource(
        if (granted) R.string.module_permission_granted else R.string.module_permission_denied,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GadgetStatusDot(
            contentDescription = statusLabel,
            color = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Column(modifier = Modifier.padding(start = spacing.micro)) {
            val title = if (permission.optional) {
                stringResource(R.string.module_permission_optional_label, permission.label)
            } else {
                permission.label
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = permission.rationale,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * OS-compatibility section of the module blueprint.
 *
 * Compares the module's [minSdk][OsCompatibility.minSdk] against the
 * live device API level to show a supported / unsupported verdict, then
 * lists any behaviour [notes][OsNote] (each tagged with the API level it
 * applies from).
 */
@Composable
fun ModuleCompatibilitySection(
    compatibility: OsCompatibility,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val deviceSdk = Build.VERSION.SDK_INT
    val supported = deviceSdk >= compatibility.minSdk

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.module_compat_title),
        icon = Icons.Outlined.PhoneAndroid,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GadgetStatusDot(
                    contentDescription = stringResource(
                        if (supported) {
                            R.string.module_compat_supported
                        } else {
                            R.string.module_compat_unsupported
                        },
                    ),
                    color = if (supported) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Text(
                    text = stringResource(R.string.module_compat_requires, compatibility.minSdk),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.module_compat_current, deviceSdk),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            compatibility.notes.forEach { note ->
                Text(
                    text = stringResource(R.string.module_compat_note, note.sinceSdk, note.text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Firmware section of the module blueprint — rendered only for
 * hardware-bridge modules whose [ModuleInfo.firmware] is non-null.
 */
@Composable
fun ModuleFirmwareSection(
    firmware: FirmwareRequirement,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.module_firmware_title),
        icon = Icons.Outlined.Memory,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            Text(
                text = stringResource(
                    R.string.module_firmware_version,
                    firmware.deviceName,
                    firmware.minVersion,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            firmware.notes?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun android.content.Context.isPermissionGranted(permission: String): Boolean =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun android.content.Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

// ─── Previews ───────────────────────────────────────────────────────
//
// The permissions section is intentionally omitted: it calls
// `rememberLauncherForActivityResult`, which needs an
// ActivityResultRegistryOwner that the @Preview pane doesn't provide.
// The compatibility + firmware sections are pure and render cleanly.

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun ModuleInfoSectionsPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        ModuleCompatibilitySection(
            compatibility = OsCompatibility(
                minSdk = 29,
                notes = listOf(
                    OsNote(29, "Runs as a camera-typed foreground service while active."),
                    OsNote(34, "On Android 14+ the service is time-boxed to ~3 minutes."),
                ),
            ),
        )
        ModuleFirmwareSection(
            firmware = FirmwareRequirement(
                deviceName = "Flipper Zero",
                minVersion = "0.80",
                notes = "Older firmware uses a different RPC framing.",
            ),
        )
    }
}
