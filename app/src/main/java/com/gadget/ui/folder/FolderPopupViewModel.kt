package com.gadget.ui.folder

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gadget.apps.rules.FolderRule
import com.gadget.apps.rules.FolderRuleSet
import com.gadget.apps.rules.RuleCodec
import com.gadget.apps.rules.RuleEngine
import com.gadget.apps.rules.UsageEntry
import com.gadget.data.db.apps.AppRecord
import com.gadget.data.db.apps.AppsDao
import com.gadget.data.db.apps.Folder
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
 * Backs [FolderPopupContent]. Resolves the folder + its full materialized app
 * list — manual entries unioned with every rule match — by feeding the stored
 * [FolderRuleSet] into [RuleEngine].
 *
 * Usage stats for `UnusedSinceDays` are queried lazily and only when at least
 * one such rule is active, so granting `PACKAGE_USAGE_STATS` is purely opt-in.
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
                    val ruleSet = rules.firstOrNull { it.folderId == id }
                        ?.let { RuleCodec.decode(it.ruleJson) }
                        ?: FolderRuleSet()
                    val sortedManualKeys = members
                        .sortedBy { it.sortOrder }
                        .map { it.appKey }
                        .toCollection(LinkedHashSet())
                    val needsUsage = ruleSet.rules.any { it is FolderRule.UnusedSinceDays }
                    val usage = if (needsUsage) loadUsage() else null
                    RuleEngine.materialize(
                        ruleSet = ruleSet,
                        manualMembership = sortedManualKeys,
                        allApps = allRecords,
                        usage = usage,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(folderId: Long) {
        folderIdFlow.value = folderId
    }

    private suspend fun loadUsage(): List<UsageEntry>? = withContext(Dispatchers.IO) {
        val mgr = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext null
        val end = System.currentTimeMillis()
        val start = end - 90L * 24 * 60 * 60 * 1000
        val stats = runCatching {
            mgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        }.getOrNull().orEmpty()
        if (stats.isEmpty()) null else stats.map { UsageEntry(it.packageName, it.lastTimeUsed) }
    }
}
