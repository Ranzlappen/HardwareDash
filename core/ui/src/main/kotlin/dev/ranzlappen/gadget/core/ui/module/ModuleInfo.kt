package dev.ranzlappen.gadget.core.ui.module

import androidx.compose.runtime.Immutable

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
)

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
