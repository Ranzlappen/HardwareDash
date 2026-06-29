package dev.ranzlappen.gadget.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for the custom-theme → [androidx.compose.material3.ColorScheme]
 * resolver. Material 3 colour schemes are pure data, so this runs on plain JVM.
 */
class GadgetCustomThemeTest {

    @Test
    fun `Default resolves to null in both brightnesses`() {
        assertNull(GadgetCustomTheme.Default.colorScheme(dark = true))
        assertNull(GadgetCustomTheme.Default.colorScheme(dark = false))
    }

    @Test
    fun `every non-Default theme resolves to a scheme in both brightnesses`() {
        GadgetCustomTheme.entries
            .filter { it != GadgetCustomTheme.Default }
            .forEach { theme ->
                assertNotNull("${theme.name} dark", theme.colorScheme(dark = true))
                assertNotNull("${theme.name} light", theme.colorScheme(dark = false))
            }
    }

    @Test
    fun `high contrast and pastel differ between dark and light`() {
        assertEquals(GadgetHighContrastDarkColorScheme, GadgetCustomTheme.HighContrast.colorScheme(true))
        assertEquals(GadgetHighContrastLightColorScheme, GadgetCustomTheme.HighContrast.colorScheme(false))
        assertEquals(GadgetPastelDarkColorScheme, GadgetCustomTheme.Pastel.colorScheme(true))
        assertEquals(GadgetPastelLightColorScheme, GadgetCustomTheme.Pastel.colorScheme(false))
    }

    @Test
    fun `amoled is dark-only and falls back to the standard light palette`() {
        assertEquals(GadgetAmoledColorScheme, GadgetCustomTheme.AmoledTrue.colorScheme(true))
        assertSame(GadgetLightColorScheme, GadgetCustomTheme.AmoledTrue.colorScheme(false))
    }
}
