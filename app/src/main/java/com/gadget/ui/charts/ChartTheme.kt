package com.gadget.ui.charts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.text.TextComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ChartCyan = Color(0xFF00BCD4)
val ChartGreen = Color(0xFF4CAF50)
val ChartAmber = Color(0xFFFFC107)
val ChartPurple = Color(0xFF9C27B0)
val ChartOrange = Color(0xFFFF5722)
val ChartBlue = Color(0xFF2196F3)

val chartLineColors = listOf(ChartCyan, ChartGreen, ChartAmber, ChartPurple, ChartOrange, ChartBlue)

@Composable
fun gadgetLineSpecs(axisCount: Int = 1) = (0 until axisCount).map { index ->
    lineSpec(
        lineColor = chartLineColors.getOrElse(index) { ChartCyan },
    )
}

class TimeAxisFormatter(private val timeRange: Long) : AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    private val format = when {
        timeRange <= 3_600_000L -> SimpleDateFormat("HH:mm", Locale.getDefault())
        timeRange <= 86_400_000L -> SimpleDateFormat("HH:mm", Locale.getDefault())
        else -> SimpleDateFormat("MM/dd", Locale.getDefault())
    }

    override fun formatValue(value: Float, chartValues: com.patrykandpatrick.vico.core.chart.values.ChartValues): CharSequence {
        return format.format(Date(value.toLong()))
    }
}
