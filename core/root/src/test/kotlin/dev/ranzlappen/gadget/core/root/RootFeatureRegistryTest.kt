package dev.ranzlappen.gadget.core.root

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for [RootFeatureRegistry] — the single source of truth mapping
 * every [RootFeatureKey] to its safety policy (rate-limit window/cap,
 * confirm-required, write-capable). A gap here is not cosmetic: the
 * rooted-flavor [RootSafetyGate] and Settings UI both call
 * `descriptor(key)` unconditionally, so a missing entry means a crash
 * ([IllegalStateException] from `descriptor()`'s `error(...)`) the first
 * time that feature is ever gated or rendered.
 */
class RootFeatureRegistryTest {

    private val registry = RootFeatureRegistry()

    /**
     * Every `data object` declared inside the [RootFeatureKey] sealed class,
     * found via plain `java.lang.Class` reflection — no kotlin-reflect
     * dependency required. Kotlin compiles each nested `data object` to a
     * static nested class with a public static `INSTANCE` field, so this
     * walks the *actual* compiled inventory of feature keys rather than a
     * hand-maintained list in this test that could itself drift out of sync.
     */
    private fun declaredFeatureKeys(): List<RootFeatureKey> =
        RootFeatureKey::class.java.declaredClasses
            .filter { RootFeatureKey::class.java.isAssignableFrom(it) }
            .mapNotNull { cls ->
                runCatching { cls.getField("INSTANCE").get(null) as RootFeatureKey }.getOrNull()
            }

    @Test
    fun `reflection discovers the RootFeatureKey inventory`() {
        // Sanity floor so a reflection failure (e.g. a future Kotlin/JVM
        // target change breaking the INSTANCE-field assumption) shows up as
        // an obviously-wrong count instead of silently vacuous assertions
        // in the tests below.
        assertTrue(
            declaredFeatureKeys().size > 50,
            "expected to discover the full RootFeatureKey inventory via reflection, " +
                "found only ${declaredFeatureKeys().size}",
        )
    }

    @Test
    fun `every declared RootFeatureKey has a registered descriptor`() {
        val missing = declaredFeatureKeys().filter { key ->
            runCatching { registry.descriptor(key) }.isFailure
        }

        assertTrue(
            missing.isEmpty(),
            "RootFeatureRegistry has no descriptor for: ${missing.map { it.id }}. " +
                "descriptor(key) will throw IllegalStateException for these at runtime.",
        )
    }

    @Test
    fun `descriptor key always matches the requested key`() {
        for (key in declaredFeatureKeys()) {
            val descriptor = runCatching { registry.descriptor(key) }.getOrNull() ?: continue
            assertEquals(key, descriptor.key, "descriptor for ${key.id} is keyed under a different id")
        }
    }

    @Test
    fun `allDescriptors size matches the number of registered keys`() {
        val registered = declaredFeatureKeys().count { key -> runCatching { registry.descriptor(key) }.isSuccess }
        assertEquals(registered, registry.allDescriptors().size)
    }

    @Test
    fun `every feature id is unique`() {
        // RootPrefKeys.featureEnabledKey(feature) builds the DataStore key
        // suffix from `feature.id`. A duplicate id would silently alias two
        // unrelated features' persisted opt-in / rate-limit state.
        val ids = declaredFeatureKeys().map { it.id }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(duplicates.isEmpty(), "duplicate RootFeatureKey ids: $duplicates")
    }

    @Test
    fun `every registered feature defaults to opted-out`() {
        // Core safety invariant of the module: every rooted feature is
        // opt-in, never opt-out-by-default.
        for (descriptor in registry.allDescriptors()) {
            assertTrue(!descriptor.defaultOn, "${descriptor.key.id} must default to disabled")
        }
    }

    @Test
    fun `opt-out-only features carry no rate limit`() {
        // BackupFullData and WifiInternalTools predate the rate-limiter and
        // are gated purely by the user opt-out toggle.
        assertNull(registry.descriptor(RootFeatureKey.BackupFullData).limit)
        assertNull(registry.descriptor(RootFeatureKey.WifiInternalTools).limit)
    }

    @Test
    fun `single-shot dangerous features cap at exactly one invocation per window`() {
        val singleShot = listOf(
            RootFeatureKey.TorchThermalOverride to 5.minutes,
            RootFeatureKey.VibrationSustainedRumble to 5.minutes,
            RootFeatureKey.SensorsHighPollingExpert to 5.minutes,
            RootFeatureKey.SensorsOverclock to 5.minutes,
            RootFeatureKey.BatteryChargingProfile to 10.minutes,
            RootFeatureKey.BatteryThermalBypass to 10.minutes,
            RootFeatureKey.WifiTxPowerOverride to 5.minutes,
            RootFeatureKey.BluetoothTxPowerOverride to 5.minutes,
            RootFeatureKey.AutomationSystemSettingsOverride to 5.minutes,
            RootFeatureKey.NotificationListenerAccess to 5.minutes,
            RootFeatureKey.KeepAliveDozeBypass to 5.minutes,
            RootFeatureKey.KeepAlivePmGrant to 5.minutes,
            RootFeatureKey.StorageFstrim to 5.minutes,
            RootFeatureKey.DisplayDensityOverride to 5.minutes,
            RootFeatureKey.AudioMuteAllStreams to 5.minutes,
            RootFeatureKey.BatteryHoldSoc to 5.minutes,
            RootFeatureKey.BatteryWirelessCoilCurrent to 5.minutes,
            RootFeatureKey.AdbOverNetwork to 5.minutes,
        )

        for ((key, window) in singleShot) {
            val limit = assertNotNull(registry.descriptor(key).limit, "${key.id} must have a rate limit")
            assertEquals(1, limit.maxInvocations, "${key.id} must cap at a single invocation per window")
            assertEquals(window, limit.window, "${key.id} window mismatch")
        }
    }

    @Test
    fun `GPS spoofing features use bespoke higher invocation caps`() {
        // These intentionally deviate from the generic EXTREME_OPS_* tiers
        // because a mock-location route needs many points per minute.
        val locationOverride = assertNotNull(registry.descriptor(RootFeatureKey.GpsLocationOverride).limit)
        assertEquals(60.seconds, locationOverride.window)
        assertEquals(60, locationOverride.maxInvocations)

        val routeSimulation = assertNotNull(registry.descriptor(RootFeatureKey.GpsRouteSimulation).limit)
        assertEquals(60.seconds, routeSimulation.window)
        assertEquals(30, routeSimulation.maxInvocations)

        // The one-shot LSPosed hook installer is capped like any other
        // single-shot dangerous feature, just on its own named window.
        val lsposedInstall = assertNotNull(registry.descriptor(RootFeatureKey.GpsLsposedHookInstall).limit)
        assertEquals(5.minutes, lsposedInstall.window)
        assertEquals(1, lsposedInstall.maxInvocations)
    }

    @Test
    fun `write-capable features that mutate hardware state require explicit confirmation`() {
        // Not a universal rule (a few write-capable toggles like
        // AppsUnfreezeApp are low-risk and skip confirmation), but every
        // *destructive single-shot* feature above must both confirm and be
        // flagged write-capable so Rooted Monitor Safety Mode can suppress it.
        val destructiveSingleShot = listOf(
            RootFeatureKey.TorchThermalOverride,
            RootFeatureKey.VibrationSustainedRumble,
            RootFeatureKey.BatteryChargingProfile,
            RootFeatureKey.BatteryThermalBypass,
            RootFeatureKey.StorageFstrim,
        )

        for (key in destructiveSingleShot) {
            val descriptor = registry.descriptor(key)
            assertTrue(descriptor.requiresExplicitConfirm, "${key.id} must require explicit confirmation")
            assertTrue(descriptor.isWriteCapable, "${key.id} must be flagged write-capable")
        }
    }

    @Test
    fun `read-only diagnostic dumps are not write-capable`() {
        val readOnlyDumps = listOf(
            RootFeatureKey.SensorsSysfsRead,
            RootFeatureKey.BatteryFuelGaugeRaw,
            RootFeatureKey.BatteryFullDump,
            RootFeatureKey.DiagnosticsTailLogcat,
            RootFeatureKey.DiagnosticsDumpMemInfo,
            RootFeatureKey.StorageDumpDiskstats,
        )

        for (key in readOnlyDumps) {
            assertTrue(!registry.descriptor(key).isWriteCapable, "${key.id} must not be flagged write-capable")
        }
    }
}
