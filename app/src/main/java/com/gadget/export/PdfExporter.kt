package com.gadget.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.gadget.ui.logbook.LogbookEntry
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Exports logbook entries to PDF using Android's built-in PdfDocument API.
 */
class PdfExporter {

    /**
     * Renders logbook entries with timestamp, text, and tags to a PDF.
     *
     * @param entries The logbook entries to export
     * @param outputStream The output stream to write PDF data to
     */
    fun exportLogbook(entries: List<LogbookEntry>, outputStream: OutputStream) {
        val document = PdfDocument()

        val pageWidth = 595  // A4 width in points (72 dpi)
        val pageHeight = 842 // A4 height in points

        val marginLeft = 40f
        val marginTop = 50f
        val marginBottom = 50f
        val contentWidth = pageWidth - marginLeft * 2

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val timestampPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        val tagPaint = Paint().apply {
            color = Color.rgb(100, 100, 180)
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.5f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = marginTop

        // Draw title on first page
        canvas.drawText("Logbook Export", marginLeft, yPos, titlePaint)
        yPos += 30f

        canvas.drawText(
            "Generated: ${Instant.now().atZone(ZoneId.systemDefault()).format(DISPLAY_FORMATTER)}",
            marginLeft,
            yPos,
            timestampPaint,
        )
        yPos += 10f

        canvas.drawLine(marginLeft, yPos, marginLeft + contentWidth, yPos, linePaint)
        yPos += 15f

        val sortedEntries = entries.sortedByDescending { it.isoDate }

        for (entry in sortedEntries) {
            // Estimate needed height: timestamp + text + tags + spacing
            val entryHeight = estimateEntryHeight(entry, bodyPaint, contentWidth)

            // Check if we need a new page
            if (yPos + entryHeight > pageHeight - marginBottom) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = marginTop
            }

            // Draw timestamp
            val formattedDate = formatIsoDate(entry.isoDate)
            canvas.drawText(formattedDate, marginLeft, yPos, timestampPaint)
            yPos += 14f

            // Draw entry text (with simple word wrapping)
            val textLines = wrapText(entry.text.ifEmpty { "(empty entry)" }, bodyPaint, contentWidth)
            for (line in textLines) {
                canvas.drawText(line, marginLeft, yPos, bodyPaint)
                yPos += 15f
            }

            // Draw tags
            if (entry.tags.isNotEmpty()) {
                val tagsText = entry.tags.joinToString(", ") { "#$it" }
                canvas.drawText(tagsText, marginLeft, yPos, tagPaint)
                yPos += 13f
            }

            // Separator line
            yPos += 5f
            canvas.drawLine(marginLeft, yPos, marginLeft + contentWidth, yPos, linePaint)
            yPos += 10f
        }

        document.finishPage(page)

        document.writeTo(outputStream)
        document.close()
    }

    private fun estimateEntryHeight(
        entry: LogbookEntry,
        bodyPaint: Paint,
        contentWidth: Float,
    ): Float {
        var height = 14f // timestamp
        val textLines = wrapText(entry.text.ifEmpty { "(empty entry)" }, bodyPaint, contentWidth)
        height += textLines.size * 15f
        if (entry.tags.isNotEmpty()) height += 13f
        height += 15f // spacing + separator
        return height
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        val words = text.split(' ')
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word
            else "${currentLine} $word"

            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines.ifEmpty { listOf("") }
    }

    private fun formatIsoDate(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(DISPLAY_FORMATTER)
        } catch (_: Exception) {
            isoDate.ifEmpty { "No date" }
        }
    }

    companion object {
        private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
