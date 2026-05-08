package com.gadget.diagnostics

import android.content.Context
import android.os.Build
import com.gadget.root.core.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

internal const val CPUINFO_TAIL_CAP_BYTES = 8 * 1024

private const val LOGBOOK_DIR_NAME = "logbook"
private const val CPUINFO_FILENAME_PREFIX = "cpuinfo-"
private const val CPUINFO_FILENAME_EXTENSION = ".json"

/**
 * Read-only `dumpsys cpuinfo` snapshot, tail-capped to 8 KB.
 * Optionally persists a JSON copy to the Logbook directory.
 */
@Singleton
class CpuInfoDumpHelper @Inject constructor(
    private val shell: RootShell,
    @ApplicationContext private val context: Context,
) {
    suspend fun snapshot(): String? {
        val result = shell.exec(
            "dumpsys cpuinfo 2>/dev/null | tail -c $CPUINFO_TAIL_CAP_BYTES",
        )
        if (!result.isSuccess) return null
        return result.stdout.joinToString("\n").take(CPUINFO_TAIL_CAP_BYTES)
    }

    fun persistToLogbook(excerpt: String): File? {
        val dir = resolveLogbookDir() ?: return null
        if (!dir.exists() && !dir.mkdirs()) return null
        val payload = JSONObject().apply {
            put("timestamp", isoTimestamp())
            put("device", deviceJson())
            put("excerpt", excerpt)
        }
        val file = File(
            dir,
            "$CPUINFO_FILENAME_PREFIX${filenameTimestamp()}$CPUINFO_FILENAME_EXTENSION",
        )
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
