package dev.ranzlappen.gadget.feature.torch.components

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.OsNote
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchRootAvailability
import dev.ranzlappen.gadget.feature.torch.TorchState

/**
 * Torch's [ModuleInfo] — the reference implementation of the module
 * blueprint. Torch toggles on-device hardware via `CameraManager`, so:
 *  - the only runtime permission is the optional `POST_NOTIFICATIONS`
 *    (Android 13+) that lets the strobe foreground-service chrome and
 *    the widget toggle confirmations show — the feature works without
 *    it, so it's marked `optional`,
 *  - it works on every supported OS (minSdk 29) with two foreground-
 *    service behaviour notes,
 *  - it has **no firmware** requirement (the firmware section is
 *    omitted).
 */
@Composable
internal fun torchModuleInfo(
    torch: TorchState,
    root: TorchRootAvailability,
): ModuleInfo = ModuleInfo(
    permissions = buildList {
        // POST_NOTIFICATIONS only exists as a runtime permission on
        // API 33+; below that it's auto-granted, so don't surface it as
        // a perpetually-missing row.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                ModulePermission(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    label = stringResource(R.string.torch_module_perm_notifications_label),
                    rationale = stringResource(R.string.torch_module_perm_notifications_rationale),
                    optional = true,
                ),
            )
        }
    },
    compatibility = OsCompatibility(
        minSdk = Build.VERSION_CODES.Q,
        notes = listOf(
            OsNote(
                sinceSdk = Build.VERSION_CODES.Q,
                text = stringResource(R.string.torch_module_compat_note_fgs_active),
            ),
            OsNote(
                sinceSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                text = stringResource(R.string.torch_module_compat_note_fgs_short),
            ),
        ),
    ),
    firmware = null,
    capabilities = torchCapabilities(torch, root),
)

/**
 * Per-function capability rows for Torch — green/amber/red status for each
 * button and action across both the standard and rooted app versions. The
 * standard functions key off live flash-hardware + OS-version checks; the
 * rooted functions key off the [dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities]
 * probe ([root]), so on the standard build (or an un-rooted device) they
 * read red with a "requires the rooted app version" message.
 */
@Composable
private fun torchCapabilities(
    torch: TorchState,
    root: TorchRootAvailability,
): List<ModuleCapability> {
    val hasFlash = torch.isAvailable
    val noFlashMsg = stringResource(R.string.torch_cap_no_flash)
    val needsRootMsg = stringResource(R.string.torch_cap_needs_root)

    return listOf(
        ModuleCapability(
            name = stringResource(R.string.torch_cap_basic_name),
            detail = stringResource(R.string.torch_cap_basic_detail),
            status = {
                if (hasFlash) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_basic_ok))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, noFlashMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_strobe_name),
            detail = stringResource(R.string.torch_cap_strobe_detail),
            status = {
                when {
                    !hasFlash -> CapabilityStatus(GadgetStatusKind.Error, noFlashMsg)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_strobe_caveat))
                    else -> CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_strobe_ok))
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_brightness_name),
            detail = stringResource(R.string.torch_cap_root_brightness_detail),
            status = {
                when {
                    root.brightnessReady ->
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_root_ready))
                    root.rootReady ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_no_led))
                    else -> CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_strobe_name),
            detail = stringResource(R.string.torch_cap_root_strobe_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_root_ready))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_multiled_name),
            detail = stringResource(R.string.torch_cap_root_multiled_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_multiled_caveat))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_thermal_name),
            detail = stringResource(R.string.torch_cap_root_thermal_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_thermal_caveat))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
    )
}
