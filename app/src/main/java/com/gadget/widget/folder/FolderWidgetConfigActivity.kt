package com.gadget.widget.folder

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.data.db.apps.FolderWidgetConfig
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.ui.apps.AppsViewModel
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.theme.ThemePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * APPWIDGET_CONFIGURE Activity. Shown by the launcher right after the user
 * drops a folder widget on the home screen. Lets them pick which existing
 * folder the widget should display, then writes the per-appWidgetId config
 * row and finishes with RESULT_OK so the launcher commits the placement.
 *
 * Cancellation (back-press, swipe-away) returns RESULT_CANCELED so the
 * launcher discards the half-placed widget — important to avoid orphan
 * widgets pointing at no folder.
 */
@AndroidEntryPoint
class FolderWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var dao: AppsDao

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default to canceled so back-press is treated as "abort placement".
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        LocalizationManager.init(this)
        AccessibilityPreferencesManager.init(this)
        ThemePreferencesManager.init(this)

        setContent {
            GadgetTheme {
                FolderPickerScreen(onPick = ::onFolderPicked)
            }
        }
    }

    private fun onFolderPicked(folder: Folder) {
        lifecycleScope.launch {
            val manager = AppWidgetManager.getInstance(this@FolderWidgetConfigActivity)
            // Determine which size variant the user dropped by looking up the
            // bound provider's class name.
            val providerClassName = manager.getAppWidgetInfo(appWidgetId)?.provider?.className
            val sizeVariant = when (providerClassName) {
                FolderWidget1x1Provider::class.java.name -> FolderWidgetRenderer.SIZE_1X1
                else -> FolderWidgetRenderer.SIZE_2X2
            }
            dao.upsertWidgetConfig(
                FolderWidgetConfig(
                    appWidgetId = appWidgetId,
                    folderId = folder.id,
                    sizeVariant = sizeVariant,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            // Render immediately so the widget paints before the next periodic
            // update tick from the launcher.
            FolderWidgetRenderer.update(
                context = this@FolderWidgetConfigActivity,
                appWidgetManager = manager,
                appWidgetId = appWidgetId,
                dao = dao,
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
    val apps = S.apps

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = apps.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = apps.appsInFolder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = apps.noFolders, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(folders, key = { it.id }) { folder ->
                        FolderPickerRow(folder = folder, onClick = { onPick(folder) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderPickerRow(folder: Folder, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(folder.baseColorArgb).copy(alpha = 0.18f),
        ),
    ) {
        Text(
            text = folder.name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
