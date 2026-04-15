package com.gadget.ui.search

data class SearchResult(
    val title: String,
    val subtitle: String,
    val category: SearchCategory,
    val route: String?,
)

enum class SearchCategory { METRIC, LOGBOOK, LINK, SETTING }
