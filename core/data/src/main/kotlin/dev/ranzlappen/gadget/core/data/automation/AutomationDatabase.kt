package dev.ranzlappen.gadget.core.data.automation

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * `automation.db` — third modular Room database, sibling to `apps.db` and
 * `monitoring.db` (ADR-0002 Decision 5). Joins the whole-app backup ZIP via
 * the existing generic `databases/` sweep (backup format v5).
 *
 * v2 (W7) adds the `rule_fire_history` audit-trail table alongside the
 * existing `rules` table — a schema-additive migration.
 */
@Database(
    entities = [RuleEntity::class, RuleFireEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun ruleFireDao(): RuleFireDao

    companion object {
        /** v1 → v2: create the firing-history table (W7). Additive; existing
         *  rules are untouched. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rule_fire_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`rule_id` TEXT NOT NULL, " +
                        "`rule_name` TEXT NOT NULL, " +
                        "`fired_at` INTEGER NOT NULL, " +
                        "`outcome` TEXT NOT NULL, " +
                        "`dispatched` INTEGER NOT NULL, " +
                        "`throttled` INTEGER NOT NULL, " +
                        "`dry_run` INTEGER NOT NULL, " +
                        "`detail` TEXT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_rule_fire_history_fired_at` " +
                        "ON `rule_fire_history` (`fired_at`)",
                )
            }
        }
    }
}
