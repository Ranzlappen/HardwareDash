package com.gadget.ui.folder

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.apps.rules.FolderRule
import com.gadget.apps.rules.RuleCodec
import com.gadget.apps.rules.RuleEngine
import com.gadget.apps.rules.UsageEntry
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
import com.gadget.data.db.apps.FolderApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Backs [FolderPopupContent]. Resolves the folder + its current member apps by
 * either consulting `apps_folder_app` rows (Manual rule) or evaluating the
 * stored [FolderRule] via [RuleEngine] (everything else).
 *
 * Usage stats for `UnusedSinceDays` are queried lazily and only when needed,
 * so granting `PACKAGE_USAGE_STATS` is purely opt-in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderPopupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: AppsDao,
) : ViewModel() {

    private val folderIdFlow = MutableStateFlow(-1L)

    val folder: StateFlow<Folder?> = folderIdFlow
        .flatMapLatest { id ->
            if (id < 0L) flowOf(null)
            else dao.observeFolders().map { folders -> folders.firstOrNull { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appsInFolder: StateFlow<List<AppRecord>> = folderIdFlow
        .flatMapLatest { id ->
            if (id < 0L) {
                flowOf(emptyList())
            } else {
                combine(
                    dao.observeMembership(id),
                    dao.observeAppRecords(),
                    dao.observeRules(),
                ) { members, allRecords, rules ->
                    val rule = rules.firstOrNull { it.folderId == id }
                        ?.let { RuleCodec.decode(it.ruleJson) }
                        ?: FolderRule.Manual
                    materialize(rule, members.sortedBy { it.sortOrder }, allRecords)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(folderId: Long) {
        folderIdFlow.value = folderId
    }

    private suspend fun materialize(
        rule: FolderRule,
        sortedMembership: List<FolderApp>,
        allRecords: List<AppRecord>,
    ): List<AppRecord> {
        if (rule is FolderRule.Manual) {
            val orderByKey = sortedMembership
                .withIndex()
                .associate { (idx, m) -> m.appKey to idx }
            return allRecords
                .filter { it.appKey in orderByKey }
                .sortedBy { orderByKey[it.appKey] ?: Int.MAX_VALUE }
        }
        // For smart rules, optionally fetch usage stats off the main thread.
        val usage = if (rule is FolderRule.UnusedSinceDays) loadUsage() else null
        return RuleEngine.materialize(
            rule = rule,
            manualMembership = emptySet(),
            allApps = allRecords,
            usage = usage,
        )
    }

    private suspend fun loadUsage(): List<UsageEntry>? = withContext(Dispatchers.IO) {
        val mgr = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext null
        val end = System.currentTimeMillis()
        // 90 days of usage covers UnusedSinceDays(any reasonable window).
        val start = end - 90L * 24 * 60 * 60 * 1000
        val stats = runCatching {
            mgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        }.getOrNull().orEmpty()
        // queryUsageStats silently returns empty when PACKAGE_USAGE_STATS isn't
        // granted; treat that as null so RuleEngine returns empty rather than
        // claiming "everything is unused".
        if (stats.isEmpty()) null else stats.map { UsageEntry(it.packageName, it.lastTimeUsed) }
    }
}
