package dev.ranzlappen.gadget.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.emergency.EmergencyResetCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for the root-safety framework. Composable code in
 * `src/main` reaches the capability / toggle / emergency-reset services via
 * `EntryPointAccessors.fromApplication(...)` rather than `@Inject`, since
 * `@Composable` functions can't take constructor parameters.
 *
 * **Scope (post refactor-2026 seam dissolution).** This entry point used to
 * also hand out every rooted **feature controller** (`CameraController`,
 * `StorageController`, …) to a matching `ui/Rooted*ExtrasSection` composable.
 * Those controllers have all migrated into their own `:feature:<name>-rooted`
 * modules and are consumed by the automation + monitoring seams; the legacy
 * interactive "Root extras" sections that reached them through this entry
 * point were never re-surfaced in the modular feature screens, so they were
 * removed as dead code. Re-building that interactive rooted UX natively inside
 * each feature screen (inject the controller, gate on root) is tracked as a
 * Phase-3 epic — see
 * https://github.com/Ranzlappen/HardwareDash/issues/94.
 *
 * What remains here is only the cross-cutting **safety framework**, still
 * consumed live by `MainActivity` (`FatalLaunchScreen`), the
 * `RootedFeatureTogglesCard` (settings + torch nav), and the emergency-reset
 * surface. The framework types themselves live in `:core:root`; this thin
 * entry point stays in `:app/src/main/` only so those `dev.ranzlappen.gadget.root.ui.*`
 * composables can reach them without an `@Inject` site.
 *
 * Mirrors the `AppsEntryPoint` shape.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RootFeaturesEntryPoint {
    fun capabilityRegistry(): RootCapabilityRegistry
    fun featureRegistry(): RootFeatureRegistry
    fun featureToggles(): RootFeatureToggles
    fun emergencyResetCoordinator(): EmergencyResetCoordinator
}
