package dev.ranzlappen.gadget.feature.apps.widget

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.widgetkit.provider.ContentWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches the App-Organizer data flows and repaints every placed folder widget
 * whenever folder metadata, membership, or the app catalog changes — the
 * content-source → repaint side of the kit's content archetype, driven through
 * [ContentWidgetUpdater].
 *
 * Eagerly instantiated for the process lifetime (the app's startup path injects
 * it, the same trick as `AppRepository`).
 */
@Singleton
class FolderWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            combine(
                dao.observeFolders(),
                dao.observeAppRecords(),
                dao.observeRules(),
            ) { folders, records, rules -> Triple(folders, records.size, rules.size) }
                .distinctUntilChanged()
                // Skip the initial replay — the launcher already paints placed
                // widgets via onUpdate; only repaint on subsequent changes.
                .drop(1)
                .collect {
                    ContentWidgetUpdater.requestUpdate(context, FolderWidgetProvider.PROVIDER_CLASS)
                }
        }
    }
}
