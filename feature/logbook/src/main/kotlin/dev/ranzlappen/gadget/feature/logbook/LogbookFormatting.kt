package dev.ranzlappen.gadget.feature.logbook

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** `java.time` formatting helpers for epoch-millis fields on the Logbook
 *  entities — matches `:core:automation`'s existing `java.time` usage
 *  (`AutomationScheduler`, `NextScheduleCalculator`) rather than reaching
 *  for a new date library. */
object LogbookFormatting {

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    fun formatDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

    fun formatDateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateTimeFormatter)

    fun isOverdue(dueAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean =
        dueAtMillis != null && dueAtMillis < nowMillis
}
