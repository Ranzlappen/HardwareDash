package dev.ranzlappen.gadget.feature.vibration.components

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
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationRootAvailability
import dev.ranzlappen.gadget.feature.vibration.VibrationState

/**
 * Vibration's [ModuleInfo] — the standard **Permissions → OS compatibility →
 * Functions & compatibility** metadata block for the scaffold. Mirrors
 * `torchModuleInfo`:
 *  - the only declared permission is `VIBRATE` (install-time / normal, so it's
 *    surfaced informationally, not as a perpetually-missing runtime row),
 *  - works on every supported OS (minSdk 29) with a `VibratorManager` (API 31+)
 *    note and an amplitude-control caveat,
 *  - no firmware requirement,
 *  - tri-state per-function rows for the standard tier + all four rooted tools.
 */
@Composable
internal fun vibrationModuleInfo(
    vibration: VibrationState,
    root: VibrationRootAvailability,
): ModuleInfo = ModuleInfo(
    permissions = listOf(
        ModulePermission(
            permission = Manifest.permission.VIBRATE,
            label = stringResource(R.string.vibration_module_perm_vibrate_label),
            rationale = stringResource(R.string.vibration_module_perm_vibrate_rationale),
            // VIBRATE is a normal (install-time) permission — auto-granted, so
            // it's informational rather than a grantable row.
            optional = true,
        ),
    ),
    compatibility = OsCompatibility(
        minSdk = Build.VERSION_CODES.Q,
        notes = listOf(
            OsNote(
                sinceSdk = Build.VERSION_CODES.Q,
                text = stringResource(R.string.vibration_module_compat_note_effects),
            ),
            OsNote(
                sinceSdk = Build.VERSION_CODES.S,
                text = stringResource(R.string.vibration_module_compat_note_manager),
            ),
        ),
    ),
    firmware = null,
    capabilities = vibrationCapabilities(vibration, root),
)

@Composable
private fun vibrationCapabilities(
    vibration: VibrationState,
    root: VibrationRootAvailability,
): List<ModuleCapability> {
    val available = vibration.isAvailable
    val noMotorMsg = stringResource(R.string.vibration_cap_no_motor)
    val needsRootMsg = stringResource(R.string.vibration_cap_needs_root)

    return listOf(
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_basic_name),
            detail = stringResource(R.string.vibration_cap_basic_detail),
            status = {
                if (available) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_basic_ok))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, noMotorMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_amplitude_name),
            detail = stringResource(R.string.vibration_cap_amplitude_detail),
            status = {
                when {
                    !available -> CapabilityStatus(GadgetStatusKind.Error, noMotorMsg)
                    vibration.hasAmplitudeControl ->
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_amplitude_ok))
                    else ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.vibration_cap_amplitude_none))
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_patterns_name),
            detail = stringResource(R.string.vibration_cap_patterns_detail),
            status = {
                if (available) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_patterns_ok))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, noMotorMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_root_amplitude_name),
            detail = stringResource(R.string.vibration_cap_root_amplitude_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_root_ready))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_root_pwm_name),
            detail = stringResource(R.string.vibration_cap_root_pwm_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_root_ready))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_root_dual_name),
            detail = stringResource(R.string.vibration_cap_root_dual_detail),
            status = {
                when {
                    root.dualReady ->
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.vibration_cap_root_ready))
                    root.rootReady ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.vibration_cap_root_no_dual))
                    else -> CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.vibration_cap_root_rumble_name),
            detail = stringResource(R.string.vibration_cap_root_rumble_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.vibration_cap_root_rumble_caveat))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
    )
}
