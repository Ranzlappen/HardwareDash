package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField

/**
 * Fully-custom ARGB colour picker — an HSV saturation/value square with a
 * draggable thumb, a hue slider, an alpha slider, a live preview swatch
 * and a `#AARRGGBB` hex field. State is hoisted: [argb] is a packed ARGB
 * [Long] (matching `WidgetAppearance.solidColor` /
 * `IconStyle.customTintArgb`), and [onArgbChange] fires on every edit.
 *
 * Conversions go through `android.graphics.Color` HSV helpers to avoid
 * Compose `Color.hsv` rounding surprises at the gamut edges.
 */
@Composable
fun GadgetColorPicker(
    argb: Long,
    onArgbChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val hsv = remember(argb) { argb.toHsv() }
    var hue by remember(argb) { mutableStateOf(hsv[0]) }
    var sat by remember(argb) { mutableStateOf(hsv[1]) }
    var value by remember(argb) { mutableStateOf(hsv[2]) }
    var alpha by remember(argb) { mutableStateOf(((argb ushr 24) and 0xFFL) / 255f) }

    fun emit() = onArgbChange(hsvToArgb(hue, sat, value, alpha))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        val hueColor = Color(hsvToArgb(hue, 1f, 1f, 1f).toInt())

        // Saturation / value square.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(spacing.small))
                .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        sat = (offset.x / size.width).coerceIn(0f, 1f)
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val p: Offset = change.position
                        sat = (p.x / size.width).coerceIn(0f, 1f)
                        value = (1f - p.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                },
        ) {}

        // Hue slider (0..360).
        Slider(
            value = hue,
            onValueChange = { hue = it; emit() },
            valueRange = 0f..360f,
        )
        // Alpha slider (0..1).
        Slider(
            value = alpha,
            onValueChange = { alpha = it; emit() },
            valueRange = 0f..1f,
        )

        // Preview + hex.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            ColorPreviewSwatch(argb = argb)
            GadgetTextField(
                value = argb.toHexString(),
                onValueChange = { text ->
                    parseHexArgb(text)?.let { onArgbChange(it) }
                },
                label = "#AARRGGBB",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColorPreviewSwatch(argb: Long) {
    Column(
        modifier = Modifier
            .size(ColorPickerDefaults.SwatchSize)
            .clip(RoundedCornerShape(ColorPickerDefaults.SwatchCorner))
            .background(Color(argb.toInt()))
            .border(
                width = ColorPickerDefaults.SwatchBorder,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(ColorPickerDefaults.SwatchCorner),
            ),
    ) {}
}

/** Fixed preview-swatch geometry — design-token constants per the repo's
 *  no-raw-dp rule. */
private object ColorPickerDefaults {
    val SwatchSize = 48.dp
    val SwatchCorner = 8.dp
    val SwatchBorder = 1.dp
}

private fun Long.toHsv(): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toInt(), out)
    return out
}

private fun hsvToArgb(hue: Float, sat: Float, value: Float, alpha: Float): Long {
    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
    val a = (alpha.coerceIn(0f, 1f) * 255f).toInt()
    return ((a.toLong() shl 24) or (rgb.toLong() and 0xFFFFFFL)) and 0xFFFFFFFFL
}

private fun Long.toHexString(): String = "#%08X".format(this and 0xFFFFFFFFL)

/** Parse `#AARRGGBB` or `#RRGGBB` (assumes opaque). Returns null on a
 *  malformed string so the field can reject partial typing gracefully. */
private fun parseHexArgb(text: String): Long? {
    val hex = text.trim().removePrefix("#")
    return when (hex.length) {
        6 -> hex.toLongOrNull(16)?.let { 0xFF000000L or it }
        8 -> hex.toLongOrNull(16)
        else -> null
    }
}
