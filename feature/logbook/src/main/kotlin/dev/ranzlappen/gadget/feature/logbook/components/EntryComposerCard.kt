package dev.ranzlappen.gadget.feature.logbook.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ranzlappen.gadget.core.data.logbook.LogbookTagColor
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.logbook.EntryComposerState
import dev.ranzlappen.gadget.feature.logbook.LogbookTagSwatch
import dev.ranzlappen.gadget.feature.logbook.R
import dev.ranzlappen.gadget.feature.logbook.label

/**
 * "New note" composer — free-text field, a tag-color swatch row, and a
 * submit button. Mirrors the "session notes" half of the legacy Logbook
 * tool's brief, minus the auto-logged/custom distinction and the
 * `tags: List<String>` free-tagging — one [LogbookTagColor] per note is
 * the scoped-down replacement.
 */
@Composable
fun EntryComposerCard(
    state: EntryComposerState,
    onTextChange: (String) -> Unit,
    onTagChange: (LogbookTagColor) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.logbook_composer_title),
        icon = Icons.Outlined.EditNote,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            GadgetTextField(
                value = state.text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.logbook_composer_placeholder),
                singleLine = false,
                minLines = 2,
                maxLines = 6,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                LogbookTagColor.entries.forEach { tag ->
                    LogbookTagSwatch(
                        tag = tag,
                        selected = state.tag == tag,
                        onClick = { onTagChange(tag) },
                        contentDescription = tag.label(),
                    )
                }
            }
            GadgetPrimaryButton(
                onClick = onSubmit,
                text = stringResource(R.string.logbook_composer_submit),
                enabled = state.text.isNotBlank(),
                trailingIcon = Icons.Outlined.Send,
            )
        }
    }
}
