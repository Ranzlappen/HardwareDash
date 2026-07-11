package dev.ranzlappen.gadget.feature.settings

/**
 * The app's supported UI languages (W8) — kept in lock-step with
 * `app/src/main/res/xml/locales_config.xml` and the `values-<tag>/`
 * resource directories across the module graph. [SystemDefault] clears
 * the per-app override so the OS locale (or the user's Android-13+
 * system-level per-app language pick) applies instead.
 */
enum class AppLanguage(internal val tag: String?) {
    SystemDefault(null),
    English("en"),
    German("de"),
    Spanish("es"),
    French("fr"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SystemDefault
    }
}
