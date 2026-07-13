package dev.ranzlappen.gadget.core.datastore

import kotlinx.serialization.Serializable

/**
 * The user's dashboard arrangement (W9): the order, hidden set, and pinned set
 * of dashboard module entries, keyed by each module's stable
 * `GadgetDestination.route` string. Persisted as a JSON string in
 * [UserPreferences.dashboardLayout] (the one field the app-wide preferences
 * repository serializes rather than storing as a scalar).
 *
 * All fields default empty — an untouched dashboard shows the full module
 * catalog in its natural order, nothing hidden or pinned. Routes present in
 * the catalog but absent from [order] (a module added in a later release)
 * append at the end in catalog order, so the layout never has to be migrated
 * when the catalog grows. Unknown routes in [order] (a module removed) are
 * ignored at resolve time.
 */
@Serializable
data class DashboardLayout(
    /** Explicit user ordering of module routes; empty = catalog order. */
    val order: List<String> = emptyList(),
    /** Routes hidden from the dashboard (still reachable via the nav rail). */
    val hidden: Set<String> = emptySet(),
    /** Routes pinned to the top of the dashboard. */
    val pinned: Set<String> = emptySet(),
)
