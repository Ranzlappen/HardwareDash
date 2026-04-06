package com.hardwaredash.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Defines every metric that can be shown in a home screen widget.
 * Each enum entry knows how to fetch its current value from the system.
 */
enum class WidgetMetric(
    val key: String,
    val displayName: String,
    val category: String,
    val unit: String,
    val iconResName: String,
) {
    // ── Battery ──────────────────────────────────────────────────────────────
    BATTERY_LEVEL("battery_level", "Battery Level", "Battery", "%", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            return if (scale > 0) "${(lvl * 100) / scale}%" else "--"
        }
    },
    BATTERY_STATUS("battery_status", "Battery Status", "Battery", "", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }
        }
    },
    BATTERY_TEMPERATURE("battery_temp", "Battery Temp", "Battery", "°C", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            return "${"%.1f".format(temp)}°C"
        }
    },
    BATTERY_VOLTAGE("battery_voltage", "Battery Voltage", "Battery", "V", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val v = (intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
            return "${"%.3f".format(v)} V"
        }
    },
    BATTERY_HEALTH("battery_health", "Battery Health", "Battery", "", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
        }
    },
    BATTERY_CURRENT("battery_current", "Current Draw", "Battery", "mA", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val uA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            return "${uA / 1000} mA"
        }
    },
    BATTERY_CHARGE_TIME("battery_charge_time", "Charge Time", "Battery", "", "ic_battery") {
        override fun fetch(ctx: Context): String {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val ms = bm.computeChargeTimeRemaining()
            if (ms <= 0) return "N/A"
            val mins = ms / 1000 / 60
            return "${mins / 60}h ${mins % 60}m"
        }
    },

    // ── WiFi ─────────────────────────────────────────────────────────────────
    WIFI_SSID("wifi_ssid", "WiFi SSID", "Network", "", "ic_wifi") {
        override fun fetch(ctx: Context): String {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wm.isWifiEnabled) return "WiFi Off"
            @Suppress("DEPRECATION")
            val ssid = wm.connectionInfo?.ssid?.removeSurrounding("\"") ?: ""
            return if (ssid.isNotBlank() && ssid != "<unknown ssid>") ssid else "Not connected"
        }
    },
    WIFI_SIGNAL("wifi_signal", "WiFi Signal", "Network", "dBm", "ic_wifi") {
        override fun fetch(ctx: Context): String {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wm.isWifiEnabled) return "Off"
            @Suppress("DEPRECATION")
            val rssi = wm.connectionInfo?.rssi ?: 0
            val pct = WifiManager.calculateSignalLevel(rssi, 100)
            return "$rssi dBm ($pct%)"
        }
    },
    WIFI_SPEED("wifi_speed", "WiFi Link Speed", "Network", "Mbps", "ic_wifi") {
        override fun fetch(ctx: Context): String {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wm.isWifiEnabled) return "Off"
            @Suppress("DEPRECATION")
            return "${wm.connectionInfo?.linkSpeed ?: 0} Mbps"
        }
    },
    WIFI_FREQUENCY("wifi_freq", "WiFi Frequency", "Network", "MHz", "ic_wifi") {
        override fun fetch(ctx: Context): String {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wm.isWifiEnabled) return "Off"
            @Suppress("DEPRECATION")
            val freq = wm.connectionInfo?.frequency ?: 0
            val band = if (freq > 4900) "5 GHz" else "2.4 GHz"
            return "$freq MHz ($band)"
        }
    },

    // ── Bluetooth ────────────────────────────────────────────────────────────
    BLUETOOTH_STATUS("bt_status", "Bluetooth", "Network", "", "ic_bluetooth") {
        override fun fetch(ctx: Context): String {
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
            val enabled = bm.adapter?.isEnabled == true
            val name = try { bm.adapter?.name ?: "" } catch (_: SecurityException) { "" }
            return if (enabled) "On ($name)" else "Off"
        }
    },

    // ── Cellular ─────────────────────────────────────────────────────────────
    CELLULAR_SIGNAL("cell_signal", "Cellular Signal", "Network", "dBm", "ic_cellular") {
        override fun fetch(ctx: Context): String = try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val dbm = tm.signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm ?: 0
            val level = tm.signalStrength?.level ?: 0
            "$dbm dBm ($level/4)"
        } catch (_: Exception) { "N/A" }
    },
    NETWORK_TYPE("net_type", "Network Type", "Network", "", "ic_cellular") {
        override fun fetch(ctx: Context): String = try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                else -> "Other"
            }
        } catch (_: Exception) { "N/A" }
    },

    // ── NFC ──────────────────────────────────────────────────────────────────
    NFC_STATUS("nfc_status", "NFC", "Network", "", "ic_nfc") {
        override fun fetch(ctx: Context): String {
            val nfc = android.nfc.NfcAdapter.getDefaultAdapter(ctx)
            return when {
                nfc == null -> "Not available"
                nfc.isEnabled -> "Enabled"
                else -> "Disabled"
            }
        }
    },

    // ── Sensors ──────────────────────────────────────────────────────────────
    ACCELEROMETER("accel", "Accelerometer", "Sensors", "m/s²", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_ACCELEROMETER) { v ->
            val mag = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            "${"%.2f".format(mag)} m/s²"
        }
    },
    GYROSCOPE("gyro", "Gyroscope", "Sensors", "rad/s", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_GYROSCOPE) { v ->
            val mag = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            "${"%.3f".format(mag)} rad/s"
        }
    },
    MAGNETOMETER("magneto", "Compass", "Sensors", "µT", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_MAGNETIC_FIELD) { v ->
            val mag = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            "${"%.1f".format(mag)} µT"
        }
    },
    LIGHT("light", "Light", "Sensors", "lux", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_LIGHT) { v ->
            "${"%.0f".format(v[0])} lux"
        }
    },
    PROXIMITY("proximity", "Proximity", "Sensors", "cm", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_PROXIMITY) { v ->
            val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val max = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.maximumRange ?: 5f
            if (v[0] < max) "NEAR (${"%.1f".format(v[0])} cm)" else "FAR"
        }
    },
    BAROMETER("barometer", "Barometer", "Sensors", "hPa", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_PRESSURE) { v ->
            "${"%.1f".format(v[0])} hPa"
        }
    },
    AMBIENT_TEMP("ambient_temp", "Temperature", "Sensors", "°C", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_AMBIENT_TEMPERATURE) { v ->
            "${"%.1f".format(v[0])}°C"
        }
    },
    HUMIDITY("humidity", "Humidity", "Sensors", "%", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_RELATIVE_HUMIDITY) { v ->
            "${"%.0f".format(v[0])}%"
        }
    },
    STEP_COUNTER("steps", "Step Counter", "Sensors", "steps", "ic_sensors") {
        override fun fetch(ctx: Context): String = readSensor(ctx, Sensor.TYPE_STEP_COUNTER) { v ->
            "${v[0].toLong()} steps"
        }
    },

    // ── Device ───────────────────────────────────────────────────────────────
    BRIGHTNESS("brightness", "Brightness", "Device", "%", "ic_brightness") {
        override fun fetch(ctx: Context): String = try {
            val cur = android.provider.Settings.System.getInt(
                ctx.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS, 128
            )
            "${(cur * 100) / 255}%"
        } catch (_: Exception) { "N/A" }
    };

    abstract fun fetch(ctx: Context): String

    companion object {
        fun fromKey(key: String): WidgetMetric? = entries.find { it.key == key }

        /** Groups metrics by category for the config picker. */
        fun grouped(): Map<String, List<WidgetMetric>> = entries.groupBy { it.category }
    }
}

/**
 * Reads a one-shot sensor value with a 500ms timeout.
 */
private fun readSensor(
    ctx: Context,
    type: Int,
    format: (FloatArray) -> String,
): String {
    val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = sm.getDefaultSensor(type) ?: return "N/A"
    val latch = CountDownLatch(1)
    var result = "N/A"
    val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            result = format(e.values)
            latch.countDown()
        }
        override fun onAccuracyChanged(s: Sensor, a: Int) {}
    }
    sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    latch.await(500, TimeUnit.MILLISECONDS)
    sm.unregisterListener(listener)
    return result
}
