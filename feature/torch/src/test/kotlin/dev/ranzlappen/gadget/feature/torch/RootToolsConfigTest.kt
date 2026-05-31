package dev.ranzlappen.gadget.feature.torch

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [TorchRootToolsConfig]: the `@Serializable` round-trip (so a
 * field rename / default-value drift surfaces as a test failure rather than a
 * silent on-disk incompatibility) and the [TorchRootToolsConfig.coercedTo]
 * clamping that keeps a corrupted or stale value from driving the rooted tools
 * past the hardware limit.
 */
class RootToolsConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `config round-trips through json`() {
        val original = TorchRootToolsConfig(
            boostBrightnessPercent = 130,
            dutyFrequencyHz = 25,
            dutyPercent = 40,
            dutyDurationMs = 4_000L,
            multiLedDurationMs = 2_000L,
            multiLedIncludeScreen = true,
            thermalFrequencyHz = 15,
            thermalDutyPercent = 35,
            thermalDurationMs = 20_000L,
        )

        val decoded = json.decodeFromString(
            TorchRootToolsConfig.serializer(),
            json.encodeToString(TorchRootToolsConfig.serializer(), original),
        )

        assertEquals(original, decoded)
    }

    @Test
    fun `empty object decodes to defaults`() {
        val decoded = json.decodeFromString(TorchRootToolsConfig.serializer(), "{}")

        assertEquals(TorchRootToolsConfig(), decoded)
    }

    @Test
    fun `coercedTo clamps brightness to the live ceiling`() {
        // The on-disk value asks for 150, but the live ceiling is 100 (no LED
        // node found) — the run must not exceed it.
        val coerced = TorchRootToolsConfig(boostBrightnessPercent = 150)
            .coercedTo(maxBrightnessPercent = 100)

        assertEquals(100, coerced.boostBrightnessPercent)
    }

    @Test
    fun `coercedTo allows brightness up to a raised ceiling`() {
        val coerced = TorchRootToolsConfig(boostBrightnessPercent = 150)
            .coercedTo(maxBrightnessPercent = 150)

        assertEquals(150, coerced.boostBrightnessPercent)
    }

    @Test
    fun `coercedTo clamps thermal duration to the hard ceiling`() {
        val coerced = TorchRootToolsConfig(thermalDurationMs = 60_000L)
            .coercedTo(maxBrightnessPercent = 150)

        assertEquals(TorchRootToolsConfig.MAX_THERMAL_DURATION_MS, coerced.thermalDurationMs)
    }

    @Test
    fun `coercedTo clamps out-of-range strobe params`() {
        val coerced = TorchRootToolsConfig(
            dutyFrequencyHz = 9_999,
            dutyPercent = 0,
            dutyDurationMs = 0L,
        ).coercedTo(maxBrightnessPercent = 150)

        assertEquals(TorchRootToolsConfig.MAX_HZ, coerced.dutyFrequencyHz)
        assertEquals(TorchRootToolsConfig.MIN_DUTY, coerced.dutyPercent)
        assertEquals(TorchRootToolsConfig.MIN_DURATION_MS, coerced.dutyDurationMs)
    }
}
