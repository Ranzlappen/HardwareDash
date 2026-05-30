package dev.ranzlappen.gadget.core.widgetkit

/**
 * Outcome of a "pin a new widget" request, so callers can tell apart the
 * cases the UI must surface differently. Replaces a bare `Boolean` return
 * (which conflated "launcher can't pin" with "you've hit the cap").
 */
enum class WidgetPinResult {
    /** The OS accepted the pin request and showed the launcher's dialog.
     *  (Acceptance of the *dialog* is still up to the user — this only means
     *  the request was dispatched.) */
    Requested,

    /** The active launcher doesn't support `requestPinAppWidget`. */
    LauncherUnsupported,

    /** The per-kind widget cap ([WidgetPinPolicy.MAX_WIDGETS_PER_KIND]) is
     *  already reached; refuse to pin more. */
    CapReached,
}

/**
 * Pin-creation guard rails shared by every feature's widget creator.
 *
 * The cap bounds two things that otherwise grow unbounded: launcher clutter
 * and the per-feature DataStore that stores one config per `appWidgetId`. A
 * pathological in-app pin loop (or a user spamming "Add") can't run away.
 */
object WidgetPinPolicy {
    /** Maximum simultaneously-pinned widgets of a single kind per feature. */
    const val MAX_WIDGETS_PER_KIND = 20

    /** `true` iff another widget of a kind with [currentCount] live instances
     *  may be pinned. */
    fun canPin(currentCount: Int): Boolean = currentCount < MAX_WIDGETS_PER_KIND
}
