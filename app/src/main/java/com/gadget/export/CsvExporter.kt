package com.gadget.export

import com.gadget.data.repository.MetricRepository
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports metric readings to CSV format.
 */
@Singleton
class CsvExporter @Inject constructor(
    private val metricRepository: MetricRepository,
) {
    /**
     * Writes metric readings to CSV with headers: timestamp, metric_key, raw_value, formatted_value.
     *
     * @param metricKeys The metric keys to export
     * @param startTime Start of the time range (epoch millis, inclusive)
     * @param endTime End of the time range (epoch millis, inclusive)
     * @param outputStream The output stream to write CSV data to
     */
    suspend fun exportMetrics(
        metricKeys: List<String>,
        startTime: Long,
        endTime: Long,
        outputStream: OutputStream,
    ) {
        val sb = StringBuilder()
        sb.appendLine("timestamp,metric_key,raw_value,formatted_value")

        for (key in metricKeys) {
            val readings = metricRepository
                .getReadingsInRange(key, startTime, endTime)
                .first()

            for (reading in readings) {
                sb.append(reading.timestamp)
                sb.append(',')
                sb.append(escapeCsv(reading.metricKey))
                sb.append(',')
                sb.append(reading.rawValue)
                sb.append(',')
                sb.append(escapeCsv(reading.formattedValue))
                sb.appendLine()
            }
        }

        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
