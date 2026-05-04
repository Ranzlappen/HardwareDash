package com.gadget.widget.folder

import android.content.Context
import com.gadget.data.db.apps.AppsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches the App-Organizer Room flows and re-renders every placed folder
 * widget whenever folder data, membership, or the app catalog changes.
 *
 * Wired up by `MainActivity` injecting it (forces eager Hilt instantiation,
 * same trick as `AppRepository`). Survives for the process lifetime.
 */
@Singleton
class FolderWidgetController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppsDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            // Combine the three flows whose changes affect rendered widget
            // contents. distinctUntilChanged drops dupes (especially handy on
            // observeAppRecords which can emit on every refresh tick).
            combine(
                dao.observeFolders(),
                dao.observeAppRecords(),
                dao.observeRules(),
            ) { folders, records, rules -> Triple(folders, records.size, rules.size) }
                .distinctUntilChanged()
                .collect {
                    FolderWidgetProvider.updateAll(context)
                }
        }
    }
}
