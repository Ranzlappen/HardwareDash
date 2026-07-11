package dev.ranzlappen.gadget.feature.apps.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.icons.AppIcon

/**
 * Compose content rendered inside the translucent [FolderPopupActivity]. Shows
 * the folder's name on a header tinted with `baseColorArgb` plus a 4-column
 * grid of app cells. Tapping a cell delegates to the activity (which calls
 * `AppLauncher` and finishes itself).
 */
@Composable
fun FolderPopupContent(
    folderId: Long,
    onAppClick: (AppRecord) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val viewModel: FolderPopupViewModel = hiltViewModel()
    LaunchedEffect(folderId) { viewModel.load(folderId) }

    val folder by viewModel.folder.collectAsState()
    val apps by viewModel.appsInFolder.collectAsState()

    val accent = folder?.let { Color(it.baseColorArgb) } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FolderPopupContentDefaults.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            FolderHeader(name = folder?.name.orEmpty(), accent = accent)
            Spacer(Modifier.height(spacing.small))
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.large),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(vertical = spacing.tiny),
                    horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    items(apps, key = { it.appKey }) { record ->
                        AppCell(record = record, onClick = { onAppClick(record) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderHeader(name: String, accent: Color) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(FolderPopupContentDefaults.HeaderDotSize)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.size(spacing.small))
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppCell(
    record: AppRecord,
    onClick: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FolderPopupContentDefaults.CellCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = FolderPopupContentDefaults.CellVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(record = record, sizeDp = FolderPopupContentDefaults.CellIconSize)
        Spacer(Modifier.height(spacing.micro))
        Text(
            text = record.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = spacing.micro),
        )
    }
}

private object FolderPopupContentDefaults {
    val CardCornerRadius: Dp = 28.dp
    val HeaderDotSize: Dp = 28.dp
    val CellCornerRadius: Dp = 12.dp
    val CellVerticalPadding: Dp = 6.dp
    val CellIconSize: Dp = 48.dp
}
