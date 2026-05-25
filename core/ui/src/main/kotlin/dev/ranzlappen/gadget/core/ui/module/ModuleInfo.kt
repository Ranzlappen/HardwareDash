package dev.ranzlappen.gadget.core.ui.module

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind

/**
 * Self-describing metadata every Gadget feature module supplies so the
 * shared scaffold can render a consistent **Permissions**,
 * **OS compatibility**, and (optional) **Firmware** block without each
 * module hand-rolling that chrome.
 *
 * This is the future-proof seam for the whole app: new modules and
 * modules migrated from the legacy app fill in a [ModuleInfo] and get
 * the standard sections for free via
 * [dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold]'s `moduleInfo`
 * parameter. Torch is the reference implementation — it requires no
 * runtime permissions, so its [permissions] list is empty (the section
 * then renders a "no permissions required" state) and its [firmware] is
 * `null` (the firmware section is omitted entirely).
 *
 * All members are plain, already-localized strings: build the
 * [ModuleInfo] inside a `@Composable` and resolve `stringResource(...)`
 * at construction so module-specific copy stays in each feature's
 * resources while the generic section chrome lives in `:core:ui`.
 */
@Immutable
data class ModuleInfo(
    val permissions: List<ModulePermission> = emptyList(),
    val compatibility: OsCompatibility,
    val firmware: FirmwareRequirement? = null,
    val capabilities: List<ModuleCapability> = emptyList(),
)

/**
 * One per-function capability row in a module's status block: a named
 * feature (a button, a hardware action) plus a live [status] check that
 * resolves to a green / amber / red [CapabilityStatus]. This is how a
 * module reports, per function and per flavor, exactly what works on this
 * device and what's missing — with an optional inline [CapabilityAction]
 * (request a permission, open settings) to resolve a warning.
 *
 * [status] is a `@Composable` lambda so it can read live signals
 * (`Build.VERSION`, permission grant state, root availability). It is
 * re-invoked whenever [dev.ranzlappen.gadget.core.ui.module.ModuleCapabilitiesSection]
 * recomposes (e.g. after a permission result or `ON_RESUME`), so the
 * badge stays current.
 */
@Immutable
class ModuleCapability(
    val name: String,
    val detail: String? = null,
    val status: @Composable () -> CapabilityStatus,
)

/** Resolved tri-state status of a [ModuleCapability] on this device/flavor. */
@Immutable
data class CapabilityStatus(
    val kind: GadgetStatusKind,
    val message: String,
    val action: CapabilityAction? = null,
)

/** An inline action offered to resolve a non-[GadgetStatusKind.Success]
 *  capability. */
sealed interface CapabilityAction {
    /** Request the given runtime permissions in-app. */
    data class RequestPermissions(val permissions: List<String>) : CapabilityAction

    /** Deep-link to the app's system settings page. */
    data object OpenAppSettings : CapabilityAction

    /** A module-specific action with its own [label] + handler. */
    data class Custom(val label: String, val onClick: () -> Unit) : CapabilityAction
}

/**
 * A single Android runtime permission a module relies on.
 *
 * @property permission the `Manifest.permission.*` string, used both to
 *   query the current grant state and to drive the runtime request.
 * @property label short human-readable name (e.g. "Camera").
 * @property rationale one sentence on *why* the module needs it.
 * @property optional `true` when the module degrades gracefully without
 *   the grant (the section frames it as "enhances" rather than
 *   "required").
 */
@Immutable
data class ModulePermission(
    val permission: String,
    val label: String,
    val rationale: String,
    val optional: Boolean = false,
)

/**
 * OS-version compatibility for a module.
 *
 * @property minSdk the lowest `Build.VERSION.SDK_INT` the module
 *   functions on. The section compares this against the live device SDK
 *   to show a supported / unsupported verdict.
 * @property notes behaviour caveats keyed by the API level they start
 *   applying from (e.g. "on Android 14+ the strobe is time-boxed").
 */
@Immutable
data class OsCompatibility(
    val minSdk: Int,
    val notes: List<OsNote> = emptyList(),
)

/** A single OS-behaviour note that applies from [sinceSdk] upward. */
@Immutable
data class OsNote(
    val sinceSdk: Int,
    val text: String,
)

/**
 * External-hardware firmware requirement for hardware-bridge modules
 * (e.g. a Flipper-style companion device). `null` on [ModuleInfo] for
 * modules that talk only to on-device hardware (like Torch).
 *
 * @property deviceName the companion device this module targets.
 * @property minVersion the lowest firmware version known to work.
 * @property notes optional precaution (e.g. "downgrade unsupported").
 */
@Immutable
data class FirmwareRequirement(
    val deviceName: String,
    val minVersion: String,
    val notes: String? = null,
)
