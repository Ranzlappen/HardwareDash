package dev.ranzlappen.gadget.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the persisted names of [CustomThemeOption]. The repository stores the
 * enum via `.name`, so renaming a constant would silently reset every user's
 * saved palette — pin the exact strings here.
 */
class CustomThemeOptionTest {

    @Test
    fun `persisted names are stable`() {
        assertEquals(
            listOf("Default", "HighContrast", "AmoledTrue", "Pastel"),
            CustomThemeOption.entries.map { it.name },
        )
    }

    @Test
    fun `valueOf round-trips every option`() {
        CustomThemeOption.entries.forEach { option ->
            assertEquals(option, CustomThemeOption.valueOf(option.name))
        }
    }

    @Test
    fun `default preferences use the Default palette`() {
        assertEquals(CustomThemeOption.Default, UserPreferences().customTheme)
    }
}
