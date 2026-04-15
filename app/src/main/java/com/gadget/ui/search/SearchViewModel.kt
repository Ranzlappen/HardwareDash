package com.gadget.ui.search

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.gadget.ui.link.LinkRule
import com.gadget.ui.link.loadRules
import com.gadget.ui.logbook.LogbookRepository
import com.gadget.ui.logbook.LogbookStore
import com.gadget.ui.navigation.Routes
import com.gadget.widget.WidgetMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val logbookRepository: LogbookRepository,
    application: Application,
) : AndroidViewModel(application) {

    val query = MutableStateFlow("")

    private val context: Context get() = getApplication()

    // Searchable setting names (hardcoded for search matching)
    private val settingLabels = listOf(
        "Language" to Routes.SETTINGS,
        "Widget Customizer" to Routes.SETTINGS,
        "Phone Ring Duration" to Routes.SETTINGS,
        "Notification Delay" to Routes.SETTINGS,
        "Bypass Do Not Disturb" to Routes.SETTINGS,
        "Metric Logging" to Routes.SETTINGS,
        "High Contrast" to Routes.SETTINGS,
        "Large Text" to Routes.SETTINGS,
        "Reduced Motion" to Routes.SETTINGS,
        "Backup & Restore" to Routes.SETTINGS,
    )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<SearchResult>> = query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                logbookRepository.storeFlow.map { store ->
                    buildResults(q.trim(), store)
                }
            }
        }
        .stateIn(
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private fun buildResults(q: String, store: LogbookStore): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val lower = q.lowercase()

        // 1. Search WidgetMetric entries
        WidgetMetric.entries.forEach { metric ->
            if (metric.displayName.lowercase().contains(lower) ||
                metric.key.lowercase().contains(lower) ||
                metric.category.lowercase().contains(lower) ||
                metric.unit.lowercase().contains(lower)
            ) {
                results.add(
                    SearchResult(
                        title = metric.displayName,
                        subtitle = "${metric.category} \u2022 ${metric.unit.ifBlank { "--" }}",
                        category = SearchCategory.METRIC,
                        route = Routes.MONITOR,
                    )
                )
            }
        }

        // 2. Search logbook entries
        store.entries.forEach { entry ->
            if (entry.text.lowercase().contains(lower) ||
                entry.tags.any { it.lowercase().contains(lower) }
            ) {
                results.add(
                    SearchResult(
                        title = entry.text.ifBlank { "Log entry" },
                        subtitle = entry.tags.joinToString(", ").ifBlank { entry.isoDate.take(10) },
                        category = SearchCategory.LOGBOOK,
                        route = Routes.LOGBOOK,
                    )
                )
            }
        }

        // 3. Search link rules
        try {
            val prefs = context.getSharedPreferences("link_rules", Context.MODE_PRIVATE)
            val rulesJson = prefs.getString("rules_json", "") ?: ""
            val rules = loadRules(rulesJson)
            rules.forEach { rule ->
                if (rule.name.lowercase().contains(lower) ||
                    rule.metricKey.lowercase().contains(lower)
                ) {
                    results.add(
                        SearchResult(
                            title = rule.name.ifBlank { "Link Rule" },
                            subtitle = rule.metricKey,
                            category = SearchCategory.LINK,
                            route = Routes.LINK,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to search link rules")
        }

        // 4. Search settings labels
        settingLabels.forEach { (label, route) ->
            if (label.lowercase().contains(lower)) {
                results.add(
                    SearchResult(
                        title = label,
                        subtitle = "Settings",
                        category = SearchCategory.SETTING,
                        route = route,
                    )
                )
            }
        }

        return results.sortedBy { it.category.ordinal }
    }
}
