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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.data.apps.AppRecord
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
    val viewModel: FolderPopupViewModel = hiltViewModel()
    LaunchedEffect(folderId) { viewModel.load(folderId) }

    val folder by viewModel.folder.collectAsState()
    val apps by viewModel.appsInFolder.collectAsState()

    val accent = folder?.let { Color(it.baseColorArgb) } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FolderHeader(name = folder?.name.orEmpty(), accent = accent)
            Spacer(Modifier.height(12.dp))
            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_no_apps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.size(12.dp))
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(record = record, sizeDp = 48.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = record.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
