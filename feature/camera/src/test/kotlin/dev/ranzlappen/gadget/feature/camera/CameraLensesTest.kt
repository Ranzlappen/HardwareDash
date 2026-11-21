package dev.ranzlappen.gadget.feature.camera

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraLensesTest {

    @Test
    fun `three back lenses get ultra-wide, main and tele by focal length`() {
        val lenses = assignLensRoles(
            listOf(
                Triple("0", LensFacing.Back, 5.4f),
                Triple("1", LensFacing.Front, 3.0f),
                Triple("2", LensFacing.Back, 2.2f),
                Triple("3", LensFacing.Back, 9.6f),
            ),
        )
        assertEquals(
            listOf("2" to LensRole.UltraWide, "0" to LensRole.Main, "3" to LensRole.Tele, "1" to LensRole.Front),
            lenses.map { it.cameraId to it.role },
        )
    }

    @Test
    fun `a single logical back camera is main`() {
        val lenses = assignLensRoles(listOf(Triple("0", LensFacing.Back, 6.0f), Triple("1", LensFacing.Front, 3.0f)))
        assertEquals(LensRole.Main, lenses.first { it.cameraId == "0" }.role)
    }

    @Test
    fun `unknown focal lengths fall back to main`() {
        val lenses = assignLensRoles(listOf(Triple("0", LensFacing.Back, null), Triple("2", LensFacing.Back, 2.0f)))
        assertTrue(lenses.all { it.role == LensRole.Main })
    }

    @Test
    fun `zoom presets cover ultra-wide and stay inside the range`() {
        assertEquals(listOf(0.6f, 1f, 2f, 5f, 10f), zoomPresets(0.6f, 10f))
        assertEquals(listOf(1f, 2f), zoomPresets(1f, 4f))
        assertEquals(listOf(1f), zoomPresets(1f, 1f))
        assertEquals(emptyList(), zoomPresets(0f, 0f))
    }

    @Test
    fun `zoom and exposure are formatted for chips and sliders`() {
        assertEquals("1×", formatZoom(1f))
        assertEquals("0.6×", formatZoom(0.6f))
        assertEquals("2.5×", formatZoom(2.49f))
        assertEquals("+0.7 EV", formatEv(2, 1f / 3f))
        assertEquals("−1.3 EV", formatEv(-4, 1f / 3f))
        assertEquals("0 EV", formatEv(0, 1f / 3f))
    }

    @Test
    fun `photo names are timestamped and go to Pictures-Gadget`() {
        assertTrue(Regex("Gadget_\\d{8}_\\d{6}").matches(photoFileName(0L)))
        assertEquals("Pictures/Gadget", PHOTO_RELATIVE_PATH)
    }
}
