package dev.ranzlappen.gadget.core.data.automation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row of the automation **firing history** (W7) — an append-only audit
 * trail of rule evaluations written by `RuleFireExecutor`. Indexed by
 * `fired_at` (descending reads drive the history UI). The outcome enum is
 * stored by name, not ordinal, so a reordered enum can't corrupt old rows.
 */
@Entity(
    tableName = "rule_fire_history",
    indices = [Index("fired_at")],
)
data class RuleFireEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "rule_id") val ruleId: String,
    @ColumnInfo(name = "rule_name") val ruleName: String,
    @ColumnInfo(name = "fired_at") val firedAt: Long,
    @ColumnInfo(name = "outcome") val outcome: String,
    @ColumnInfo(name = "dispatched") val dispatched: Int,
    @ColumnInfo(name = "throttled") val throttled: Boolean,
    @ColumnInfo(name = "dry_run") val dryRun: Boolean,
    @ColumnInfo(name = "detail") val detail: String? = null,
)
