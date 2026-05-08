package com.gadget.battery

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

private const val LOGBOOK_DIR_NAME = "logbook"
private const val HEALTH_FILENAME_PREFIX = "battery-health-"
private const val HEALTH_FILENAME_EXTENSION = ".json"

private val FUEL_GAUGE_DIR_PREFIXES = listOf(
    "battery",
    "bms",
    "main",
    "maxfg",
    "max1720x",
    "qpnp-fg",
    "qg-battery",
)

private val CYCLE_COUNT_NODES = listOf(
    "battery.cycle_count",
    "bms.cycle_count",
    "maxfg.cycle_count",
    "main.cycle_count",
)
private val DESIGN_CAPACITY_NODES = listOf(
    "battery.charge_full_design",
    "bms.charge_full_design",
    "main.charge_full_design",
)
private val FULL_CAPACITY_NODES = listOf(
    "battery.charge_full",
    "bms.charge_full",
    "main.charge_full",
)

/**
 * Reads cycle-count, design-capacity, and full-charge-capacity nodes
 * across the most common fuel-gauge IC layouts (qpnp-fg, MAX1720x,
 * qg-battery). Optionally persists a JSON snapshot to the Logbook
 * directory using the same `<external files>/logbook/` convention as
 * [BatteryDumpWriter].
 */
@Singleton
class BatteryHealthReader @Inject constructor(
    private val psuSysfs: PowerSupplySysfs,
    @ApplicationContext private val context: Context,
) {
    suspend fun read(persist: Boolean = true): BatteryControllerResult {
        val merged = LinkedHashMap<String, String>()
        val psuDirs = psuSysfs.listPsuDirs()
        if (psuDirs.isEmpty()) return BatteryControllerResult.Unsupported
        for (dir in psuDirs) {
            val name = dir.trimEnd('/').substringAfterLast('/')
            if (FUEL_GAUGE_DIR_PREFIXES.none { name.startsWith(it) }) continue
            val nodes = psuSysfs.readPsuMap(dir)
            for ((k, v) in nodes) merged["$name.$k"] = v
        }
        if (merged.isEmpty()) return BatteryControllerResult.Unsupported
        val cycleCount = pickInt(merged, CYCLE_COUNT_NODES)
        val design = pickLong(merged, DESIGN_CAPACITY_NODES)
        val full = pickLong(merged, FULL_CAPACITY_NODES)
        val persistedFile = if (persist) writeLogbook(merged, cycleCount, design, full) else null
        return BatteryControllerResult.BatteryHealthReading(
            nodes = merged,
            cycleCount = cycleCount,
            designCapacityUah = design,
            fullChargeCapacityUah = full,
            persistedFile = persistedFile,
        )
    }

    private fun pickInt(nodes: Map<String, String>, candidates: List<String>): Int? {
        for (key in candidates) {
            val v = nodes[key]?.toIntOrNull()
            if (v != null) return v
        }
        return null
    }

    private fun pickLong(nodes: Map<String, String>, candidates: List<String>): Long? {
        for (key in candidates) {
            val v = nodes[key]?.toLongOrNull()
            if (v != null) return v
        }
        return null
    }

    private fun writeLogbook(
        nodes: Map<String, String>,
        cycleCount: Int?,
        designCapacityUah: Long?,
        fullChargeCapacityUah: Long?,
    ): String? {
        val external = context.getExternalFilesDir(null) ?: return null
        val dir = File(external, LOGBOOK_DIR_NAME)
        if (!dir.exists() && !dir.mkdirs()) return null
        val payload = JSONObject().apply {
            put("timestamp", isoTimestamp())
            put("device", deviceJson())
            put("cycle_count", cycleCount ?: JSONObject.NULL)
            put("design_capacity_uah", designCapacityUah ?: JSONObject.NULL)
            put("full_charge_capacity_uah", fullChargeCapacityUah ?: JSONObject.NULL)
            put("nodes", JSONObject(nodes as Map<*, *>))
        }
        val file = File(
            dir,
            "$HEALTH_FILENAME_PREFIX${filenameTimestamp()}$HEALTH_FILENAME_EXTENSION",
        )
        file.writeText(payload.toString(2))
        return file.absolutePath
    }

    private fun deviceJson(): JSONObject = JSONObject().apply {
        put("manufacturer", Build.MANUFACTURER)
        put("model", Build.MODEL)
        put("device", Build.DEVICE)
        put("sdk_int", Build.VERSION.SDK_INT)
        put("release", Build.VERSION.RELEASE)
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
