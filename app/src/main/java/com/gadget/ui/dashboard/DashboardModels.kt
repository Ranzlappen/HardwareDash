package com.gadget.ui.dashboard

import androidx.compose.ui.graphics.vector.ImageVector

data class HeroMetric(
    val key: String,
    val label: String,
    val currentValue: String,
    val icon: ImageVector,
    val sparklineData: List<Float>,
)

data class Insight(
    val id: String,
    val message: String,
    val icon: ImageVector,
    val timestamp: Long,
)

data class ActivityItem(
    val id: String,
    val text: String,
    val timestamp: String,
    val type: ActivityType,
)

enum class ActivityType {
    LOG_ENTRY,
    LINK_TRIGGER,
    SERVICE_EVENT,
}
