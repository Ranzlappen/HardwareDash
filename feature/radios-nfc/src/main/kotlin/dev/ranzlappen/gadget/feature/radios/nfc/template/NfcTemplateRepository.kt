package dev.ranzlappen.gadget.feature.radios.nfc.template

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class NfcTemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val templates: List<NfcTemplate> by lazy {
        runCatching {
            val raw = context.assets.open("nfc_templates.json").bufferedReader().readText()
            json.decodeFromString<List<NfcTemplate>>(raw)
        }.getOrDefault(emptyList())
    }
}
