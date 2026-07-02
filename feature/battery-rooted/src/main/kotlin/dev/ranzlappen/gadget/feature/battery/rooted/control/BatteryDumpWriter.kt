package dev.ranzlappen.gadget.feature.battery.rooted.control

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.battery.control.BatteryControllerResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

private const val LOGBOOK_DIR_NAME = "logbook"
private const val DUMP_FILENAME_PREFIX = "battery-dump-"
private const val DUMP_FILENAME_EXTENSION = ".json"

/**
 * Serialises a battery + thermal snapshot to a timestamped JSON file in
 * `<external files>/logbook/`. Filename pattern:
 * `battery-dump-2026-05-08T15-22-04Z.json`. Returns the absolute path
 * for surfacing to the user via the controller's
 * [BatteryControllerResult.DumpWritten].
 *
 * Uses `org.json.JSONObject` so no additional dependency is needed.
 */
@Singleton
class BatteryDumpWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun write(
        psu: Map<String, Map<String, String>>,
        thermal: List<ThermalZoneSnapshot>,
    ): File? {
        val dir = resolveLogbookDir() ?: return null
        if (!dir.exists() && !dir.mkdirs()) return null

        val payload = JSONObject().apply {
            put("timestamp", isoTimestamp())
            put("device", deviceJson())
            put("power_supply", psuJson(psu))
            put("thermal", thermalJson(thermal))
        }
        val file = File(dir, "$DUMP_FILENAME_PREFIX${filenameTimestamp()}$DUMP_FILENAME_EXTENSION")
        file.writeText(payload.toString(2))
        return file
    }

    private fun resolveLogbookDir(): File? {
        val external = context.getExternalFilesDir(null) ?: return null
        return File(external, LOGBOOK_DIR_NAME)
    }

    private fun deviceJson(): JSONObject = JSONObject().apply {
        put("manufacturer", Build.MANUFACTURER)
        put("model", Build.MODEL)
        put("device", Build.DEVICE)
        put("sdk_int", Build.VERSION.SDK_INT)
        put("release", Build.VERSION.RELEASE)
    }

    private fun psuJson(psu: Map<String, Map<String, String>>): JSONObject =
        JSONObject().apply {
            for ((name, nodes) in psu) {
                put(name, JSONObject(nodes as Map<*, *>))
            }
        }

    private fun thermalJson(zones: List<ThermalZoneSnapshot>): JSONArray = JSONArray().apply {
        for (zone in zones) {
            put(
                JSONObject().apply {
                    put("zone_dir", zone.zoneDir)
                    put("type", zone.type ?: JSONObject.NULL)
                    put("temp", zone.temp ?: JSONObject.NULL)
                    put("mode", zone.mode ?: JSONObject.NULL)
                    put("trip_points", JSONObject(zone.tripPoints as Map<*, *>))
                },
            )
        }
    }

    private fun isoTimestamp(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun filenameTimestamp(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
