package com.gadget.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DrawnPoint(val timeNorm: Float, val intensity: Float)

data class SavedDrawnPattern(
    val name: String,
    val points: List<DrawnPoint>,
    val loop: Boolean,
)

object DrawnPatternUtils {
    private const val PREFS_NAME = "vibration_patterns"
    private const val KEY_SAVED_DRAWN = "saved_drawn_patterns"
    private const val KEY_ACTIVE_DRAWN = "active_drawn_pattern"
    private const val MAX_SAVED = 20

    // ─── Serialization ───────────────────────────────────────────────────────────

    private fun serializePoints(points: List<DrawnPoint>): JSONArray {
        val arr = JSONArray()
        points.forEach { p ->
            arr.put(JSONObject().apply {
                put("t", p.timeNorm.toDouble())
                put("i", p.intensity.toDouble())
            })
        }
        return arr
    }

    private fun deserializePoints(arr: JSONArray): List<DrawnPoint> =
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            DrawnPoint(obj.getDouble("t").toFloat(), obj.getDouble("i").toFloat())
        }

    // ─── Save/Load named drawn patterns ──────────────────────────────────────────

    fun saveDrawnPatterns(context: Context, patterns: List<SavedDrawnPattern>) {
        val arr = JSONArray()
        patterns.take(MAX_SAVED).forEach { p ->
            arr.put(JSONObject().apply {
                put("name", p.name)
                put("loop", p.loop)
                put("points", serializePoints(p.points))
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SAVED_DRAWN, arr.toString()).apply()
    }

    fun loadDrawnPatterns(context: Context): List<SavedDrawnPattern> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED_DRAWN, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SavedDrawnPattern(
                    name = obj.getString("name"),
                    loop = obj.getBoolean("loop"),
                    points = deserializePoints(obj.getJSONArray("points")),
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    // ─── Active drawn pattern for widget ─────────────────────────────────────────

    fun setActiveDrawnPattern(context: Context, points: List<DrawnPoint>, loop: Boolean) {
        val obj = JSONObject().apply {
            put("loop", loop)
            put("points", serializePoints(points))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE_DRAWN, obj.toString()).apply()
    }

    fun getActiveDrawnPattern(context: Context): Pair<List<DrawnPoint>, Boolean>? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_DRAWN, null) ?: return null
        return try {
            val obj = JSONObject(json)
            val points = deserializePoints(obj.getJSONArray("points"))
            if (points.isEmpty()) null else Pair(points, obj.getBoolean("loop"))
        } catch (_: Exception) { null }
    }

    fun clearActiveDrawnPattern(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE_DRAWN).apply()
    }

    // ─── Interpolation & waveform conversion ─────────────────────────────────────

    fun interpolateIntensity(points: List<DrawnPoint>, tNorm: Float): Float {
        if (points.isEmpty()) return 0f
        if (tNorm <= points.first().timeNorm) return points.first().intensity
        if (tNorm >= points.last().timeNorm) return points.last().intensity

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (tNorm in p1.timeNorm..p2.timeNorm) {
                val frac = if (p2.timeNorm > p1.timeNorm)
                    (tNorm - p1.timeNorm) / (p2.timeNorm - p1.timeNorm) else 0f
                return p1.intensity + frac * (p2.intensity - p1.intensity)
            }
        }
        return points.last().intensity
    }

    fun toWaveformArrays(
        points: List<DrawnPoint>,
        hasAmplitude: Boolean,
    ): Pair<LongArray, IntArray> {
        if (points.isEmpty()) return Pair(LongArray(0), IntArray(0))
        val sorted = points.sortedBy { it.timeNorm }
        val totalMs = 2000L
        val sampleInterval = 50L
        val numSamples = (totalMs / sampleInterval).toInt()

        val timings = LongArray(numSamples)
        val amplitudes = IntArray(numSamples)

        for (i in 0 until numSamples) {
            val tNorm = i.toFloat() / numSamples
            val intensity = interpolateIntensity(sorted, tNorm)
            timings[i] = sampleInterval
            amplitudes[i] = if (hasAmplitude) (intensity * 255).toInt().coerceIn(0, 255)
                            else if (intensity > 0.1f) 255 else 0
        }
        return Pair(timings, amplitudes)
    }
}
