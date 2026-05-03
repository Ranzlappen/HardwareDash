package com.gadget.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.gadget.ui.theme.DashIcon
import com.gadget.ui.theme.DashShapes
import com.gadget.ui.theme.LocalAccessibilityPreferences

/**
 * Visual style of a [GadgetButton].
 *
 * - [Primary]   filled high-emphasis action (Button)
 * - [Secondary] outlined medium-emphasis action (OutlinedButton)
 * - [Tonal]     filled-tonal low-emphasis action (FilledTonalButton)
 */
enum class GadgetButtonStyle { Primary, Secondary, Tonal }

/**
 * Standardized application button. Wraps Material3 Button / OutlinedButton /
 * FilledTonalButton with consistent sizing, shape, content padding, and a
 * light haptic tick on press (gated by reduced-motion).
 *
 * Use [ResponsiveButtonText] inside the label slot when a long localized
 * label might overflow.
 *
 * Public API; uses only public material3 + theme types so it is safe to
 * call from any module file (see CLAUDE.md visibility rules).
 */
@Composable
fun GadgetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GadgetButtonStyle = GadgetButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    label: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = LocalAccessibilityPreferences.current.reducedMotion

    val onClickWithHaptic: () -> Unit = {
        if (!reducedMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }

    val sizingModifier = modifier.heightIn(min = 44.dp)

    when (style) {
        GadgetButtonStyle.Primary -> Button(
            onClick = onClickWithHaptic,
            modifier = sizingModifier,
            enabled = enabled,
            shape = DashShapes.pill,
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon, label)
        }
        GadgetButtonStyle.Secondary -> OutlinedButton(
            onClick = onClickWithHaptic,
            modifier = sizingModifier,
            enabled = enabled,
            shape = DashShapes.pill,
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon, label)
        }
        GadgetButtonStyle.Tonal -> FilledTonalButton(
            onClick = onClickWithHaptic,
            modifier = sizingModifier,
            enabled = enabled,
            shape = DashShapes.pill,
            contentPadding = contentPadding,
        ) {
            ButtonContent(leadingIcon, label)
        }
    }
}

@Composable
private fun ButtonContent(
    leadingIcon: ImageVector?,
    label: @Composable () -> Unit,
) {
    if (leadingIcon != null) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(DashIcon.button),
        )
        Spacer(Modifier.width(8.dp))
    }
    label()
}
