package dev.ranzlappen.gadget.feature.apps.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.ui.AppsViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `APPWIDGET_CONFIGURE` activity shown by the launcher right after the user
 * drops a folder widget from the tray. Lets them pick which folder the widget
 * binds to, writes the per-`appWidgetId` [FolderWidgetConfig] to the kit
 * [WidgetConfigStore], repaints, and finishes `RESULT_OK`.
 *
 * Defaults to `RESULT_CANCELED` so a back-press / swipe-away discards the
 * half-placed widget (no orphan bound to no folder).
 */
@AndroidEntryPoint
class FolderWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var configStore: WidgetConfigStore<FolderWidgetConfig>

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            GadgetTheme {
                FolderPickerScreen(onPick = ::onFolderPicked)
            }
        }
    }

    private fun onFolderPicked(folder: Folder) {
        lifecycleScope.launch {
            configStore.save(
                appWidgetId,
                FolderWidgetConfig(folderId = folder.id, displayName = folder.name),
            )
            // Repaint placed instances (including this freshly-bound one).
            ContentWidgetUpdater.requestUpdate(
                this@FolderWidgetConfigActivity,
                FolderWidgetProvider.PROVIDER_CLASS,
            )
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@Composable
private fun FolderPickerScreen(onPick: (Folder) -> Unit) {
    val viewModel = hiltViewModel<AppsViewModel>()
    val folders by viewModel.folders.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.apps_widget_pick_folder),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (folders.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.apps_no_folders),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders, key = { it.id }) { folder ->
                        CompactCard(
                            title = folder.name,
                            onClick = { onPick(folder) },
                        )
                    }
                }
            }
        }
    }
}
