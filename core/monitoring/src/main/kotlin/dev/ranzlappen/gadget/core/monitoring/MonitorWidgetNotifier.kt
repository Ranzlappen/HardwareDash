package dev.ranzlappen.gadget.core.monitoring

/**
 * Seam that lets a feature refresh its home-screen monitor widget when a
 * new sample lands — without `:core:monitoring` depending on the feature.
 *
 * A feature binds one notifier per monitored metric into a Hilt
 * `Map<String, MonitorWidgetNotifier>` (`@IntoMap @StringKey(metricKey)`).
 * [MonitorService] calls [onSample] each poll when the metric's config has
 * `widgetEnabled = true`; the implementation broadcasts an update to its
 * own AppWidgetProvider. Torch's `TorchMonitorWidgetNotifier` is the
 * reference implementation.
 */
interface MonitorWidgetNotifier {
    val metricKey: String
    fun onSample(value: Float)
}
