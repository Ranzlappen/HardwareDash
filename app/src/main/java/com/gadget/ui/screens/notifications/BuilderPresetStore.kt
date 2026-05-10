package com.gadget.ui.screens.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject

// Save / load named NotifSpec presets.  Backed by the same
// "schedule_actions" SharedPreferences file the screen already uses,
// under a dedicated "presets_v1" key so the existing schedule-list
// JSON is never touched.
object BuilderPresetStore {

    private const val PREFS_NAME = "schedule_actions"
    private const val KEY = "presets_v1"

    data class Preset(val name: String, val spec: NotifSpec)

    fun load(context: Context): List<Preset> = read(prefs(context))

    fun save(context: Context, name: String, spec: NotifSpec) {
        if (name.isBlank()) return
        val p = prefs(context)
        val existing = read(p).filterNot { it.name == name }
        val updated = existing + Preset(name.trim(), spec)
        write(p, updated)
    }

    fun delete(context: Context, name: String) {
        val p = prefs(context)
        val remaining = read(p).filterNot { it.name == name }
        write(p, remaining)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun read(prefs: SharedPreferences): List<Preset> {
        val json = prefs.getString(KEY, "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length())
            .mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Preset(name, obj.toSpec())
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun write(prefs: SharedPreferences, presets: List<Preset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun Preset.toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("title", spec.title)
        put("body", spec.body)
        put("subtext", spec.subtext)
        put("priority", spec.priority)
        put("visibility", spec.visibility)
        spec.category?.let { put("category", it) }
        spec.accentColor?.let { put("accentColor", it) }
        put("actions", JSONArray().apply { spec.actions.forEach { put(it.name) } })
        put("progressMode", spec.progressMode.name)
        put("progressValue", spec.progressValue)
        put("style", spec.style.name)
        put("ongoing", spec.ongoing)
        put("autoCancel", spec.autoCancel)
        put("sound", spec.sound)
        put("vibrate", spec.vibrate)
        put("timeoutSec", spec.timeoutSec)
        put("badge", spec.badge)
        put("quickReplyHint", spec.quickReplyHint)
    }

    private fun JSONObject.toSpec(): NotifSpec {
        val actionsArr = optJSONArray("actions") ?: JSONArray()
        val actions = (0 until actionsArr.length()).mapNotNull { i ->
            val name = actionsArr.optString(i)
            runCatching { NotifActionEntry.valueOf(name) }.getOrNull()
        }
        return NotifSpec(
            title = optString("title"),
            body = optString("body"),
            subtext = optString("subtext"),
            priority = optInt("priority", NotificationCompat.PRIORITY_DEFAULT),
            visibility = optInt("visibility", NotificationCompat.VISIBILITY_PUBLIC),
            category = if (has("category")) optString("category") else null,
            accentColor = if (has("accentColor")) optInt("accentColor") else null,
            actions = actions,
            progressMode = runCatching { ProgressMode.valueOf(optString("progressMode", "OFF")) }.getOrDefault(ProgressMode.OFF),
            progressValue = optInt("progressValue", 0),
            style = runCatching { NotifStyle.valueOf(optString("style", "NORMAL")) }.getOrDefault(NotifStyle.NORMAL),
            ongoing = optBoolean("ongoing", false),
            autoCancel = optBoolean("autoCancel", true),
            sound = optBoolean("sound", true),
            vibrate = optBoolean("vibrate", true),
            timeoutSec = optInt("timeoutSec", 0),
            badge = optInt("badge", 0),
            quickReplyHint = optString("quickReplyHint"),
        )
    }
}
