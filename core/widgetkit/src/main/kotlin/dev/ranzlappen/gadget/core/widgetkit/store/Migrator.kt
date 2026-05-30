package dev.ranzlappen.gadget.core.widgetkit.store

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig

/**
 * Per-feature schema-bump migration seam for [WidgetConfigStore].
 *
 * When a feature's `@Serializable` widget config grows fields or
 * changes semantics, bump [WidgetKitConfig.schemaVersion] and bind a
 * `Migrator` from the feature's Hilt module that upgrades any older
 * on-disk version to the latest. The store calls [migrate] on every
 * read, so the migration runs lazily for each user as their stored
 * configs come into scope.
 *
 * Default to [NoOpMigrator] when no migration is needed yet (every
 * existing on-disk config is already on the latest schema).
 *
 * Closes P1-9 (in full) — the legacy `Link`-style hardcoded migration
 * sweep replaced by a per-feature seam the kit drives transparently.
 */
fun interface Migrator<T : WidgetKitConfig> {
    /**
     * Upgrade [stored] (which carries its own `schemaVersion`) to the
     * latest shape, or return [stored] unchanged when no work is
     * needed for that record.
     */
    fun migrate(stored: T): T
}

/** Identity migrator — no schema work to do. */
class NoOpMigrator<T : WidgetKitConfig> : Migrator<T> {
    override fun migrate(stored: T): T = stored
}
