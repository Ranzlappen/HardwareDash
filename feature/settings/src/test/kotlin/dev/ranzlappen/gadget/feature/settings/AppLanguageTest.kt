package dev.ranzlappen.gadget.feature.settings

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AppLanguage.fromTag] — the mapping back from a persisted /
 * `AppCompatDelegate`-reported language tag to the enum, including the
 * default-to-[AppLanguage.SystemDefault] fallback for null, empty, and
 * unrecognized tags.
 */
class AppLanguageTest {

    @Test
    fun `null tag maps to SystemDefault`() {
        assertEquals(AppLanguage.SystemDefault, AppLanguage.fromTag(null))
    }

    @Test
    fun `en maps to English`() {
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en"))
    }

    @Test
    fun `de maps to German`() {
        assertEquals(AppLanguage.German, AppLanguage.fromTag("de"))
    }

    @Test
    fun `es maps to Spanish`() {
        assertEquals(AppLanguage.Spanish, AppLanguage.fromTag("es"))
    }

    @Test
    fun `fr maps to French`() {
        assertEquals(AppLanguage.French, AppLanguage.fromTag("fr"))
    }

    @Test
    fun `unrecognized tag falls back to SystemDefault`() {
        assertEquals(AppLanguage.SystemDefault, AppLanguage.fromTag("xx"))
    }

    @Test
    fun `empty tag falls back to SystemDefault`() {
        assertEquals(AppLanguage.SystemDefault, AppLanguage.fromTag(""))
    }
}
