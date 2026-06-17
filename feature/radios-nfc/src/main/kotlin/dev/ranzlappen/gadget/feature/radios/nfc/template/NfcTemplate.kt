package dev.ranzlappen.gadget.feature.radios.nfc.template

import kotlinx.serialization.Serializable

@Serializable
data class NfcTemplate(
    val id: String,
    val name: String,
    val category: String,
    val mode: String,
    val template: String,
) {
    val placeholders: List<String>
        get() = Regex("""\{\{(\w+)\}\}""").findAll(template).map { it.groupValues[1] }.distinct().toList()

    fun resolve(values: Map<String, String>): String =
        values.entries.fold(template) { acc, (k, v) -> acc.replace("{{$k}}", v) }
}
