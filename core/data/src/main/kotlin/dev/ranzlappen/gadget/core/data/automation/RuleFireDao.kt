package dev.ranzlappen.gadget.core.data.automation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleFireDao {

    @Query("SELECT * FROM rule_fire_history ORDER BY fired_at DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RuleFireEntity>>

    @Insert
    suspend fun insert(entity: RuleFireEntity)

    /**
     * Trim the trail to its newest [keep] rows — called after each insert so
     * the audit log can't grow unbounded.
     */
    @Query(
        "DELETE FROM rule_fire_history WHERE id NOT IN " +
            "(SELECT id FROM rule_fire_history ORDER BY fired_at DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM rule_fire_history")
    suspend fun clear()
}
