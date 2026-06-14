package dev.ranzlappen.gadget.feature.camera

import kotlinx.serialization.Serializable

@Serializable
data class BarcodeResult(
    val id: String,
    val rawValue: String,
    val format: String,
    val displayType: String,
    val timestamp: Long,
) {
    fun isUrl(): Boolean = displayType == "URL"

    fun isWifi(): Boolean = displayType == "WiFi"

    fun parsedWifi(): ParsedWifi? {
        if (!isWifi()) return null
        // Format: WIFI:T:WPA;S:MyNetwork;P:password;;
        val map = mutableMapOf<String, String>()
        rawValue.removePrefix("WIFI:").trimEnd(';').split(';').forEach { part ->
            val colon = part.indexOf(':')
            if (colon > 0) map[part.substring(0, colon)] = part.substring(colon + 1)
        }
        val ssid = map["S"] ?: return null
        return ParsedWifi(
            ssid = ssid,
            password = map["P"] ?: "",
            type = map["T"] ?: "nopass",
        )
    }

    data class ParsedWifi(val ssid: String, val password: String, val type: String)
}
