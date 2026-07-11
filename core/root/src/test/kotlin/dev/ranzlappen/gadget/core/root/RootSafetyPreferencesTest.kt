package dev.ranzlappen.gadget.core.root

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [RootPrefKeys]. The class-level doc comment on
 * [RootPrefKeys] is explicit that these key strings are stable-forever:
 * "DO NOT rename, doing so would silently reset the user's preference."
 * These tests pin the exact generated key names down so a refactor that
 * touches the string templates fails loudly here instead of shipping a
 * silent preference reset.
 */
class RootSafetyPreferencesTest {

    @Test
    fun `static keys use their documented stable names`() {
        assertEquals("root_master_enabled", RootPrefKeys.MasterEnabled.name)
        assertEquals("root_monitor_safety_mode", RootPrefKeys.MonitorSafetyMode.name)
        assertEquals("root_master_safety_initialized", RootPrefKeys.MasterSafetyInitialized.name)
        assertEquals("root_rooted_acknowledged", RootPrefKeys.RootedAcknowledged.name)
    }

    @Test
    fun `per-feature keys are derived from the feature id with the documented prefixes`() {
        val feature = RootFeatureKey.TorchThermalOverride

        assertEquals("root_feat_enabled_torch_thermal_override", RootPrefKeys.featureEnabledKey(feature).name)
        assertEquals(
            "root_feat_window_start_torch_thermal_override",
            RootPrefKeys.featureWindowStartKey(feature).name,
        )
        assertEquals(
            "root_feat_invocations_torch_thermal_override",
            RootPrefKeys.featureInvocationCountKey(feature).name,
        )
    }

    @Test
    fun `the three per-feature key builders never collide for the same feature`() {
        val feature = RootFeatureKey.VibrationSustainedRumble

        val names = setOf(
            RootPrefKeys.featureEnabledKey(feature).name,
            RootPrefKeys.featureWindowStartKey(feature).name,
            RootPrefKeys.featureInvocationCountKey(feature).name,
        )

        assertEquals(3, names.size, "each builder must produce a distinct DataStore key for the same feature")
    }

    @Test
    fun `per-feature keys never collide across different features`() {
        // A collision here would mean two unrelated rooted features share
        // persisted opt-in / rate-limit state.
        val features = listOf(
            RootFeatureKey.TorchThermalOverride,
            RootFeatureKey.TorchExtremeBrightness,
            RootFeatureKey.VibrationSustainedRumble,
            RootFeatureKey.BatteryChargingProfile,
            RootFeatureKey.AdbOverNetwork,
        )

        val enabledKeyNames = features.map { RootPrefKeys.featureEnabledKey(it).name }
        assertEquals(features.size, enabledKeyNames.toSet().size)

        val windowKeyNames = features.map { RootPrefKeys.featureWindowStartKey(it).name }
        assertEquals(features.size, windowKeyNames.toSet().size)

        val invocationKeyNames = features.map { RootPrefKeys.featureInvocationCountKey(it).name }
        assertEquals(features.size, invocationKeyNames.toSet().size)
    }

    @Test
    fun `per-feature keys never collide with the static keys`() {
        val staticNames = setOf(
            RootPrefKeys.MasterEnabled.name,
            RootPrefKeys.MonitorSafetyMode.name,
            RootPrefKeys.MasterSafetyInitialized.name,
            RootPrefKeys.RootedAcknowledged.name,
        )
        val perFeatureName = RootPrefKeys.featureEnabledKey(RootFeatureKey.TorchThermalOverride).name

        assertTrue(perFeatureName !in staticNames)
    }
}
