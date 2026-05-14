package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.designsystem.tokens.GadgetSpacing

/**
 * Modal bottom sheet with Gadget styling.
 *
 * Wraps Material 3 [ModalBottomSheet] with the design-system's large
 * shape token and theme-derived surface colours. Content slot is a
 * [ColumnScope] so callers can use `Modifier.fillMaxWidth()`,
 * `Modifier.weight(...)`, and `Modifier.align(...)` directly without
 * an extra Column.
 *
 * Pass a [title] for the conventional header row; pass `null` to
 * render just the content (drag handle still appears).
 *
 * Caller owns the show/hide via [sheetState]; this composable should
 * be conditionally placed in the composition. Typical usage:
 *
 * ```kotlin
 * if (showSheet) {
 *     GadgetBottomSheet(onDismissRequest = { showSheet = false }) { … }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GadgetBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = GadgetSpacing.Large,
        vertical = GadgetSpacing.Medium,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            content()
        }
    }
}

/**
 * Alert dialog with Gadget styling.
 *
 * Wraps Material 3 [AlertDialog]. The two button slots accept
 * arbitrary composables — pair with [GadgetPrimaryButton] /
 * [GadgetTertiaryButton] for the conventional "Confirm + Cancel"
 * shape. Pass `null` for [dismissButton] to render a single-button
 * dialog (e.g. an info acknowledgement).
 *
 * Long body copy in [text] wraps freely (up to [bodyMaxLines]).
 * Long titles truncate to 2 lines with [TextOverflow.Ellipsis].
 */
@Composable
fun GadgetDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    icon: ImageVector? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    bodyMaxLines: Int = DialogBodyDefaultMaxLines,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(DialogIconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
        title = if (title != null) {
            {
                // A11y: announce as a heading so screen readers know
                // this is the dialog's primary subject (TalkBack
                // emits a "Heading" earcon before reading the text).
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        } else null,
        text = if (text != null) {
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = bodyMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else null,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ─── Internals ──────────────────────────────────────────────────────

/** Default maxLines for [GadgetDialog]'s body text. */
private const val DialogBodyDefaultMaxLines: Int = 10

/** Fixed-size design token: hero icon at the top of a dialog. */
private val DialogIconSize: Dp = 24.dp
