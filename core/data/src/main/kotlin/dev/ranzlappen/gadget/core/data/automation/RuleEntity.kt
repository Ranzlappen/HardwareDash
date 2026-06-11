package dev.ranzlappen.gadget.core.data.automation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Row shape of `automation.db`'s `rules` table (ADR-0002 Decision 5): the
 * sealed graphs (trigger / conditions / actions) are stored as
 * kotlinx-serialization **JSON columns** — flat relational schema, wire
 * format guarded by the pinned `@SerialName`s + `RuleSerializationTest` in
 * `:core:automation`. `last_fired_at` persists the cooldown clock across
 * process death and reboot (Decision 8).
 */
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enabled: Boolean,
    @ColumnInfo(name = "trigger_json") val triggerJson: String,
    @ColumnInfo(name = "conditions_json") val conditionsJson: String,
    @ColumnInfo(name = "condition_logic") val conditionLogic: String,
    @ColumnInfo(name = "actions_json") val actionsJson: String,
    @ColumnInfo(name = "cooldown_seconds") val cooldownSeconds: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "last_fired_at") val lastFiredAt: Long? = null,
)
