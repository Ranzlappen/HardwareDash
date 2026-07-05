package dev.ranzlappen.gadget.localization

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

object LocalizationManager {

    private const val PREFS_NAME = "gadget_settings"
    private const val KEY_LANGUAGE = "selected_language"

    private val _currentLanguage = mutableStateOf(Language.EN)
    val currentLanguage: State<Language> = _currentLanguage

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, Language.EN.code) ?: Language.EN.code
        _currentLanguage.value = Language.entries.firstOrNull { it.code == code } ?: Language.EN
    }

    fun setLanguage(context: Context, language: Language) {
        _currentLanguage.value = language
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }

    /** Load the saved language from prefs — safe to call from services/receivers. */
    fun loadLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, Language.EN.code) ?: Language.EN.code
        return Language.entries.firstOrNull { it.code == code } ?: Language.EN
    }
}
