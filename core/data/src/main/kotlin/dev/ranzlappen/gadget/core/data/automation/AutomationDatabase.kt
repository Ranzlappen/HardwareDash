package dev.ranzlappen.gadget.core.data.automation

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * `automation.db` — third modular Room database, sibling to `apps.db` and
 * `monitoring.db` (ADR-0002 Decision 5). Joins the whole-app backup ZIP via
 * the existing generic `databases/` sweep (backup format v5).
 */
@Database(
    entities = [RuleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
}
