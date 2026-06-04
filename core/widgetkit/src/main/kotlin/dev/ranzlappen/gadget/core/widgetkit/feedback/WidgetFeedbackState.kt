package dev.ranzlappen.gadget.core.widgetkit.feedback

/**
 * The outcome a widget tap reports to [WidgetFeedbackDispatcher], replacing the
 * old on/off-only boolean so momentary functions (and failures) get accurate
 * confirmation text.
 *
 * Drives the `{state}` placeholder in the feedback templates:
 * - [Toggle] — `{state}` → `"on"` / `"off"` (binary toggle functions).
 * - [Triggered] — `{state}` → `"triggered"` (momentary functions: one-shot
 *   vibrate, morse, pattern). Fixes the bug where a momentary widget always
 *   read "… off".
 * - [Failed] — `{state}` → the failure [reason] (e.g. a rooted action that's
 *   opted-out), so the user sees why nothing happened instead of a misleading
 *   "off".
 */
sealed interface WidgetFeedbackState {
    data class Toggle(val active: Boolean) : WidgetFeedbackState
    data object Triggered : WidgetFeedbackState
    data class Failed(val reason: String) : WidgetFeedbackState
}
