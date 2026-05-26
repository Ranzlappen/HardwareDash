package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Fixed-size design token for [GadgetCircleControl]. Per the no-raw-dp
 * rule, the circle's diameter is a deliberate touch-target size (matches
 * the FAB family), so it lives here with a rationale rather than inline.
 */
private object GadgetCircleControlDefaults {
    val Diameter: Dp = 56.dp
}

/**
 * Circular, captioned control: a round icon surface over a short label.
 *
 * The reusable building block for "a row of identical round controls"
 * layouts — torch's toggle / hold / strobe / morse row is the reference
 * consumer, and future actuator modules (vibration, audio) should reuse
 * this instead of re-rolling a circular button.
 *
 * Pass [onClick] for tap-to-toggle **or** [onHold] for press-and-hold
 * (invoked `true` on press and `false` on release **or** cancel via
 * try/finally, so it can't get stuck "on"). [active] tints it with the
 * primary container to signal the on-state. [hero] marks the primary
 * action with a filled `primary` surface so it stands out while keeping
 * the identical circle-over-caption shape that gives the row its
 * consistency.
 *
 * [contentDescription] is required (icon-only surface). Honours
 * `defaultMinSize` at the 56 dp accessibility touch target.
 */
@Composable
fun GadgetCircleControl(
    icon: ImageVector,
    contentDescription: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    hero: Boolean = false,
    onClick: (() -> Unit)? = null,
    onHold: ((Boolean) -> Unit)? = null,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        hero && active -> MaterialTheme.colorScheme.primary
        hero -> MaterialTheme.colorScheme.primaryContainer
        active -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        hero && active -> MaterialTheme.colorScheme.onPrimary
        hero -> MaterialTheme.colorScheme.onPrimaryContainer
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val interaction = when {
        onHold != null -> Modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    onHold?.invoke(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHold?.invoke(false)
                    }
                },
            )
        }
        onClick != null -> Modifier.clickable(enabled = enabled, role = Role.Button) { onClick?.invoke() }
        else -> Modifier
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(GadgetCircleControlDefaults.Diameter, GadgetCircleControlDefaults.Diameter)
                .size(GadgetCircleControlDefaults.Diameter)
                .clip(CircleShape)
                .background(container)
                .then(interaction)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentTint)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.outline
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun GadgetCircleControlPreview() = GadgetThemedPreview {
    val spacing = LocalGadgetTheme.current.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
        GadgetCircleControl(
            icon = Icons.Outlined.FlashlightOn,
            contentDescription = "Torch",
            caption = "Torch",
            enabled = true,
            active = true,
            hero = true,
            onClick = {},
        )
        GadgetCircleControl(
            icon = Icons.Outlined.TouchApp,
            contentDescription = "Hold for light",
            caption = "Hold",
            enabled = true,
            onHold = {},
        )
        GadgetCircleControl(
            icon = Icons.Outlined.FlashlightOn,
            contentDescription = "Unavailable",
            caption = "No flash",
            enabled = false,
            onClick = {},
        )
    }
}
