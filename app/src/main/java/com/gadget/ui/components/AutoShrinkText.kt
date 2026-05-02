package com.gadget.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * Text that progressively shrinks its font size to fit a single line in the
 * available width, then ellipsizes if even [minFontSize] doesn't fit.
 *
 * Designed for buttons whose localized labels (DE/ES/FR) can run longer than
 * the EN reference and would otherwise wrap awkwardly. Shrinks in 0.5.sp
 * increments. Uses [Text] with [Text.onTextLayout] to detect overflow.
 *
 * Public — kept material-free aside from [LocalTextStyle] so any non-internal
 * caller can use it without touching internal types.
 */
@Composable
fun AutoShrinkText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    minFontSize: TextUnit = 11.sp,
    maxLines: Int = 1,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val baseFontSize = if (style.fontSize.isUnspecified) 14.sp else style.fontSize
    var currentFontSize by remember(text, baseFontSize) { mutableStateOf(baseFontSize) }
    var readyToDraw by remember(text, baseFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(fontSize = currentFontSize),
        maxLines = maxLines,
        softWrap = maxLines > 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        onTextLayout = { layoutResult ->
            if (!readyToDraw) {
                val overflows = layoutResult.didOverflowWidth || layoutResult.lineCount > maxLines
                if (overflows && currentFontSize > minFontSize) {
                    val nextSize = (currentFontSize.value - 0.5f).coerceAtLeast(minFontSize.value)
                    currentFontSize = nextSize.sp
                } else {
                    readyToDraw = true
                }
            }
        },
    )
}
