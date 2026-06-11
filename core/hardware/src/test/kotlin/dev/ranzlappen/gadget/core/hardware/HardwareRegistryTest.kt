package dev.ranzlappen.gadget.core.hardware

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareRegistryTest {

    private class FakeSource(
        key: String,
        name: String,
        private val value: Float,
    ) : MetricSource {
        override val descriptor = MetricDescriptor(
            metricKey = key,
            displayName = name,
            unit = "cm",
            min = 0f,
            max = 10f,
            category = MetricCategory.Sensor,
        )

        override suspend fun sample(): Float = value
    }

    private val registry = HardwareRegistry(
        mapOf(
            "proximity" to FakeSource("proximity", "Proximity", 4f),
            "light" to FakeSource("light", "Ambient light", 120f),
        ),
    )

    @Test
    fun signals_listsEveryRegisteredDescriptor_sortedByDisplayName() {
        val signals = registry.signals()
        assertEquals(listOf("Ambient light", "Proximity"), signals.map { it.displayName })
        assertEquals(listOf("light", "proximity"), signals.map { it.metricKey })
    }

    @Test
    fun descriptor_byKey_andUnknownIsNull() {
        assertEquals("Proximity", registry.descriptor("proximity")?.displayName)
        assertNull(registry.descriptor("battery_level"))
    }

    @Test
    fun isRegistered_reflectsTheMap() {
        assertTrue(registry.isRegistered("proximity"))
        assertFalse(registry.isRegistered("battery_level"))
    }

    @Test
    fun read_samplesTheSource_unknownIsNull() = runTest {
        assertEquals(4f, registry.read("proximity"))
        assertNull(registry.read("battery_level"))
    }

    @Test
    fun emptyRegistry_isEmptyNotCrashing() {
        val empty = HardwareRegistry(emptyMap())
        assertTrue(empty.signals().isEmpty())
    }
}
