package dev.ranzlappen.gadget.core.widgetkit.pin

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs.Companion.selectSolePending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the sole-match + predicate-filter selection logic behind
 * [PendingWidgetConfigs.claimSolePending] — the recovery path that rescues a
 * freshly-pinned widget whose OS success callback never fired.
 *
 * The DataStore/IO layer (`enqueue` / `delete` / `getAll`) is exercised by
 * `FeaturePreferencesTest` in `:core:datastore`; this verifies the pure
 * selection rule in isolation: a single matching entry is returned, but an
 * ambiguous (2+) match defers to `null` rather than guessing — guessing would
 * risk swapping two same-type widgets' configs.
 */
class PendingWidgetConfigsTest {

    /** Minimal [WidgetKitConfig] carrying a [kind] discriminator so the
     *  predicate can filter as torch filters by `WidgetType`. */
    private data class FakeConfig(
        val kind: String,
        override val displayName: String = kind,
        override val removed: Boolean = false,
        override val schemaVersion: Int = 1,
        override val appearance: WidgetAppearance = WidgetAppearance(),
    ) : WidgetKitConfig

    private fun entry(key: Int, kind: String): Pair<Int, PendingEntry<FakeConfig>> =
        key to PendingEntry(
            token = "tok-$key",
            savedAtMs = key.toLong(),
            config = FakeConfig(kind),
        )

    @Test
    fun `selects the single entry matching the predicate`() {
        val snapshot = mapOf(
            entry(1, "strobe"),
            entry(2, "flashlight"),
        )

        val match = selectSolePending(snapshot) { it.kind == "strobe" }

        assertEquals(1, match?.key)
        assertEquals("strobe", match?.value?.config?.kind)
    }

    @Test
    fun `defers (null) when two entries match — can't correlate to an appWidgetId`() {
        // Two strobes pinned back-to-back: guessing one would risk swapping
        // their configs, so the sole-match rule returns null and lets the
        // provider self-heal a default the user can re-edit.
        val snapshot = mapOf(
            entry(1, "strobe"),
            entry(2, "strobe"),
        )

        val match = selectSolePending(snapshot) { it.kind == "strobe" }

        assertNull(match)
    }

    @Test
    fun `predicate isolates kinds so one strobe + one flashlight is still sole`() {
        val snapshot = mapOf(
            entry(1, "strobe"),
            entry(2, "flashlight"),
        )

        // Each kind has exactly one entry, so both resolve unambiguously.
        assertEquals(1, selectSolePending(snapshot) { it.kind == "strobe" }?.key)
        assertEquals(2, selectSolePending(snapshot) { it.kind == "flashlight" }?.key)
    }

    @Test
    fun `returns null when nothing matches`() {
        val snapshot = mapOf(
            entry(1, "flashlight"),
            entry(2, "flashlight"),
        )

        val match = selectSolePending(snapshot) { it.kind == "strobe" }

        assertNull(match)
    }

    @Test
    fun `returns null for an empty snapshot`() {
        val match = selectSolePending(emptyMap<Int, PendingEntry<FakeConfig>>()) { true }

        assertNull(match)
    }
}
